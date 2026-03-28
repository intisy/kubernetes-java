package io.github.intisy.kubernetes.command.system;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to check Kubernetes API server health.
 *
 * @author Finn Birich
 */
public class HealthzCmd {
    private final KubernetesHttpClient client;

    public HealthzCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    /**
     * Execute and return true if healthy.
     */
    public boolean exec() {
        try {
            KubernetesResponse response = client.get("/healthz");
            return response.isSuccessful() && "ok".equals(response.getBody().trim());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Execute and throw if not healthy.
     */
    public void execOrThrow() {
        try {
            KubernetesResponse response = client.get("/healthz");

            if (!response.isSuccessful()) {
                throw new KubernetesException("API server is not healthy: " + response.getBody(), response.getStatusCode());
            }

            if (!"ok".equals(response.getBody().trim())) {
                throw new KubernetesException("API server health check failed: " + response.getBody(), 500);
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to check API server health", e);
        }
    }
}
