package io.github.intisy.kubernetes.command.hpa;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.HorizontalPodAutoscaler;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list horizontal pod autoscalers.
 *
 * @author Finn Birich
 */
public class ListHorizontalPodAutoscalersCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListHorizontalPodAutoscalersCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListHorizontalPodAutoscalersCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListHorizontalPodAutoscalersCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListHorizontalPodAutoscalersCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListHorizontalPodAutoscalersCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListHorizontalPodAutoscalersCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<HorizontalPodAutoscaler> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = namespace != null
                    ? "/apis/autoscaling/v1/namespaces/" + namespace + "/horizontalpodautoscalers"
                    : "/apis/autoscaling/v1/horizontalpodautoscalers";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list horizontal pod autoscalers: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<HorizontalPodAutoscaler>>() {}.getType();
            KubernetesList<HorizontalPodAutoscaler> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list horizontal pod autoscalers", e);
        }
    }
}
