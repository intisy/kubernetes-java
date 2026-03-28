package io.github.intisy.kubernetes.command.clusterrolebinding;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.ClusterRoleBinding;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a cluster role binding.
 *
 * @author Finn Birich
 */
public class CreateClusterRoleBindingCmd {
    private final KubernetesHttpClient client;
    private ClusterRoleBinding clusterRoleBinding;

    public CreateClusterRoleBindingCmd(KubernetesHttpClient client, ClusterRoleBinding clusterRoleBinding) {
        this.client = client;
        this.clusterRoleBinding = clusterRoleBinding;
    }

    public ClusterRoleBinding exec() {
        try {
            String path = "/apis/rbac.authorization.k8s.io/v1/clusterrolebindings";
            KubernetesResponse response = client.post(path, clusterRoleBinding);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("ClusterRoleBinding already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create cluster role binding: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), ClusterRoleBinding.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create cluster role binding", e);
        }
    }
}
