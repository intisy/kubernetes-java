package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.KubernetesClient;
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
import java.net.URLDecoder;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SweepPodsTest {
    private static final String NAMESPACE = "seaweedfs";
    private static final String SELECTOR = "spisor.dev/ephemeral=true";
    private static final String PODS_PATH = "/api/v1/namespaces/" + NAMESPACE + "/pods";
    private static final String TWO_PODS =
            "{\"items\":[{\"metadata\":{\"name\":\"probe-1\"}},{\"metadata\":{\"name\":\"probe-2\"}}]}";
    private static final String NO_PODS = "{\"items\":[]}";
    private static final String DELETED = "{\"kind\":\"Status\",\"status\":\"Success\"}";
    private static final String GONE = "{\"kind\":\"Status\",\"reason\":\"NotFound\"}";

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
    void deletesEveryMatchingPodAndReturnsTheNamesItSwept() {
        server.respond(PODS_PATH, 200, TWO_PODS);
        server.respond(PODS_PATH + "/probe-1", 200, DELETED);
        server.respond(PODS_PATH + "/probe-2", 200, DELETED);

        List<String> swept = client.sweepPods()
                .withNamespace(NAMESPACE).withLabelSelector(SELECTOR).exec();

        assertEquals(Arrays.asList("probe-1", "probe-2"), swept);
        assertEquals(Arrays.asList("DELETE " + PODS_PATH + "/probe-1", "DELETE " + PODS_PATH + "/probe-2"),
                server.deleteRequests(), "expected one DELETE per matched pod");
    }

    /**
     * @implNote asserts the selector reached the api server rather than that the right pods came
     * back, because the fake returns whatever it is stubbed with. Filtering client-side instead
     * would pass a list-and-count test while still dragging every pod in the namespace over the
     * wire, and would delete the ones a narrower selector was meant to protect.
     */
    @Test
    void appliesTheLabelSelectorToTheListRequestRatherThanFilteringLocally() throws Exception {
        server.respond(PODS_PATH, 200, NO_PODS);

        client.sweepPods().withNamespace(NAMESPACE).withLabelSelector(SELECTOR).exec();

        String listRequest = server.requests().get(0);
        String decoded = URLDecoder.decode(listRequest, "UTF-8");
        assertTrue(decoded.startsWith("GET " + PODS_PATH + "?"),
                "expected a GET of the pods path with a query, got: " + decoded);
        assertTrue(decoded.contains("labelSelector=" + SELECTOR),
                "expected the selector in the list query, got: " + decoded);
    }

    /**
     * @implNote an absent selector matches every pod in the namespace, so this refusal is the only
     * thing standing between a convenience default and deleting the namespace's workload.
     */
    @Test
    void refusesToSweepWithoutALabelSelectorAndIssuesNoRequest() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> client.sweepPods().withNamespace(NAMESPACE).exec());

        assertTrue(thrown.getMessage().contains(NAMESPACE),
                "expected the refusal to name the namespace it protected, got: " + thrown.getMessage());
        assertEquals(Collections.emptyList(), server.requests(),
                "a refused sweep must not reach the api server at all");
    }

    @Test
    void toleratesAPodThatDisappearsBetweenTheListAndTheDelete() {
        server.respond(PODS_PATH, 200, TWO_PODS);
        server.respond(PODS_PATH + "/probe-1", 404, GONE);
        server.respond(PODS_PATH + "/probe-2", 200, DELETED);

        List<String> swept = client.sweepPods()
                .withNamespace(NAMESPACE).withLabelSelector(SELECTOR).exec();

        assertEquals(Arrays.asList("probe-1", "probe-2"), swept,
                "a pod already gone is still swept, and must not abort the pods after it");
    }

    @Test
    void forceRequestsAZeroGracePeriodOnEveryDelete() {
        server.respond(PODS_PATH, 200, TWO_PODS);
        server.respond(PODS_PATH + "/probe-1", 200, DELETED);
        server.respond(PODS_PATH + "/probe-2", 200, DELETED);

        client.sweepPods()
                .withNamespace(NAMESPACE).withLabelSelector(SELECTOR).withForce(true).exec();

        for (String request : server.deleteTargets()) {
            assertTrue(request.contains("gracePeriodSeconds=0"),
                    "expected a forced delete to ask for grace period 0, got: " + request);
        }
        assertEquals(2, server.deleteTargets().size());
    }

    private static final class FakeApiServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Map<String, Integer> statusByPath = new HashMap<>();
        private final Map<String, String> bodyByPath = new HashMap<>();
        private final List<String> requests = Collections.synchronizedList(new ArrayList<>());
        private volatile boolean running = true;

        FakeApiServer() throws IOException {
            serverSocket = new ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"));
            executor.submit(this::serve);
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        void respond(String path, int status, String body) {
            statusByPath.put(path, status);
            bodyByPath.put(path, body);
        }

        List<String> requests() {
            return new ArrayList<>(requests);
        }

        List<String> deleteRequests() {
            List<String> deletes = new ArrayList<>();
            for (String target : deleteTargets()) {
                deletes.add(target.split("\\?", 2)[0]);
            }
            return deletes;
        }

        List<String> deleteTargets() {
            List<String> deletes = new ArrayList<>();
            for (String request : requests()) {
                if (request.startsWith("DELETE ")) {
                    deletes.add(request);
                }
            }
            return deletes;
        }

        private void serve() {
            while (running) {
                try (Socket socket = serverSocket.accept()) {
                    String request = readRequestLine(socket.getInputStream());
                    requests.add(request);
                    String target = request.split(" ", 2).length > 1 ? request.split(" ", 2)[1] : "";
                    String path = target.split("\\?", 2)[0];
                    Integer status = statusByPath.get(path);
                    String body = bodyByPath.get(path);
                    if (status == null) {
                        status = 404;
                        body = "{\"kind\":\"Status\",\"reason\":\"NoStubbedResponse\"}";
                    }
                    write(socket.getOutputStream(), status, body);
                } catch (IOException stopping) {
                    // close() closes the listening socket, which is how this loop is meant to end.
                }
            }
        }

        /**
         * @implNote reads the WHOLE header block, not just the request line. Responding and closing
         * while request bytes are still unread makes the close a TCP reset on Linux and macOS, which
         * destroys the response the client has not read yet.
         */
        private static String readRequestLine(InputStream in) throws IOException {
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
            return parts.length > 1 ? parts[0] + " " + parts[1] : requestLine;
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
