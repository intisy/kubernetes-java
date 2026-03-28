package io.github.intisy.kubernetes.transport;

import java.util.List;
import java.util.Map;

/**
 * Represents an HTTP response from the Kubernetes API server.
 *
 * @author Finn Birich
 */
public class KubernetesResponse {
    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final String body;

    public KubernetesResponse(int statusCode, Map<String, List<String>> headers, String body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    @Override
    public String toString() {
        return "KubernetesResponse{" +
                "statusCode=" + statusCode +
                ", bodyLength=" + (body != null ? body.length() : 0) +
                '}';
    }
}
