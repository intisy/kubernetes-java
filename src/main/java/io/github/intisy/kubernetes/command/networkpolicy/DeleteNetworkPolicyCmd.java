package io.github.intisy.kubernetes.command.networkpolicy;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to delete a network policy.
 *
 * @author Finn Birich
 */
public class DeleteNetworkPolicyCmd {
    private final KubernetesHttpClient client;
    private final String networkPolicyName;
    private String namespace = "default";

    public DeleteNetworkPolicyCmd(KubernetesHttpClient client, String networkPolicyName) {
        this.client = client;
        this.networkPolicyName = networkPolicyName;
    }

    public DeleteNetworkPolicyCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public void exec() {
        try {
            String path = "/apis/networking.k8s.io/v1/namespaces/" + namespace + "/networkpolicies/" + networkPolicyName;
            KubernetesResponse response = client.delete(path);
            if (response.getStatusCode() == 404) {
                throw new NotFoundException("NetworkPolicy not found: " + networkPolicyName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete network policy: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete network policy", e);
        }
    }
}
