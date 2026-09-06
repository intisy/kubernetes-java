package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketPatchTest {

    @Test
    void requestLineCarriesPatchAndTheFullPathWithQuery() throws Exception {
        try (FakeServer server = new FakeServer()) {
            server.respondWith(cannedResponse(200, "OK", "application/json", "{}"));
            KubernetesHttpClient client = new KubernetesHttpClient("http://127.0.0.1:" + server.port());

            Map<String, String> query = new LinkedHashMap<>();
            query.put("fieldManager", "kubernetes-java");
            client.patch("/api/v1/namespaces/demo", query, "{}", "application/json");

            String request = server.awaitRequest();
            assertTrue(request.startsWith("PATCH /api/v1/namespaces/demo?fieldManager=kubernetes-java HTTP/1.1\r\n"),
                    "expected request line with full path and query, got: " + firstLine(request));
        }
    }

    @Test
    void contentTypeHeaderMatchesWhatTheCallerPassed() throws Exception {
        try (FakeServer server = new FakeServer()) {
            server.respondWith(cannedResponse(200, "OK", "application/json", "{}"));
            KubernetesHttpClient client = new KubernetesHttpClient("http://127.0.0.1:" + server.port());

            client.patch("/api/v1/namespaces/demo", null, "manifest: true\n", "application/apply-patch+yaml");

            String request = server.awaitRequest();
            assertEquals("application/apply-patch+yaml", header(request, "Content-Type"));
        }
    }

    @Test
    void contentLengthMatchesUtf8BytesAndTheBodyArrivesIntactWithNonAscii() throws Exception {
        try (FakeServer server = new FakeServer()) {
            server.respondWith(cannedResponse(200, "OK", "application/json", "{}"));
            KubernetesHttpClient client = new KubernetesHttpClient("http://127.0.0.1:" + server.port());

            String body = "{\"name\":\"café-☃\"}";
            int expectedLength = body.getBytes(StandardCharsets.UTF_8).length;

            client.patch("/api/v1/namespaces/demo", body);

            String request = server.awaitRequest();
            assertEquals(String.valueOf(expectedLength), header(request, "Content-Length"));
            assertEquals(body, requestBody(request));
        }
    }

    @Test
    void aContentLengthResponseIsParsedCorrectly() throws Exception {
        try (FakeServer server = new FakeServer()) {
            server.respondWith(cannedResponse(200, "OK", "application/json", "{\"status\":\"ok\"}"));
            KubernetesHttpClient client = new KubernetesHttpClient("http://127.0.0.1:" + server.port());

            KubernetesResponse response = client.patch("/api/v1/namespaces/demo", "{}");

            assertEquals(200, response.getStatusCode());
            assertEquals("{\"status\":\"ok\"}", response.getBody());
        }
    }

    @Test
    void aChunkedTransferEncodingResponseIsParsedCorrectly() throws Exception {
        try (FakeServer server = new FakeServer()) {
            String chunked = "HTTP/1.1 200 OK\r\n"
                    + "Transfer-Encoding: chunked\r\n"
                    + "Content-Type: application/json\r\n"
                    + "\r\n"
                    + "5\r\nhello\r\n"
                    + "6\r\n world\r\n"
                    + "0\r\n\r\n";
            server.respondWith(chunked.getBytes(StandardCharsets.US_ASCII));
            KubernetesHttpClient client = new KubernetesHttpClient("http://127.0.0.1:" + server.port());

            KubernetesResponse response = client.patch("/api/v1/namespaces/demo", "{}");

            assertEquals(200, response.getStatusCode());
            assertEquals("hello world", response.getBody());
        }
    }

    @Test
    void aNonSuccessResponseStillReturnsItsBody() throws Exception {
        try (FakeServer server = new FakeServer()) {
            String errorBody = "{\"message\":\"the manifest was rejected\"}";
            server.respondWith(cannedResponse(422, "Unprocessable Entity", "application/json", errorBody));
            KubernetesHttpClient client = new KubernetesHttpClient("http://127.0.0.1:" + server.port());

            KubernetesResponse response = client.patch("/api/v1/namespaces/demo", "{}");

            assertEquals(422, response.getStatusCode());
            assertEquals(errorBody, response.getBody());
        }
    }

    @Test
    void httpsInsecureClientReachesASelfSignedServerAndSendsPatchNotPost() throws Exception {
        try (FakeServer server = new FakeServer(sslServerSocket())) {
            server.respondWith(cannedResponse(200, "OK", "application/json", "{\"status\":\"ok\"}"));
            KubernetesHttpClient client = new KubernetesHttpClient(
                    "https://localhost:" + server.port(), null, null, TIMEOUT_MS, true);

            Map<String, String> query = new LinkedHashMap<>();
            query.put("fieldManager", "kubernetes-java");
            KubernetesResponse response = client.patch("/api/v1/namespaces/demo", query,
                    "{\"spec\":true}", "application/apply-patch+yaml");

            String request = server.awaitRequest();
            assertTrue(request.startsWith("PATCH /api/v1/namespaces/demo?fieldManager=kubernetes-java HTTP/1.1\r\n"),
                    "expected the server to see PATCH over HTTPS, not POST; got: " + firstLine(request));
            assertEquals("application/apply-patch+yaml", header(request, "Content-Type"));
            assertEquals(200, response.getStatusCode());
            assertEquals("{\"status\":\"ok\"}", response.getBody());
        }
    }

    @Test
    void httpsVerifyingClientRejectsACertificateThatDoesNotIdentifyTheHost() throws Exception {
        try (FakeServer server = new FakeServer(sslServerSocket())) {
            server.respondWith(cannedResponse(200, "OK", "application/json", "{}"));
            KubernetesHttpClient client = new KubernetesHttpClient(
                    "https://localhost:" + server.port(), null, fixture("tls-cert.pem"), TIMEOUT_MS, false);

            Throwable thrown = assertThrows(IOException.class, () -> client.patch("/api/v1/namespaces/demo", "{}"),
                    "a verifying client must not silently accept a certificate that does not identify localhost");
            assertTrue(thrown instanceof SSLException,
                    "expected real JSSE endpoint identification (setEndpointIdentificationAlgorithm(\"HTTPS\")) to "
                            + "reject this certificate during the handshake since it names neither localhost nor "
                            + "127.0.0.1, got: " + thrown);
        }
    }

    @Test
    void httpsVerifyingClientSucceedsAgainstACertificateThatMatchesTheHost() throws Exception {
        GeneratedTlsMaterial tls = sslServerSocketWithMatchingSan();
        try (FakeServer server = new FakeServer(tls.serverSocket)) {
            server.respondWith(cannedResponse(200, "OK", "application/json", "{\"status\":\"ok\"}"));
            KubernetesHttpClient client = new KubernetesHttpClient(
                    "https://localhost:" + server.port(), null, tls.certPem, TIMEOUT_MS, false);

            Map<String, String> query = new LinkedHashMap<>();
            query.put("fieldManager", "kubernetes-java");
            KubernetesResponse response = client.patch("/api/v1/namespaces/demo", query,
                    "{\"spec\":true}", "application/apply-patch+yaml");

            String request = server.awaitRequest();
            assertTrue(request.startsWith("PATCH /api/v1/namespaces/demo?fieldManager=kubernetes-java HTTP/1.1\r\n"),
                    "expected the server to see PATCH once real endpoint identification accepts a matching "
                            + "certificate; got: " + firstLine(request));
            assertEquals("application/apply-patch+yaml", header(request, "Content-Type"));
            assertEquals(200, response.getStatusCode());
            assertEquals("{\"status\":\"ok\"}", response.getBody());
        }
    }

    @Test
    void aFailedHostnameVerificationClosesTheClientSocket() throws Exception {
        try (FakeServer server = new FakeServer(sslServerSocket())) {
            Future<Integer> peerRead = server.expectEndOfStream();
            KubernetesHttpClient client = new KubernetesHttpClient(
                    "https://localhost:" + server.port(), null, fixture("tls-cert.pem"), TIMEOUT_MS, false);

            assertThrows(IOException.class, () -> client.patch("/api/v1/namespaces/demo", "{}"));

            int firstByte = peerRead.get(10, TimeUnit.SECONDS);
            assertEquals(-1, firstByte,
                    "expected the server to observe end-of-stream (or a connection abort, treated the same way, "
                            + "since real JSSE endpoint identification can fail mid-handshake) once the client's "
                            + "socket is closed after the failed hostname verification; a leaked, never-closed "
                            + "socket would instead leave the server blocked until its own SocketTimeoutException, "
                            + "which is not caught and would fail this test");
        }
    }

    private static final int TIMEOUT_MS = 5000;

    private static SSLServerSocket sslServerSocket() throws Exception {
        X509Certificate cert = parseCertificate(fixture("tls-cert.pem"));
        PrivateKey privateKey = parsePkcs8EcPrivateKey(fixture("tls-key-pkcs8.pem"));

        char[] password = "changeit".toCharArray();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setKeyEntry("server", privateKey, password, new Certificate[]{cert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        return (SSLServerSocket) factory.createServerSocket(0);
    }

    private static final class GeneratedTlsMaterial {
        final SSLServerSocket serverSocket;
        final String certPem;

        GeneratedTlsMaterial(SSLServerSocket serverSocket, String certPem) {
            this.serverSocket = serverSocket;
            this.certPem = certPem;
        }
    }

    /**
     * @implNote The existing {@code tls-cert.pem} fixture has no subject alternative name, so it
     * cannot exercise real JSSE endpoint identification succeeding. {@code keytool} ships with
     * every JDK, so generating a throwaway keypair and self-signed certificate with a matching SAN
     * through it, rather than via a new cryptography dependency or JDK-internal certificate
     * classes, proves the positive case without either.
     */
    private static GeneratedTlsMaterial sslServerSocketWithMatchingSan() throws Exception {
        Path keystoreFile = Files.createTempFile("socketpatch-keystore-", ".p12");
        keystoreFile.toFile().deleteOnExit();
        Files.delete(keystoreFile);
        Path certFile = Files.createTempFile("socketpatch-cert-", ".pem");
        certFile.toFile().deleteOnExit();

        String alias = "kubernetes-java-test-san";
        char[] password = "changeit".toCharArray();

        runKeytool("-genkeypair", "-alias", alias, "-keyalg", "EC", "-keysize", "256",
                "-sigalg", "SHA256withECDSA", "-dname", "CN=" + alias,
                "-ext", "SAN=dns:localhost,ip:127.0.0.1",
                "-validity", "2", "-storetype", "PKCS12",
                "-keystore", keystoreFile.toString(), "-storepass", "changeit", "-keypass", "changeit");

        runKeytool("-exportcert", "-alias", alias, "-rfc",
                "-keystore", keystoreFile.toString(), "-storepass", "changeit",
                "-file", certFile.toString());

        String certPem = new String(Files.readAllBytes(certFile), StandardCharsets.UTF_8);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(keystoreFile)) {
            keyStore.load(in, password);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(0);
        return new GeneratedTlsMaterial(serverSocket, certPem);
    }

    private static void runKeytool(String... args) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(keytoolPath());
        command.addAll(Arrays.asList(args));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = drain(process.getInputStream());
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("keytool timed out; output so far: " + output);
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("keytool exited with " + process.exitValue() + ": " + output);
        }
    }

    private static String drain(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String keytoolPath() {
        String javaHome = System.getProperty("java.home");
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        return javaHome + File.separator + "bin" + File.separator + (windows ? "keytool.exe" : "keytool");
    }

    private static X509Certificate parseCertificate(String pem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (InputStream in = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))) {
            return (X509Certificate) cf.generateCertificate(in);
        }
    }

    private static PrivateKey parsePkcs8EcPrivateKey(String pem) throws Exception {
        String base64 = pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private static String fixture(String resource) throws IOException {
        try (InputStream in = SocketPatchTest.class.getClassLoader().getResourceAsStream(resource)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] cannedResponse(int statusCode, String reason, String contentType, String body) {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + statusCode + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + bodyBytes.length + "\r\n"
                + "\r\n";
        byte[] headBytes = head.getBytes(StandardCharsets.US_ASCII);
        byte[] full = new byte[headBytes.length + bodyBytes.length];
        System.arraycopy(headBytes, 0, full, 0, headBytes.length);
        System.arraycopy(bodyBytes, 0, full, headBytes.length, bodyBytes.length);
        return full;
    }

    private static String firstLine(String request) {
        int newline = request.indexOf("\r\n");
        return newline >= 0 ? request.substring(0, newline) : request;
    }

    private static String header(String request, String name) {
        String[] lines = request.split("\r\n");
        String prefix = name.toLowerCase() + ":";
        for (String line : lines) {
            if (line.toLowerCase().startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private static String requestBody(String request) {
        int separator = request.indexOf("\r\n\r\n");
        return separator >= 0 ? request.substring(separator + 4) : "";
    }

    private static final class FakeServer implements Closeable {
        private final ServerSocket serverSocket;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private Future<String> requestFuture;

        FakeServer() throws IOException {
            this(new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")));
        }

        FakeServer(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        void respondWith(final byte[] response) {
            requestFuture = executor.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    try (Socket socket = serverSocket.accept()) {
                        byte[] request = readRequest(socket.getInputStream());
                        socket.getOutputStream().write(response);
                        socket.getOutputStream().flush();
                        return new String(request, StandardCharsets.UTF_8);
                    }
                }
            });
        }

        String awaitRequest() throws Exception {
            return requestFuture.get(10, TimeUnit.SECONDS);
        }

        Future<Integer> expectEndOfStream() {
            return executor.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    try (Socket socket = serverSocket.accept()) {
                        socket.setSoTimeout(8000);
                        try {
                            return socket.getInputStream().read();
                        } catch (java.net.SocketException e) {
                            return -1;
                        }
                    }
                }
            });
        }

        private static byte[] readRequest(InputStream in) throws IOException {
            ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
            int matched = 0;
            String terminator = "\r\n\r\n";
            int b;
            while ((b = in.read()) != -1) {
                headerBuffer.write(b);
                if (b == terminator.charAt(matched)) {
                    matched++;
                    if (matched == terminator.length()) {
                        break;
                    }
                } else {
                    matched = (b == terminator.charAt(0)) ? 1 : 0;
                }
            }

            String headerText = new String(headerBuffer.toByteArray(), StandardCharsets.US_ASCII);
            int contentLength = 0;
            for (String line : headerText.split("\r\n")) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                }
            }

            ByteArrayOutputStream full = new ByteArrayOutputStream();
            full.write(headerBuffer.toByteArray());
            if (contentLength > 0) {
                byte[] bodyBytes = new byte[contentLength];
                int offset = 0;
                while (offset < contentLength) {
                    int read = in.read(bodyBytes, offset, contentLength - offset);
                    if (read == -1) {
                        break;
                    }
                    offset += read;
                }
                full.write(bodyBytes, 0, offset);
            }
            return full.toByteArray();
        }

        @Override
        public void close() {
            executor.shutdownNow();
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
