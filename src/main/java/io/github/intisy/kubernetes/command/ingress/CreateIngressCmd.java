package io.github.intisy.kubernetes.command.ingress;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.Ingress;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create an ingress.
 *
 * @author Finn Birich
 */
public class CreateIngressCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private Ingress ingress;

    public CreateIngressCmd(KubernetesHttpClient client, Ingress ingress) {
        this.client = client;
        this.ingress = ingress;
    }

    public CreateIngressCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Ingress exec() {
        try {
            String ns = namespace;
            if (ingress.getMetadata() != null && ingress.getMetadata().getNamespace() != null) {
                ns = ingress.getMetadata().getNamespace();
            }
            String path = "/apis/networking.k8s.io/v1/namespaces/" + ns + "/ingresses";
            KubernetesResponse response = client.post(path, ingress);
            if (response.getStatusCode() == 409) {
                throw new ConflictException("Ingress already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create ingress: " + response.getBody(), response.getStatusCode());
            }
            return client.getGson().fromJson(response.getBody(), Ingress.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create ingress", e);
        }
    }
}
