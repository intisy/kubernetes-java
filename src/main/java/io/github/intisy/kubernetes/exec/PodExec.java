package io.github.intisy.kubernetes.exec;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Runs a command in a pod over the Kubernetes exec WebSocket subprotocol.
 *
 * @implNote This is the thin, unavoidably un-unit-testable half of exec: the handshake needs a
 * real TLS socket and a real API server. {@link ExecChannels} carries all the frame parsing and
 * exit code logic that can be, and is, tested without either.
 */
public class PodExec {
    private static final Logger log = LoggerFactory.getLogger(PodExec.class);
    private static final String SUBPROTOCOL = "v5.channel.k8s.io";

    private final String apiServerUrl;
    private final SSLSocketFactory sslSocketFactory;
    private final boolean verifyHostname;
    private final int timeoutMillis;

    public PodExec(String apiServerUrl, SSLSocketFactory sslSocketFactory, boolean verifyHostname, int timeoutMillis) {
        this.apiServerUrl = apiServerUrl;
        this.sslSocketFactory = sslSocketFactory;
        this.verifyHostname = verifyHostname;
        this.timeoutMillis = timeoutMillis;
    }

    public ExecResult exec(String namespace, String pod, String[] command) throws IOException {
        String url = buildUrl(namespace, pod, command);
        final ExecChannels channels = new ExecChannels();
        final CountDownLatch closed = new CountDownLatch(1);

        WebSocketFactory factory = new WebSocketFactory();
        factory.setConnectionTimeout(timeoutMillis);
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

        try {
            if (!closed.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                log.warn("exec against {} did not close within {}ms; returning what was received so far", url, timeoutMillis);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            webSocket.disconnect();
        }

        return channels.result();
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
