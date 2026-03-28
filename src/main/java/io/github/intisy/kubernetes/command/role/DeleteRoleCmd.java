package io.github.intisy.kubernetes.command.role;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to delete a role.
 *
 * @author Finn Birich
 */
public class DeleteRoleCmd {
    private final KubernetesHttpClient client;
    private final String roleName;
    private String namespace = "default";
    private Integer gracePeriodSeconds;

    public DeleteRoleCmd(KubernetesHttpClient client, String roleName) {
        this.client = client;
        this.roleName = roleName;
    }

    public DeleteRoleCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DeleteRoleCmd withGracePeriod(int seconds) {
        this.gracePeriodSeconds = seconds;
        return this;
    }

    public void exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (gracePeriodSeconds != null) {
                queryParams.put("gracePeriodSeconds", String.valueOf(gracePeriodSeconds));
            }

            String path = "/apis/rbac.authorization.k8s.io/v1/namespaces/" + namespace + "/roles/" + roleName;
            KubernetesResponse response = client.delete(path, queryParams);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Role not found: " + roleName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete role: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete role", e);
        }
    }
}
