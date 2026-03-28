package io.github.intisy.kubernetes.command.ingress;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.Ingress;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) an ingress.
 *
 * @author Finn Birich
 */
public class GetIngressCmd {
    private final KubernetesHttpClient client;
    private final String ingressName;
    private String namespace = "default";

    public GetIngressCmd(KubernetesHttpClient client, String ingressName) {
        this.client = client;
        this.ingressName = ingressName;
    }

    public GetIngressCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Ingress exec() {
        try {
            String path = "/apis/networking.k8s.io/v1/namespaces/" + namespace + "/ingresses/" + ingressName;
            KubernetesResponse response = client.get(path);
            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Ingress not found: " + ingressName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get ingress: " + response.getBody(), response.getStatusCode());
            }
            return client.getGson().fromJson(response.getBody(), Ingress.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get ingress", e);
        }
    }
}
