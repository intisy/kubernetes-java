package io.github.intisy.kubernetes.command.namespace;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.Namespace;
import io.github.intisy.kubernetes.model.ObjectMeta;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a namespace.
 *
 * @author Finn Birich
 */
public class CreateNamespaceCmd {
    private final KubernetesHttpClient client;
    private String name;

    public CreateNamespaceCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public CreateNamespaceCmd withName(String name) {
        this.name = name;
        return this;
    }

    public Namespace exec() {
        try {
            Namespace ns = new Namespace();
            ObjectMeta meta = new ObjectMeta();
            meta.setName(name);
            ns.setMetadata(meta);

            KubernetesResponse response = client.post("/api/v1/namespaces", ns);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("Namespace already exists: " + name);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create namespace: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Namespace.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create namespace", e);
        }
    }
}
