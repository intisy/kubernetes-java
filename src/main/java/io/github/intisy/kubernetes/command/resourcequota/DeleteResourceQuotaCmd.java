package io.github.intisy.kubernetes.command.resourcequota;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to delete a resource quota.
 *
 * @author Finn Birich
 */
public class DeleteResourceQuotaCmd {
    private final KubernetesHttpClient client;
    private final String resourceQuotaName;
    private String namespace = "default";
    private Integer gracePeriodSeconds;

    public DeleteResourceQuotaCmd(KubernetesHttpClient client, String resourceQuotaName) {
        this.client = client;
        this.resourceQuotaName = resourceQuotaName;
    }

    public DeleteResourceQuotaCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DeleteResourceQuotaCmd withGracePeriod(int seconds) {
        this.gracePeriodSeconds = seconds;
        return this;
    }

    public void exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (gracePeriodSeconds != null) {
                queryParams.put("gracePeriodSeconds", String.valueOf(gracePeriodSeconds));
            }

            String path = "/api/v1/namespaces/" + namespace + "/resourcequotas/" + resourceQuotaName;
            KubernetesResponse response = client.delete(path, queryParams);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("ResourceQuota not found: " + resourceQuotaName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete resource quota: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete resource quota", e);
        }
    }
}
