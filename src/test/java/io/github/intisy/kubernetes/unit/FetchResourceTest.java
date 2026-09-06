package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.KubernetesClient;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FetchResourceTest {
    private static final String APPS_DISCOVERY =
            "{\"resources\":[{\"name\":\"daemonsets\",\"kind\":\"DaemonSet\"},"
                    + "{\"name\":\"daemonsets/status\",\"kind\":\"DaemonSet\"}]}";
    private static final String DAEMONSET = "{\"kind\":\"DaemonSet\",\"status\":{\"numberReady\":1}}";

    private FakeApiServer server;
    private KubernetesClient client;

    @BeforeEach
    void startServer() throws Exception {
        server = new FakeApiServer();
        client = KubernetesClient.builder().withApiServer("http://127.0.0.1:" + server.port()).build();
    }

    @AfterEach
    void stopServer() throws Exception {
        client.close();
        server.close();
    }

    @Test
    void readsTheResourceDocumentFromThePathDiscoveryResolves() throws Exception {
        server.respond("/apis/apps/v1", 200, APPS_DISCOVERY);
        server.respond("/apis/apps/v1/namespaces/longhorn-system/daemonsets/longhorn-manager", 200, DAEMONSET);

        String document = client.fetchResource("apps/v1", "DaemonSet", "longhorn-system", "longhorn-manager");

        assertEquals(DAEMONSET, document);
        assertEquals(Arrays.asList("/apis/apps/v1",
                        "/apis/apps/v1/namespaces/longhorn-system/daemonsets/longhorn-manager"),
                server.requestedPaths());
    }

    @Test
    void reportsAMissingResourceAsNotFoundRatherThanAsAGenericFailure() throws Exception {
        server.respond("/apis/apps/v1", 200, APPS_DISCOVERY);
        server.respond("/apis/apps/v1/namespaces/longhorn-system/daemonsets/absent", 404,
                "{\"kind\":\"Status\",\"reason\":\"NotFound\"}");

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> client.fetchResource("apps/v1", "DaemonSet", "longhorn-system", "absent"));

        assertEquals(404, thrown.getStatusCode());
        assertTrue(thrown.getMessage().contains("daemonsets/absent"),
                "expected the message to name the path that was read, got: " + thrown.getMessage());
    }

    /**
     * @implNote A 403 must NOT arrive as {@link NotFoundException}: "you may not look" and "it is
     * not there" lead an operator to different actions, and only the status code separates them.
     */
    @Test
    void leavesAForbiddenReadDistinguishableFromAMissingResource() throws Exception {
        server.respond("/apis/apps/v1", 200, APPS_DISCOVERY);
        server.respond("/apis/apps/v1/namespaces/longhorn-system/daemonsets/longhorn-manager", 403, "{}");

        KubernetesException thrown = assertThrows(KubernetesException.class,
                () -> client.fetchResource("apps/v1", "DaemonSet", "longhorn-system", "longhorn-manager"));

        assertFalse(thrown instanceof NotFoundException, "a forbidden read must not read as not-found");
        assertEquals(403, thrown.getStatusCode());
    }

    private static final class FakeApiServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Map<String, int[]> statusByPath = new HashMap<>();
        private final Map<String, String> bodyByPath = new HashMap<>();
        private final List<String> requestedPaths = Collections.synchronizedList(new ArrayList<>());
        private volatile boolean running = true;

        FakeApiServer() throws IOException {
            serverSocket = new ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"));
            executor.submit(this::serve);
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        void respond(String path, int status, String body) {
            statusByPath.put(path, new int[]{status});
            bodyByPath.put(path, body);
        }

        List<String> requestedPaths() {
            return new ArrayList<>(requestedPaths);
        }

        private void serve() {
            while (running) {
                try (Socket socket = serverSocket.accept()) {
                    String path = readRequestPath(socket.getInputStream());
                    requestedPaths.add(path);
                    int[] status = statusByPath.get(path);
                    String body = bodyByPath.get(path);
                    if (status == null) {
                        status = new int[]{404};
                        body = "{\"kind\":\"Status\",\"reason\":\"NoStubbedResponse\"}";
                    }
                    write(socket.getOutputStream(), status[0], body);
                } catch (IOException stopping) {
                    // close() closes the listening socket, which is how this loop is meant to end.
                }
            }
        }

        /**
         * @implNote reads the WHOLE header block, not just the request line. Responding and closing
         * while request bytes are still unread makes the close a TCP reset on Linux and macOS, which
         * destroys the response the client has not read yet and surfaces as a connection failure
         * instead of the status the test stubbed. That is what made an earlier form of this server
         * pass on Windows and fail elsewhere.
         */
        private static String readRequestPath(InputStream in) throws IOException {
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            int matched = 0;
            String terminator = "\r\n\r\n";
            int b;
            while ((b = in.read()) != -1) {
                header.write(b);
                if (b == terminator.charAt(matched)) {
                    matched++;
                    if (matched == terminator.length()) {
                        break;
                    }
                } else {
                    matched = (b == terminator.charAt(0)) ? 1 : 0;
                }
            }
            String requestLine = new String(header.toByteArray(), StandardCharsets.UTF_8).split("\r\n", 2)[0];
            String[] parts = requestLine.split(" ");
            return parts.length > 1 ? parts[1] : "";
        }

        private static void write(OutputStream out, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            String head = "HTTP/1.1 " + status + " x\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Content-Length: " + bytes.length + "\r\n"
                    + "Connection: close\r\n\r\n";
            out.write(head.getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
            out.flush();
        }

        @Override
        public void close() throws Exception {
            running = false;
            serverSocket.close();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
