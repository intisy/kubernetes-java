package io.github.intisy.kubernetes.command.configmap;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to delete a config map.
 *
 * @author Finn Birich
 */
public class DeleteConfigMapCmd {
    private final KubernetesHttpClient client;
    private final String configMapName;
    private String namespace = "default";

    public DeleteConfigMapCmd(KubernetesHttpClient client, String configMapName) {
        this.client = client;
        this.configMapName = configMapName;
    }

    public DeleteConfigMapCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public void exec() {
        try {
            String path = "/api/v1/namespaces/" + namespace + "/configmaps/" + configMapName;
            KubernetesResponse response = client.delete(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("ConfigMap not found: " + configMapName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete config map: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete config map", e);
        }
    }
}
