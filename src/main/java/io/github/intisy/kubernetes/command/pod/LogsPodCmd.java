package io.github.intisy.kubernetes.command.pod;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;
import io.github.intisy.kubernetes.transport.StreamCallback;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to get pod logs.
 *
 * @author Finn Birich
 */
public class LogsPodCmd {
    private final KubernetesHttpClient client;
    private final String podName;
    private String namespace = "default";
    private String container;
    private Boolean follow;
    private Boolean previous;
    private Integer tailLines;
    private Integer sinceSeconds;
    private Boolean timestamps;

    public LogsPodCmd(KubernetesHttpClient client, String podName) {
        this.client = client;
        this.podName = podName;
    }

    public LogsPodCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public LogsPodCmd withContainer(String container) {
        this.container = container;
        return this;
    }

    public LogsPodCmd withFollow(boolean follow) {
        this.follow = follow;
        return this;
    }

    public LogsPodCmd withPrevious(boolean previous) {
        this.previous = previous;
        return this;
    }

    public LogsPodCmd withTailLines(int tailLines) {
        this.tailLines = tailLines;
        return this;
    }

    public LogsPodCmd withSinceSeconds(int sinceSeconds) {
        this.sinceSeconds = sinceSeconds;
        return this;
    }

    public LogsPodCmd withTimestamps(boolean timestamps) {
        this.timestamps = timestamps;
        return this;
    }

    /**
     * Execute and return logs as a string.
     */
    public String exec() {
        try {
            Map<String, String> queryParams = buildQueryParams();
            String path = "/api/v1/namespaces/" + namespace + "/pods/" + podName + "/log";
            KubernetesResponse response = client.get(path, queryParams);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Pod not found: " + podName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get pod logs: " + response.getBody(), response.getStatusCode());
            }

            return response.getBody();
        } catch (IOException e) {
            throw new KubernetesException("Failed to get pod logs", e);
        }
    }

    /**
     * Execute with streaming callback (for follow mode).
     */
    public void exec(StreamCallback<String> callback) {
        try {
            Map<String, String> queryParams = buildQueryParams();
            if (follow == null || !follow) {
                queryParams.put("follow", "true");
            }
            String path = "/api/v1/namespaces/" + namespace + "/pods/" + podName + "/log";
            client.getStream(path, queryParams, callback);
        } catch (IOException e) {
            callback.onError(e);
        }
    }

    private Map<String, String> buildQueryParams() {
        Map<String, String> queryParams = new HashMap<>();
        if (container != null) queryParams.put("container", container);
        if (follow != null) queryParams.put("follow", String.valueOf(follow));
        if (previous != null) queryParams.put("previous", String.valueOf(previous));
        if (tailLines != null) queryParams.put("tailLines", String.valueOf(tailLines));
        if (sinceSeconds != null) queryParams.put("sinceSeconds", String.valueOf(sinceSeconds));
        if (timestamps != null) queryParams.put("timestamps", String.valueOf(timestamps));
        return queryParams;
    }
}
