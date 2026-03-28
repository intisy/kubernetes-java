package io.github.intisy.kubernetes.command.namespace;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to delete a namespace.
 *
 * @author Finn Birich
 */
public class DeleteNamespaceCmd {
    private final KubernetesHttpClient client;
    private final String namespaceName;

    public DeleteNamespaceCmd(KubernetesHttpClient client, String namespaceName) {
        this.client = client;
        this.namespaceName = namespaceName;
    }

    public void exec() {
        try {
            KubernetesResponse response = client.delete("/api/v1/namespaces/" + namespaceName);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Namespace not found: " + namespaceName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete namespace: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete namespace", e);
        }
    }
}
