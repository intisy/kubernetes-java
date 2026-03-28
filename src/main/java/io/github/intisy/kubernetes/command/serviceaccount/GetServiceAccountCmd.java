package io.github.intisy.kubernetes.command.serviceaccount;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.ServiceAccount;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a service account.
 *
 * @author Finn Birich
 */
public class GetServiceAccountCmd {
    private final KubernetesHttpClient client;
    private final String serviceAccountName;
    private String namespace = "default";

    public GetServiceAccountCmd(KubernetesHttpClient client, String serviceAccountName) {
        this.client = client;
        this.serviceAccountName = serviceAccountName;
    }

    public GetServiceAccountCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ServiceAccount exec() {
        try {
            String path = "/api/v1/namespaces/" + namespace + "/serviceaccounts/" + serviceAccountName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("ServiceAccount not found: " + serviceAccountName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get service account: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), ServiceAccount.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get service account", e);
        }
    }
}
