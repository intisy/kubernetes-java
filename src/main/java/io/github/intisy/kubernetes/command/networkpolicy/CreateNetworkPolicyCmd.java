package io.github.intisy.kubernetes.command.networkpolicy;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.NetworkPolicy;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a network policy.
 *
 * @author Finn Birich
 */
public class CreateNetworkPolicyCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private NetworkPolicy networkPolicy;

    public CreateNetworkPolicyCmd(KubernetesHttpClient client, NetworkPolicy networkPolicy) {
        this.client = client;
        this.networkPolicy = networkPolicy;
    }

    public CreateNetworkPolicyCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public NetworkPolicy exec() {
        try {
            String ns = namespace;
            if (networkPolicy.getMetadata() != null && networkPolicy.getMetadata().getNamespace() != null) {
                ns = networkPolicy.getMetadata().getNamespace();
            }
            String path = "/apis/networking.k8s.io/v1/namespaces/" + ns + "/networkpolicies";
            KubernetesResponse response = client.post(path, networkPolicy);
            if (response.getStatusCode() == 409) {
                throw new ConflictException("NetworkPolicy already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create network policy: " + response.getBody(), response.getStatusCode());
            }
            return client.getGson().fromJson(response.getBody(), NetworkPolicy.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create network policy", e);
        }
    }
}
