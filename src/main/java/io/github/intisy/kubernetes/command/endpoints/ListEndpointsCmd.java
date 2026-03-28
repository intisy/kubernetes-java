package io.github.intisy.kubernetes.command.endpoints;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.Endpoints;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list endpoints.
 *
 * @author Finn Birich
 */
public class ListEndpointsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListEndpointsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListEndpointsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListEndpointsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListEndpointsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListEndpointsCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListEndpointsCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<Endpoints> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = namespace != null
                    ? "/api/v1/namespaces/" + namespace + "/endpoints"
                    : "/api/v1/endpoints";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list endpoints: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<Endpoints>>() {}.getType();
            KubernetesList<Endpoints> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list endpoints", e);
        }
    }
}
