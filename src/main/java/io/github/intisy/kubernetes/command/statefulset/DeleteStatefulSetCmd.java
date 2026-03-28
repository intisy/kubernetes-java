package io.github.intisy.kubernetes.command.statefulset;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to delete a stateful set.
 *
 * @author Finn Birich
 */
public class DeleteStatefulSetCmd {
    private final KubernetesHttpClient client;
    private final String statefulSetName;
    private String namespace = "default";

    public DeleteStatefulSetCmd(KubernetesHttpClient client, String statefulSetName) {
        this.client = client;
        this.statefulSetName = statefulSetName;
    }

    public DeleteStatefulSetCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public void exec() {
        try {
            String path = "/apis/apps/v1/namespaces/" + namespace + "/statefulsets/" + statefulSetName;
            KubernetesResponse response = client.delete(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("StatefulSet not found: " + statefulSetName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete stateful set: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete stateful set", e);
        }
    }
}
