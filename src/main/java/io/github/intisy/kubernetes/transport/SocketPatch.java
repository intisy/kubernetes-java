package io.github.intisy.kubernetes.transport;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sends a single HTTP PATCH request over a raw socket.
 *
 * @implNote {@link java.net.HttpURLConnection#setRequestMethod(String)} rejects PATCH, and the
 * common workaround reflects into the connection's private {@code method} field. Over HTTPS that
 * connection is an {@code HttpsURLConnectionImpl} wrapper around an inner delegate that actually
 * writes the request line, so the reflected change on the wrapper never reaches the delegate and
 * the request still goes out as POST. Writing the request line and headers by hand over a socket
 * sidesteps {@code HttpURLConnection} entirely, so this problem cannot recur.
 */
public final class SocketPatch {
    private final SSLSocketFactory sslSocketFactory;
    private final boolean verifyHostname;
    private final int timeoutMs;
    private final String bearerToken;

    public SocketPatch(SSLSocketFactory sslSocketFactory, boolean verifyHostname,
                        int timeoutMs, String bearerToken) {
        this.sslSocketFactory = sslSocketFactory;
        this.verifyHostname = verifyHostname;
        this.timeoutMs = timeoutMs;
        this.bearerToken = bearerToken;
    }

    public KubernetesResponse send(URL url, String body, String contentType) throws IOException {
        boolean https = "https".equalsIgnoreCase(url.getProtocol());
        String host = url.getHost();
        int port = url.getPort() != -1 ? url.getPort() : (https ? 443 : 80);
        String path = url.getFile().isEmpty() ? "/" : url.getFile();
        byte[] bodyBytes = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];

        Socket socket = null;
        try {
            socket = https ? openSecureSocket(host, port) : openPlainSocket(host, port);
            socket.setSoTimeout(timeoutMs);

            OutputStream out = socket.getOutputStream();
            out.write(buildRequest(host, port, https, path, contentType, bodyBytes.length));
            if (bodyBytes.length > 0) {
                out.write(bodyBytes);
            }
            out.flush();

            return readResponse(socket.getInputStream());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private Socket openPlainSocket(String host, int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return socket;
        } catch (IOException e) {
            closeQuietly(socket);
            throw e;
        }
    }

    /**
     * @implNote A custom {@link javax.net.ssl.HostnameVerifier} run as the primary gate would
     * repeat the mistake this class replaces: {@code HttpsURLConnection} only ever falls back to
     * that verifier after its own JSSE endpoint identification has already rejected the
     * certificate, so {@link javax.net.ssl.HttpsURLConnection#getDefaultHostnameVerifier()} is a
     * deny-everything last resort, not a real check. Setting {@code setEndpointIdentificationAlgorithm("HTTPS")}
     * before the handshake instead lets JSSE itself match the certificate's DNS and IP subject
     * alternative names, exactly as an ordinary HTTPS connection does.
     */
    private Socket openSecureSocket(String host, int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket = sslSocketFactory.createSocket(socket, host, port, true);
            SSLSocket sslSocket = (SSLSocket) socket;
            sslSocket.setSoTimeout(timeoutMs);
            if (verifyHostname) {
                SSLParameters parameters = sslSocket.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                sslSocket.setSSLParameters(parameters);
            }
            sslSocket.startHandshake();
            return sslSocket;
        } catch (IOException e) {
            closeQuietly(socket);
            throw e;
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private byte[] buildRequest(String host, int port, boolean https, String path,
                                 String contentType, int contentLength) {
        boolean defaultPort = https ? port == 443 : port == 80;
        StringBuilder sb = new StringBuilder();
        sb.append("PATCH ").append(path).append(" HTTP/1.1\r\n");
        sb.append("Host: ").append(host);
        if (!defaultPort) {
            sb.append(':').append(port);
        }
        sb.append("\r\n");
        if (contentType != null) {
            sb.append("Content-Type: ").append(contentType).append("\r\n");
        }
        sb.append("Accept: application/json\r\n");
        sb.append("Content-Length: ").append(contentLength).append("\r\n");
        if (bearerToken != null) {
            sb.append("Authorization: Bearer ").append(bearerToken).append("\r\n");
        }
        sb.append("Connection: close\r\n");
        sb.append("\r\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private KubernetesResponse readResponse(InputStream in) throws IOException {
        String statusLine = readLine(in);
        if (statusLine == null) {
            throw new EOFException("server closed the connection before sending a response");
        }
        int statusCode = parseStatusCode(statusLine);

        Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
        String transferEncoding = null;
        long contentLength = -1;
        String line;
        while ((line = readLine(in)) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            List<String> values = headers.get(name);
            if (values == null) {
                values = new ArrayList<String>();
                headers.put(name, values);
            }
            values.add(value);

            if ("Transfer-Encoding".equalsIgnoreCase(name)) {
                transferEncoding = value;
            } else if ("Content-Length".equalsIgnoreCase(name)) {
                contentLength = Long.parseLong(value);
            }
        }

        String body;
        if (transferEncoding != null && transferEncoding.toLowerCase(Locale.ROOT).contains("chunked")) {
            body = readChunkedBody(in);
        } else if (contentLength >= 0) {
            body = new String(readExact(in, contentLength), StandardCharsets.UTF_8);
        } else {
            body = readUntilEof(in);
        }

        return new KubernetesResponse(statusCode, headers, body);
    }

    private int parseStatusCode(String statusLine) throws IOException {
        String[] parts = statusLine.split(" ", 3);
        if (parts.length < 2) {
            throw new IOException("malformed HTTP status line: " + statusLine);
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IOException("malformed HTTP status line: " + statusLine, e);
        }
    }

    private String readChunkedBody(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            String sizeLine = readLine(in);
            if (sizeLine == null) {
                break;
            }
            int semicolon = sizeLine.indexOf(';');
            String sizeToken = (semicolon >= 0 ? sizeLine.substring(0, semicolon) : sizeLine).trim();
            if (sizeToken.isEmpty()) {
                continue;
            }
            int chunkSize = Integer.parseInt(sizeToken, 16);
            if (chunkSize == 0) {
                String trailer;
                while ((trailer = readLine(in)) != null && !trailer.isEmpty()) {
                    // trailing headers after the final chunk carry no data we need
                }
                break;
            }
            out.write(readExact(in, chunkSize));
            readLine(in);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private byte[] readExact(InputStream in, long length) throws IOException {
        byte[] buffer = new byte[(int) length];
        int offset = 0;
        while (offset < length) {
            int read = in.read(buffer, offset, (int) (length - offset));
            if (read == -1) {
                throw new EOFException("stream ended after " + offset + " of " + length + " expected bytes");
            }
            offset += read;
        }
        return buffer;
    }

    private String readUntilEof(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream lineBytes = new ByteArrayOutputStream();
        int b;
        boolean any = false;
        while ((b = in.read()) != -1) {
            any = true;
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                lineBytes.write(b);
            }
        }
        if (!any) {
            return null;
        }
        return new String(lineBytes.toByteArray(), StandardCharsets.UTF_8);
    }
}
