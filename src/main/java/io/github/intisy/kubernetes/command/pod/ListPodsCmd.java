package io.github.intisy.kubernetes.command.pod;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.Pod;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list pods.
 *
 * @author Finn Birich
 */
public class ListPodsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListPodsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListPodsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListPodsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListPodsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListPodsCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListPodsCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<Pod> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = namespace != null
                    ? "/api/v1/namespaces/" + namespace + "/pods"
                    : "/api/v1/pods";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list pods: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<Pod>>() {}.getType();
            KubernetesList<Pod> podList = client.getGson().fromJson(response.getBody(), listType);
            return podList.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list pods", e);
        }
    }
}
