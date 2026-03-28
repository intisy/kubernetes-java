package io.github.intisy.kubernetes.command.resourcequota;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.ResourceQuota;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list resource quotas.
 *
 * @author Finn Birich
 */
public class ListResourceQuotasCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListResourceQuotasCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListResourceQuotasCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListResourceQuotasCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListResourceQuotasCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListResourceQuotasCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListResourceQuotasCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<ResourceQuota> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = namespace != null
                    ? "/api/v1/namespaces/" + namespace + "/resourcequotas"
                    : "/api/v1/resourcequotas";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list resource quotas: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<ResourceQuota>>() {}.getType();
            KubernetesList<ResourceQuota> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list resource quotas", e);
        }
    }
}
