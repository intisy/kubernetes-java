package io.github.intisy.kubernetes.command.networkpolicy;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.NetworkPolicy;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a network policy.
 *
 * @author Finn Birich
 */
public class GetNetworkPolicyCmd {
    private final KubernetesHttpClient client;
    private final String networkPolicyName;
    private String namespace = "default";

    public GetNetworkPolicyCmd(KubernetesHttpClient client, String networkPolicyName) {
        this.client = client;
        this.networkPolicyName = networkPolicyName;
    }

    public GetNetworkPolicyCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public NetworkPolicy exec() {
        try {
            String path = "/apis/networking.k8s.io/v1/namespaces/" + namespace + "/networkpolicies/" + networkPolicyName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("NetworkPolicy not found: " + networkPolicyName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get network policy: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), NetworkPolicy.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get network policy", e);
        }
    }
}
