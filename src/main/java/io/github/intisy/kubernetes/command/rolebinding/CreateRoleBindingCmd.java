package io.github.intisy.kubernetes.command.rolebinding;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.RoleBinding;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a role binding.
 *
 * @author Finn Birich
 */
public class CreateRoleBindingCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private RoleBinding roleBinding;

    public CreateRoleBindingCmd(KubernetesHttpClient client, RoleBinding roleBinding) {
        this.client = client;
        this.roleBinding = roleBinding;
    }

    public CreateRoleBindingCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public RoleBinding exec() {
        try {
            String ns = namespace;
            if (roleBinding.getMetadata() != null && roleBinding.getMetadata().getNamespace() != null) {
                ns = roleBinding.getMetadata().getNamespace();
            }
            String path = "/apis/rbac.authorization.k8s.io/v1/namespaces/" + ns + "/rolebindings";
            KubernetesResponse response = client.post(path, roleBinding);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("RoleBinding already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create role binding: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), RoleBinding.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create role binding", e);
        }
    }
}
