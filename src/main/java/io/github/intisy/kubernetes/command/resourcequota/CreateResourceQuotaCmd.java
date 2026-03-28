package io.github.intisy.kubernetes.command.resourcequota;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.ResourceQuota;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a resource quota.
 *
 * @author Finn Birich
 */
public class CreateResourceQuotaCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private ResourceQuota resourceQuota;

    public CreateResourceQuotaCmd(KubernetesHttpClient client, ResourceQuota resourceQuota) {
        this.client = client;
        this.resourceQuota = resourceQuota;
    }

    public CreateResourceQuotaCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ResourceQuota exec() {
        try {
            String ns = namespace;
            if (resourceQuota.getMetadata() != null && resourceQuota.getMetadata().getNamespace() != null) {
                ns = resourceQuota.getMetadata().getNamespace();
            }
            String path = "/api/v1/namespaces/" + ns + "/resourcequotas";
            KubernetesResponse response = client.post(path, resourceQuota);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("ResourceQuota already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create resource quota: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), ResourceQuota.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create resource quota", e);
        }
    }
}
