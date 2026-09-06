package io.github.intisy.kubernetes.exec;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Runs a command in a pod over the Kubernetes exec WebSocket subprotocol.
 *
 * @implNote This is the thin, unavoidably un-unit-testable half of exec: the handshake needs a
 * real TLS socket and a real API server. {@link ExecChannels} carries all the frame parsing,
 * exit code and completeness logic that can be, and is, tested without either.
 */
public class PodExec {
    /**
     * @implNote Long enough for a {@code psql} query or a file read inside a pod, short enough
     * that a hung exec fails a durability drill instead of hanging it. Independent of the REST
     * client's own request timeout, which governs an entirely different kind of wait (an HTTP
     * response, not a command running inside a container).
     */
    public static final int DEFAULT_SESSION_TIMEOUT_MILLIS = 120000;

    private static final String SUBPROTOCOL = "v5.channel.k8s.io";

    private final String apiServerUrl;
    private final SSLSocketFactory sslSocketFactory;
    private final boolean verifyHostname;
    private final int connectTimeoutMillis;
    private final int sessionTimeoutMillis;

    public PodExec(String apiServerUrl, SSLSocketFactory sslSocketFactory, boolean verifyHostname, int connectTimeoutMillis) {
        this(apiServerUrl, sslSocketFactory, verifyHostname, connectTimeoutMillis, DEFAULT_SESSION_TIMEOUT_MILLIS);
    }

    public PodExec(String apiServerUrl, SSLSocketFactory sslSocketFactory, boolean verifyHostname,
                    int connectTimeoutMillis, int sessionTimeoutMillis) {
        this.apiServerUrl = apiServerUrl;
        this.sslSocketFactory = sslSocketFactory;
        this.verifyHostname = verifyHostname;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.sessionTimeoutMillis = sessionTimeoutMillis;
    }

    public ExecResult exec(String namespace, String pod, String[] command) throws IOException {
        String url = buildUrl(namespace, pod, command);
        final ExecChannels channels = new ExecChannels();
        final CountDownLatch closed = new CountDownLatch(1);

        WebSocketFactory factory = new WebSocketFactory();
        factory.setConnectionTimeout(connectTimeoutMillis);
        if (sslSocketFactory != null) {
            factory.setSSLSocketFactory(sslSocketFactory);
        }
        factory.setVerifyHostname(verifyHostname);

        WebSocket webSocket = factory.createSocket(url);
        webSocket.addProtocol(SUBPROTOCOL);
        webSocket.addListener(new WebSocketAdapter() {
            @Override
            public void onBinaryMessage(WebSocket websocket, byte[] binary) {
                channels.accept(binary);
            }

            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame,
                                        WebSocketFrame clientCloseFrame, boolean closedByServer) {
                closed.countDown();
            }
        });

        try {
            webSocket.connect();
        } catch (WebSocketException e) {
            throw new IOException("exec handshake failed for " + url, e);
        }

        String agreedProtocol = webSocket.getAgreedProtocol();
        if (!SUBPROTOCOL.equals(agreedProtocol)) {
            webSocket.disconnect();
            throw new IOException("server negotiated subprotocol '" + agreedProtocol
                    + "' instead of the requested '" + SUBPROTOCOL + "' for " + url);
        }

        String target = "pod " + namespace + "/" + pod + " running " + Arrays.toString(command);
        boolean finishedInTime;
        try {
            finishedInTime = closed.await(sessionTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            webSocket.disconnect();
            return channels.requireCompletedResult("interrupted while waiting for exec in " + target + " to finish");
        }
        webSocket.disconnect();

        if (!finishedInTime) {
            return channels.requireCompletedResult("timed out after " + sessionTimeoutMillis
                    + "ms waiting for exec in " + target + " to finish");
        }
        return channels.requireCompletedResult("exec socket for " + target + " closed before a terminal status was received");
    }

    private String buildUrl(String namespace, String pod, String[] command) {
        String base = apiServerUrl.startsWith("https") ? "wss" + apiServerUrl.substring("https".length())
                : "ws" + apiServerUrl.substring("http".length());
        StringBuilder query = new StringBuilder();
        for (String c : command) {
            query.append("command=").append(urlEncode(c)).append('&');
        }
        query.append("stdout=true&stderr=true&stdin=false");
        return base + "/api/v1/namespaces/" + urlEncode(namespace) + "/pods/" + urlEncode(pod) + "/exec?" + query;
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
