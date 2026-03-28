package io.github.intisy.kubernetes.command.clusterrole;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.ClusterRole;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a cluster role.
 *
 * @author Finn Birich
 */
public class CreateClusterRoleCmd {
    private final KubernetesHttpClient client;
    private ClusterRole clusterRole;

    public CreateClusterRoleCmd(KubernetesHttpClient client, ClusterRole clusterRole) {
        this.client = client;
        this.clusterRole = clusterRole;
    }

    public ClusterRole exec() {
        try {
            String path = "/apis/rbac.authorization.k8s.io/v1/clusterroles";
            KubernetesResponse response = client.post(path, clusterRole);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("ClusterRole already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create cluster role: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), ClusterRole.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create cluster role", e);
        }
    }
}
