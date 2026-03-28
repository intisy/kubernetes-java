package io.github.intisy.kubernetes.command.ingress;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to delete an ingress.
 *
 * @author Finn Birich
 */
public class DeleteIngressCmd {
    private final KubernetesHttpClient client;
    private final String ingressName;
    private String namespace = "default";

    public DeleteIngressCmd(KubernetesHttpClient client, String ingressName) {
        this.client = client;
        this.ingressName = ingressName;
    }

    public DeleteIngressCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public void exec() {
        try {
            String path = "/apis/networking.k8s.io/v1/namespaces/" + namespace + "/ingresses/" + ingressName;
            KubernetesResponse response = client.delete(path);
            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Ingress not found: " + ingressName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete ingress: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete ingress", e);
        }
    }
}
