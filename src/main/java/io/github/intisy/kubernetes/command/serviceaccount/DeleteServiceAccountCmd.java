package io.github.intisy.kubernetes.command.serviceaccount;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to delete a service account.
 *
 * @author Finn Birich
 */
public class DeleteServiceAccountCmd {
    private final KubernetesHttpClient client;
    private final String serviceAccountName;
    private String namespace = "default";
    private Integer gracePeriodSeconds;

    public DeleteServiceAccountCmd(KubernetesHttpClient client, String serviceAccountName) {
        this.client = client;
        this.serviceAccountName = serviceAccountName;
    }

    public DeleteServiceAccountCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DeleteServiceAccountCmd withGracePeriod(int seconds) {
        this.gracePeriodSeconds = seconds;
        return this;
    }

    public void exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (gracePeriodSeconds != null) {
                queryParams.put("gracePeriodSeconds", String.valueOf(gracePeriodSeconds));
            }

            String path = "/api/v1/namespaces/" + namespace + "/serviceaccounts/" + serviceAccountName;
            KubernetesResponse response = client.delete(path, queryParams);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("ServiceAccount not found: " + serviceAccountName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete service account: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete service account", e);
        }
    }
}
