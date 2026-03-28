package io.github.intisy.kubernetes.command.daemonset;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to delete a daemon set.
 *
 * @author Finn Birich
 */
public class DeleteDaemonSetCmd {
    private final KubernetesHttpClient client;
    private final String daemonSetName;
    private String namespace = "default";

    public DeleteDaemonSetCmd(KubernetesHttpClient client, String daemonSetName) {
        this.client = client;
        this.daemonSetName = daemonSetName;
    }

    public DeleteDaemonSetCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public void exec() {
        try {
            String path = "/apis/apps/v1/namespaces/" + namespace + "/daemonsets/" + daemonSetName;
            KubernetesResponse response = client.delete(path);
            if (response.getStatusCode() == 404) {
                throw new NotFoundException("DaemonSet not found: " + daemonSetName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete daemon set: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete daemon set", e);
        }
    }
}
