package io.github.intisy.kubernetes.command.serviceaccount;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.ServiceAccount;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a service account.
 *
 * @author Finn Birich
 */
public class CreateServiceAccountCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private ServiceAccount serviceAccount;

    public CreateServiceAccountCmd(KubernetesHttpClient client, ServiceAccount serviceAccount) {
        this.client = client;
        this.serviceAccount = serviceAccount;
    }

    public CreateServiceAccountCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ServiceAccount exec() {
        try {
            String ns = namespace;
            if (serviceAccount.getMetadata() != null && serviceAccount.getMetadata().getNamespace() != null) {
                ns = serviceAccount.getMetadata().getNamespace();
            }
            String path = "/api/v1/namespaces/" + ns + "/serviceaccounts";
            KubernetesResponse response = client.post(path, serviceAccount);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("ServiceAccount already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create service account: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), ServiceAccount.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create service account", e);
        }
    }
}
