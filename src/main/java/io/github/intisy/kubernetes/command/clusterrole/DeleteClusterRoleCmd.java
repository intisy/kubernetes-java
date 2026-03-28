package io.github.intisy.kubernetes.command.clusterrole;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to delete a cluster role.
 *
 * @author Finn Birich
 */
public class DeleteClusterRoleCmd {
    private final KubernetesHttpClient client;
    private final String clusterRoleName;
    private Integer gracePeriodSeconds;

    public DeleteClusterRoleCmd(KubernetesHttpClient client, String clusterRoleName) {
        this.client = client;
        this.clusterRoleName = clusterRoleName;
    }

    public DeleteClusterRoleCmd withGracePeriod(int seconds) {
        this.gracePeriodSeconds = seconds;
        return this;
    }

    public void exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (gracePeriodSeconds != null) {
                queryParams.put("gracePeriodSeconds", String.valueOf(gracePeriodSeconds));
            }

            String path = "/apis/rbac.authorization.k8s.io/v1/clusterroles/" + clusterRoleName;
            KubernetesResponse response = client.delete(path, queryParams);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("ClusterRole not found: " + clusterRoleName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete cluster role: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete cluster role", e);
        }
    }
}
