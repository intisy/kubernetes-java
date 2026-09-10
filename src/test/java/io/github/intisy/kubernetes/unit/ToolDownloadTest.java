package io.github.intisy.kubernetes.unit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.intisy.kubernetes.KubernetesClient;
import io.github.intisy.kubernetes.KubernetesProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The contract {@link KubernetesProvider#downloadFile(String, java.nio.file.Path)} is held to, so
 * that folding the three platform providers' byte identical private copies onto one body is a
 * refactor rather than a rewrite.
 *
 * @author Finn Birich
 * @implNote a real {@link HttpServer} on port 0 rather than a mocked connection, because the body
 * under test is {@link java.net.HttpURLConnection} and the three properties worth pinning are all
 * properties of a real exchange: the bytes arrive intact, a 4xx is raised rather than written to
 * disk, and a redirect is followed. Port 0 lets the OS pick a free port, so the suite never
 * collides with anything already listening, and nothing leaves the loopback interface.
 */
public class ToolDownloadTest {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private HttpServer server;
    private String baseUrl;

    @TempDir
    Path downloadDirectory;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    @DisplayName("the bytes arrive intact")
    void writesTheBodyToTheDestination() throws IOException {
        byte[] payload = payloadOfEveryByteValue();
        serve("/minikube", 200, payload);
        Path destination = downloadDirectory.resolve("minikube");

        TestableProvider.download(baseUrl + "/minikube", destination);

        assertArrayEquals(payload, Files.readAllBytes(destination),
                "the downloaded file should be the served bytes, unaltered");
    }

    @Test
    @DisplayName("an existing file at the destination is replaced")
    void replacesAnExistingFile() throws IOException {
        Path destination = downloadDirectory.resolve("kubectl");
        Files.write(destination, "stale".getBytes(UTF_8));
        serve("/kubectl", 200, "fresh".getBytes(UTF_8));

        TestableProvider.download(baseUrl + "/kubectl", destination);

        assertArrayEquals("fresh".getBytes(UTF_8), Files.readAllBytes(destination),
                "an earlier download should have been overwritten");
    }

    @Test
    @DisplayName("a 404 is raised and names the status code")
    void raisesTheStatusCodeOnNotFound() {
        serve("/missing", 404, new byte[0]);
        Path destination = downloadDirectory.resolve("missing");

        IOException raised = assertThrows(IOException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws IOException {
                TestableProvider.download(baseUrl + "/missing", destination);
            }
        });

        assertTrue(raised.getMessage().contains("404"),
                "the failure should name the status code, but said: " + raised.getMessage());
    }

    @Test
    @DisplayName("a 404 writes nothing to the destination")
    void writesNothingWhenTheServerRefuses() {
        serve("/missing", 404, new byte[0]);
        final Path destination = downloadDirectory.resolve("missing");

        assertThrows(IOException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws IOException {
                TestableProvider.download(baseUrl + "/missing", destination);
            }
        });

        assertTrue(Files.notExists(destination),
                "a refused download should not have created the destination file");
    }

    /**
     * @implNote the published Minikube and kubectl URLs redirect to a CDN, so this is the real
     * shape of every download this method performs in production, not a hypothetical.
     */
    @Test
    @DisplayName("a 301 is followed to the redirect target")
    void followsARedirect() throws IOException {
        serve("/actual", 200, "binary".getBytes(UTF_8));
        redirect("/redirected", 301, baseUrl + "/actual");
        Path destination = downloadDirectory.resolve("redirected");

        TestableProvider.download(baseUrl + "/redirected", destination);

        assertArrayEquals("binary".getBytes(UTF_8), Files.readAllBytes(destination),
                "the redirect target's body should have been written, not the redirect response");
    }

    private static byte[] payloadOfEveryByteValue() {
        byte[] payload = new byte[256];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) i;
        }
        return payload;
    }

    private void serve(String path, final int status, final byte[] body) {
        server.createContext(path, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
                if (body.length > 0) {
                    OutputStream responseBody = exchange.getResponseBody();
                    try {
                        responseBody.write(body);
                    } finally {
                        responseBody.close();
                    }
                }
                exchange.close();
            }
        });
    }

    private void redirect(String path, final int status, final String target) {
        server.createContext(path, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getResponseHeaders().add("Location", target);
                exchange.sendResponseHeaders(status, -1);
                exchange.close();
            }
        });
    }

    /**
     * @implNote {@code downloadFile} is protected on the provider base, so a subclass is what makes
     * it reachable from this package, the same way {@code ProviderLifecycleTest} reaches the
     * registration hooks. The abstract methods are never called: only the static helper is.
     */
    private static final class TestableProvider extends KubernetesProvider {

        static void download(String url, Path destination) throws IOException {
            downloadFile(url, destination);
        }

        @Override
        public String getInstanceId() {
            throw new UnsupportedOperationException("Not used in the download tests");
        }

        @Override
        public void start() {
            throw new UnsupportedOperationException("Not used in the download tests");
        }

        @Override
        public KubernetesClient createClient() {
            throw new UnsupportedOperationException("Not used in the download tests");
        }

        @Override
        public KubernetesClient getClient() {
            throw new UnsupportedOperationException("Not used in the download tests");
        }

        @Override
        public void stop() {
            throw new UnsupportedOperationException("Not used in the download tests");
        }

        @Override
        public void ensureInstalled() {
            throw new UnsupportedOperationException("Not used in the download tests");
        }
    }
}
