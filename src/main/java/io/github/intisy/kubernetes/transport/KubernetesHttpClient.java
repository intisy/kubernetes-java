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
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
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

    /**
     * @implNote {@code caCertPath} gates the whole TLS setup here on purpose, matching the
     * historical path-based constructor: without a CA, client cert material is never even read
     * and the client runs insecure. Preserved for backward compatibility.
     */
    public KubernetesHttpClient(String apiServerUrl, String caCertPath, String clientCertPath, String clientKeyPath, int timeoutMs) {
        this(apiServerUrl,
                readPemFile(caCertPath),
                caCertPath != null ? readPemFile(clientCertPath) : null,
                caCertPath != null ? readPemFile(clientKeyPath) : null,
                timeoutMs,
                false);
    }

    public KubernetesHttpClient(String apiServerUrl, String bearerToken, String caCertPath, int timeoutMs) {
        this(apiServerUrl, bearerToken, readPemFile(caCertPath), timeoutMs, false);
    }

    public KubernetesHttpClient(String apiServerUrl, String caCertPem, String clientCertPem, String clientKeyPem, int timeoutMs, boolean insecureTrustAll) {
        this.apiServerUrl = normalize(apiServerUrl);
        this.timeout = timeoutMs;
        this.bearerToken = null;
        this.gson = newGson();

        SSLSocketFactory factory;
        HostnameVerifier verifier;
        if (!insecureTrustAll && (caCertPem != null || clientCertPem != null || clientKeyPem != null)) {
            try {
                factory = createSslSocketFactory(caCertPem, clientCertPem, clientKeyPem);
                verifier = createHostnameVerifier();
            } catch (Exception e) {
                throw new IllegalStateException("failed to build the TLS context for " + this.apiServerUrl
                        + "; pass withInsecureTrustAll(true) to accept an unverified server deliberately", e);
            }
        } else {
            factory = createInsecureSslSocketFactory();
            verifier = createInsecureHostnameVerifier();
        }
        this.sslSocketFactory = factory;
        this.hostnameVerifier = verifier;

        log.debug("Created KubernetesHttpClient for server: {}", this.apiServerUrl);
    }

    public KubernetesHttpClient(String apiServerUrl, String bearerToken, String caCertPem, int timeoutMs, boolean insecureTrustAll) {
        this.apiServerUrl = normalize(apiServerUrl);
        this.timeout = timeoutMs;
        this.bearerToken = bearerToken;
        this.gson = newGson();

        SSLSocketFactory factory;
        HostnameVerifier verifier;
        if (!insecureTrustAll && caCertPem != null) {
            try {
                factory = createSslSocketFactoryWithCa(caCertPem);
                verifier = createHostnameVerifier();
            } catch (Exception e) {
                throw new IllegalStateException("failed to build the TLS context for " + this.apiServerUrl
                        + "; pass withInsecureTrustAll(true) to accept an unverified server deliberately", e);
            }
        } else {
            factory = createInsecureSslSocketFactory();
            verifier = createInsecureHostnameVerifier();
        }
        this.sslSocketFactory = factory;
        this.hostnameVerifier = verifier;

        log.debug("Created KubernetesHttpClient with bearer token for server: {}", this.apiServerUrl);
    }

    public KubernetesHttpClient(String apiServerUrl) {
        this(apiServerUrl, (String) null, null, null, DEFAULT_TIMEOUT);
    }

    public KubernetesHttpClient(String apiServerUrl, int timeoutMs) {
        this(apiServerUrl, (String) null, null, null, timeoutMs);
    }

    private static String normalize(String apiServerUrl) {
        return apiServerUrl.endsWith("/") ? apiServerUrl.substring(0, apiServerUrl.length() - 1) : apiServerUrl;
    }

    private static Gson newGson() {
        return new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();
    }

    private static String readPemFile(String path) {
        if (path == null) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("failed to read PEM file at " + path, e);
        }
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

        setHttpMethod(conn, "PATCH");
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

    private void setHttpMethod(HttpURLConnection conn, String method) {
        try {
            conn.setRequestMethod(method);
        } catch (java.net.ProtocolException e) {
            try {
                java.lang.reflect.Field methodField = HttpURLConnection.class.getDeclaredField("method");
                methodField.setAccessible(true);
                methodField.set(conn, method);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to set HTTP method to " + method, ex);
            }
        }
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

    /**
     * @implNote {@code caCertPem} is optional here: a caller supplying only a client
     * certificate and key (no CA) still gets real server verification, via the JVM's default
     * trust store, rather than being silently pushed onto the insecure path.
     */
    private SSLSocketFactory createSslSocketFactory(String caCertPem, String clientCertPem, String clientKeyPem) throws Exception {
        TrustManager[] trustManagers = null;
        if (caCertPem != null) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            try (InputStream caInput = new ByteArrayInputStream(caCertPem.getBytes(StandardCharsets.UTF_8))) {
                X509Certificate caCert = (X509Certificate) cf.generateCertificate(caInput);
                trustStore.setCertificateEntry("ca", caCert);
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            trustManagers = tmf.getTrustManagers();
        }

        KeyManager[] keyManagers = null;
        if (clientCertPem != null && clientKeyPem != null) {
            keyManagers = createKeyManagers(clientCertPem, clientKeyPem);
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);
        return sslContext.getSocketFactory();
    }

    private SSLSocketFactory createSslSocketFactoryWithCa(String caCertPem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        try (InputStream caInput = new ByteArrayInputStream(caCertPem.getBytes(StandardCharsets.UTF_8))) {
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(caInput);
            trustStore.setCertificateEntry("ca", caCert);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }

    private KeyManager[] createKeyManagers(String clientCertPem, String clientKeyPem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate clientCert;
        try (InputStream certInput = new ByteArrayInputStream(clientCertPem.getBytes(StandardCharsets.UTF_8))) {
            clientCert = (X509Certificate) cf.generateCertificate(certInput);
        }

        byte[] keyBytes = parsePemPrivateKey(clientKeyPem);
        if (keyBytes == null) {
            throw new IllegalStateException("client key is not a PEM private key");
        }

        PrivateKey privateKey;
        if (clientKeyPem.contains("BEGIN EC PRIVATE KEY")) {
            try {
                privateKey = Sec1EcKey.toPrivateKey(keyBytes);
            } catch (GeneralSecurityException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new GeneralSecurityException("failed to parse SEC1 EC private key: malformed or truncated DER", e);
            }
        } else {
            privateKey = fromPkcs8(keyBytes);
        }

        char[] password = "changeit".toCharArray();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setKeyEntry("client", privateKey, password, new Certificate[]{clientCert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);
        return kmf.getKeyManagers();
    }

    private PrivateKey fromPkcs8(byte[] keyBytes) throws GeneralSecurityException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (InvalidKeySpecException rsaFailure) {
            try {
                return KeyFactory.getInstance("EC").generatePrivate(keySpec);
            } catch (InvalidKeySpecException ecFailure) {
                throw new GeneralSecurityException("client key is not a valid PKCS8 RSA or EC private key", ecFailure);
            }
        }
    }

    private byte[] parsePemPrivateKey(String pem) {
        boolean isPkcs1 = pem.contains("BEGIN RSA PRIVATE KEY");

        String base64 = pem
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");

        if (base64.isEmpty()) {
            return null;
        }

        byte[] decoded = Base64.getDecoder().decode(base64);

        if (isPkcs1) {
            decoded = wrapPkcs1InPkcs8(decoded);
        }

        return decoded;
    }

    private byte[] wrapPkcs1InPkcs8(byte[] pkcs1Bytes) {
        byte[] oid = {0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01};
        byte[] nullParam = {0x05, 0x00};

        byte[] algorithmSeq = new byte[2 + oid.length + nullParam.length];
        algorithmSeq[0] = 0x30;
        algorithmSeq[1] = (byte) (oid.length + nullParam.length);
        System.arraycopy(oid, 0, algorithmSeq, 2, oid.length);
        System.arraycopy(nullParam, 0, algorithmSeq, 2 + oid.length, nullParam.length);

        byte[] octetString = encodeDerOctetString(pkcs1Bytes);

        byte[] versionBytes = {0x02, 0x01, 0x00};

        int totalContentLen = versionBytes.length + algorithmSeq.length + octetString.length;
        byte[] lenBytes = encodeDerLength(totalContentLen);

        byte[] result = new byte[1 + lenBytes.length + totalContentLen];
        result[0] = 0x30;
        int offset = 1;
        System.arraycopy(lenBytes, 0, result, offset, lenBytes.length);
        offset += lenBytes.length;
        System.arraycopy(versionBytes, 0, result, offset, versionBytes.length);
        offset += versionBytes.length;
        System.arraycopy(algorithmSeq, 0, result, offset, algorithmSeq.length);
        offset += algorithmSeq.length;
        System.arraycopy(octetString, 0, result, offset, octetString.length);

        return result;
    }

    private byte[] encodeDerOctetString(byte[] content) {
        byte[] lenBytes = encodeDerLength(content.length);
        byte[] result = new byte[1 + lenBytes.length + content.length];
        result[0] = 0x04;
        System.arraycopy(lenBytes, 0, result, 1, lenBytes.length);
        System.arraycopy(content, 0, result, 1 + lenBytes.length, content.length);
        return result;
    }

    private byte[] encodeDerLength(int length) {
        if (length < 128) {
            return new byte[]{(byte) length};
        } else if (length < 256) {
            return new byte[]{(byte) 0x81, (byte) length};
        } else if (length < 65536) {
            return new byte[]{(byte) 0x82, (byte) (length >> 8), (byte) (length & 0xff)};
        } else {
            return new byte[]{(byte) 0x83, (byte) (length >> 16), (byte) ((length >> 8) & 0xff), (byte) (length & 0xff)};
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
    }
}
