package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            serverSocket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
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
