package io.github.intisy.kubernetes.command.role;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.Role;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a role.
 *
 * @author Finn Birich
 */
public class CreateRoleCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private Role role;

    public CreateRoleCmd(KubernetesHttpClient client, Role role) {
        this.client = client;
        this.role = role;
    }

    public CreateRoleCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Role exec() {
        try {
            String ns = namespace;
            if (role.getMetadata() != null && role.getMetadata().getNamespace() != null) {
                ns = role.getMetadata().getNamespace();
            }
            String path = "/apis/rbac.authorization.k8s.io/v1/namespaces/" + ns + "/roles";
            KubernetesResponse response = client.post(path, role);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("Role already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create role: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Role.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create role", e);
        }
    }
}
