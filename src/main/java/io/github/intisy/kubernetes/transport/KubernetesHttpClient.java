package io.github.intisy.kubernetes.transport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * HTTP client for communicating with the Kubernetes API server.
 * Supports HTTPS with client certificate authentication and bearer token authentication.
 *
 * @author Finn Birich
 */
public class KubernetesHttpClient implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(KubernetesHttpClient.class);

    private static final int DEFAULT_TIMEOUT = 30000;

    private final String apiServerUrl;
    private final Gson gson;
    private final int timeout;
    private final SSLSocketFactory sslSocketFactory;
    private final HostnameVerifier hostnameVerifier;
    private final String bearerToken;

    public KubernetesHttpClient(String apiServerUrl, String caCertPath, String clientCertPath, String clientKeyPath, int timeoutMs) {
        this.apiServerUrl = apiServerUrl.endsWith("/") ? apiServerUrl.substring(0, apiServerUrl.length() - 1) : apiServerUrl;
        this.timeout = timeoutMs;
        this.bearerToken = null;
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();

        SSLSocketFactory factory = null;
        HostnameVerifier verifier = null;
        if (caCertPath != null) {
            try {
                factory = createSslSocketFactory(caCertPath, clientCertPath, clientKeyPath);
                verifier = createHostnameVerifier();
            } catch (Exception e) {
                log.warn("Failed to create SSL context, falling back to insecure: {}", e.getMessage());
                factory = createInsecureSslSocketFactory();
                verifier = createInsecureHostnameVerifier();
            }
        } else {
            factory = createInsecureSslSocketFactory();
            verifier = createInsecureHostnameVerifier();
        }
        this.sslSocketFactory = factory;
        this.hostnameVerifier = verifier;

        log.debug("Created KubernetesHttpClient for server: {}", apiServerUrl);
    }

    public KubernetesHttpClient(String apiServerUrl, String bearerToken, String caCertPath, int timeoutMs) {
        this.apiServerUrl = apiServerUrl.endsWith("/") ? apiServerUrl.substring(0, apiServerUrl.length() - 1) : apiServerUrl;
        this.timeout = timeoutMs;
        this.bearerToken = bearerToken;
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();

        SSLSocketFactory factory = null;
        HostnameVerifier verifier = null;
        if (caCertPath != null) {
            try {
                factory = createSslSocketFactoryWithCa(caCertPath);
                verifier = createHostnameVerifier();
            } catch (Exception e) {
                log.warn("Failed to create SSL context with CA, falling back to insecure: {}", e.getMessage());
                factory = createInsecureSslSocketFactory();
                verifier = createInsecureHostnameVerifier();
            }
        } else {
            factory = createInsecureSslSocketFactory();
            verifier = createInsecureHostnameVerifier();
        }
        this.sslSocketFactory = factory;
        this.hostnameVerifier = verifier;

        log.debug("Created KubernetesHttpClient with bearer token for server: {}", apiServerUrl);
    }

    public KubernetesHttpClient(String apiServerUrl) {
        this(apiServerUrl, (String) null, null, null, DEFAULT_TIMEOUT);
    }

    public KubernetesHttpClient(String apiServerUrl, int timeoutMs) {
        this(apiServerUrl, (String) null, null, null, timeoutMs);
    }

    public Gson getGson() {
        return gson;
    }

    public KubernetesResponse get(String path) throws IOException {
        return request("GET", path, null);
    }

    public KubernetesResponse get(String path, Map<String, String> queryParams) throws IOException {
        String fullPath = buildPathWithQuery(path, queryParams);
        return request("GET", fullPath, null);
    }

    public KubernetesResponse post(String path, Object body) throws IOException {
        String jsonBody = body != null ? gson.toJson(body) : null;
        return request("POST", path, jsonBody);
    }

    public KubernetesResponse post(String path) throws IOException {
        return request("POST", path, null);
    }

    public KubernetesResponse post(String path, Map<String, String> queryParams, Object body) throws IOException {
        String fullPath = buildPathWithQuery(path, queryParams);
        String jsonBody = body != null ? gson.toJson(body) : null;
        return request("POST", fullPath, jsonBody);
    }

    public KubernetesResponse put(String path, Object body) throws IOException {
        String jsonBody = body != null ? gson.toJson(body) : null;
        return request("PUT", path, jsonBody);
    }

    public KubernetesResponse patch(String path, String jsonBody) throws IOException {
        return patchRequest(path, jsonBody);
    }

    public KubernetesResponse delete(String path) throws IOException {
        return request("DELETE", path, null);
    }

    public KubernetesResponse delete(String path, Map<String, String> queryParams) throws IOException {
        String fullPath = buildPathWithQuery(path, queryParams);
        return request("DELETE", fullPath, null);
    }

    public void getStream(String path, Map<String, String> queryParams, StreamCallback<String> callback) throws IOException {
        String fullPath = buildPathWithQuery(path, queryParams);
        requestStream("GET", fullPath, callback);
    }

    private String buildPathWithQuery(String path, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return path;
        }
        StringBuilder sb = new StringBuilder(path);
        sb.append("?");
        boolean first = true;
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(urlEncode(entry.getKey())).append("=").append(urlEncode(entry.getValue()));
            first = false;
        }
        return sb.toString();
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private KubernetesResponse request(String method, String path, String body) throws IOException {
        log.trace("{} {}", method, path);
        URL url = new URL(apiServerUrl + path);
        log.debug("Request: {} {} (timeout: {}ms)", method, url, timeout);

        HttpURLConnection conn;
        if (url.getProtocol().equals("https")) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) url.openConnection();
            if (sslSocketFactory != null) {
                httpsConn.setSSLSocketFactory(sslSocketFactory);
            }
            if (hostnameVerifier != null) {
                httpsConn.setHostnameVerifier(hostnameVerifier);
            }
            conn = httpsConn;
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }

        conn.setRequestMethod(method);
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");

        if (bearerToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }

        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int statusCode = conn.getResponseCode();
        log.debug("Response status: {}", statusCode);
        Map<String, List<String>> headers = conn.getHeaderFields();

        String responseBody;
        try (InputStream is = statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
            if (is != null) {
                responseBody = readStream(is);
            } else {
                responseBody = "";
            }
        }

        return new KubernetesResponse(statusCode, headers, responseBody);
    }

    private KubernetesResponse patchRequest(String path, String body) throws IOException {
        log.trace("PATCH {}", path);
        URL url = new URL(apiServerUrl + path);

        HttpURLConnection conn;
        if (url.getProtocol().equals("https")) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) url.openConnection();
            if (sslSocketFactory != null) {
                httpsConn.setSSLSocketFactory(sslSocketFactory);
            }
            if (hostnameVerifier != null) {
                httpsConn.setHostnameVerifier(hostnameVerifier);
            }
            conn = httpsConn;
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }

        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/strategic-merge-patch+json");

        if (bearerToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }

        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int statusCode = conn.getResponseCode();
        Map<String, List<String>> headers = conn.getHeaderFields();

        String responseBody;
        try (InputStream is = statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
            if (is != null) {
                responseBody = readStream(is);
            } else {
                responseBody = "";
            }
        }

        return new KubernetesResponse(statusCode, headers, responseBody);
    }

    private void requestStream(String method, String path, StreamCallback<String> callback) throws IOException {
        log.trace("{} {} (streaming)", method, path);
        URL url = new URL(apiServerUrl + path);

        HttpURLConnection conn;
        if (url.getProtocol().equals("https")) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) url.openConnection();
            if (sslSocketFactory != null) {
                httpsConn.setSSLSocketFactory(sslSocketFactory);
            }
            if (hostnameVerifier != null) {
                httpsConn.setHostnameVerifier(hostnameVerifier);
            }
            conn = httpsConn;
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }

        conn.setRequestMethod(method);
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(0);
        conn.setRequestProperty("Accept", "application/json");

        if (bearerToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }

        int statusCode = conn.getResponseCode();
        if (statusCode >= 400) {
            String errorBody;
            try (InputStream is = conn.getErrorStream()) {
                errorBody = is != null ? readStream(is) : "";
            }
            callback.onError(new IOException("HTTP " + statusCode + ": " + errorBody));
            return;
        }

        try (InputStream is = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && !callback.isCancelled()) {
                if (!line.isEmpty()) {
                    callback.onNext(line);
                }
            }
            callback.onComplete();
        } catch (IOException e) {
            callback.onError(e);
        }
    }

    private SSLSocketFactory createSslSocketFactory(String caCertPath, String clientCertPath, String clientKeyPath) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // Load CA certificate
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        try (InputStream caInput = Files.newInputStream(new File(caCertPath).toPath())) {
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(caInput);
            trustStore.setCertificateEntry("ca", caCert);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        KeyManager[] keyManagers = null;
        if (clientCertPath != null && clientKeyPath != null) {
            // For client certificate auth, we use the minikube-generated client cert/key
            // Minikube provides these in PEM format; we convert to PKCS12 via process
            keyManagers = createKeyManagers(clientCertPath, clientKeyPath);
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, tmf.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }

    private SSLSocketFactory createSslSocketFactoryWithCa(String caCertPath) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        try (InputStream caInput = Files.newInputStream(new File(caCertPath).toPath())) {
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(caInput);
            trustStore.setCertificateEntry("ca", caCert);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }

    private KeyManager[] createKeyManagers(String clientCertPath, String clientKeyPath) throws Exception {
        // Create a temporary PKCS12 keystore from PEM cert/key using openssl
        Path tempP12 = Files.createTempFile("k8s-client-", ".p12");
        try {
            String password = "changeit";
            ProcessBuilder pb = new ProcessBuilder(
                    "openssl", "pkcs12", "-export",
                    "-in", clientCertPath,
                    "-inkey", clientKeyPath,
                    "-out", tempP12.toString(),
                    "-passout", "pass:" + password
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();

            if (process.exitValue() != 0) {
                log.warn("openssl pkcs12 export failed, client cert auth may not work");
                return null;
            }

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream ksInput = Files.newInputStream(tempP12)) {
                keyStore.load(ksInput, password.toCharArray());
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password.toCharArray());
            return kmf.getKeyManagers();
        } finally {
            Files.deleteIfExists(tempP12);
        }
    }

    private SSLSocketFactory createInsecureSslSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }}, null);
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create insecure SSL factory", e);
        }
    }

    private HostnameVerifier createHostnameVerifier() {
        return HttpsURLConnection.getDefaultHostnameVerifier();
    }

    private HostnameVerifier createInsecureHostnameVerifier() {
        return new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    @Override
    public void close() throws IOException {
        // No persistent connections to close
    }
}
