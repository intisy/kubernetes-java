package io.github.intisy.kubernetes.command.namespace;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.Namespace;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a namespace.
 *
 * @author Finn Birich
 */
public class GetNamespaceCmd {
    private final KubernetesHttpClient client;
    private final String namespaceName;

    public GetNamespaceCmd(KubernetesHttpClient client, String namespaceName) {
        this.client = client;
        this.namespaceName = namespaceName;
    }

    public Namespace exec() {
        try {
            KubernetesResponse response = client.get("/api/v1/namespaces/" + namespaceName);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Namespace not found: " + namespaceName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get namespace: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Namespace.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get namespace", e);
        }
    }
}
