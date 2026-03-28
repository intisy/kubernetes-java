package io.github.intisy.kubernetes.command.system;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.VersionInfo;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get Kubernetes API server version.
 *
 * @author Finn Birich
 */
public class VersionCmd {
    private final KubernetesHttpClient client;

    public VersionCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public VersionInfo exec() {
        try {
            KubernetesResponse response = client.get("/version");

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get version: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), VersionInfo.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get version", e);
        }
    }
}
