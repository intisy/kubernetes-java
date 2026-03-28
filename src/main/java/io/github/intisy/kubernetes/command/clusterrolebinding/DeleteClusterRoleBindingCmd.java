package io.github.intisy.kubernetes.command.clusterrolebinding;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to delete a cluster role binding.
 *
 * @author Finn Birich
 */
public class DeleteClusterRoleBindingCmd {
    private final KubernetesHttpClient client;
    private final String clusterRoleBindingName;
    private Integer gracePeriodSeconds;

    public DeleteClusterRoleBindingCmd(KubernetesHttpClient client, String clusterRoleBindingName) {
        this.client = client;
        this.clusterRoleBindingName = clusterRoleBindingName;
    }

    public DeleteClusterRoleBindingCmd withGracePeriod(int seconds) {
        this.gracePeriodSeconds = seconds;
        return this;
    }

    public void exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (gracePeriodSeconds != null) {
                queryParams.put("gracePeriodSeconds", String.valueOf(gracePeriodSeconds));
            }

            String path = "/apis/rbac.authorization.k8s.io/v1/clusterrolebindings/" + clusterRoleBindingName;
            KubernetesResponse response = client.delete(path, queryParams);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("ClusterRoleBinding not found: " + clusterRoleBindingName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete cluster role binding: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete cluster role binding", e);
        }
    }
}
