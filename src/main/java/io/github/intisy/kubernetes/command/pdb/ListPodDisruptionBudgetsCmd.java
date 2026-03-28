package io.github.intisy.kubernetes.command.pdb;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.PodDisruptionBudget;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list pod disruption budgets.
 *
 * @author Finn Birich
 */
public class ListPodDisruptionBudgetsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListPodDisruptionBudgetsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListPodDisruptionBudgetsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListPodDisruptionBudgetsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListPodDisruptionBudgetsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListPodDisruptionBudgetsCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListPodDisruptionBudgetsCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<PodDisruptionBudget> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = namespace != null
                    ? "/apis/policy/v1/namespaces/" + namespace + "/poddisruptionbudgets"
                    : "/apis/policy/v1/poddisruptionbudgets";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list pod disruption budgets: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<PodDisruptionBudget>>() {}.getType();
            KubernetesList<PodDisruptionBudget> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list pod disruption budgets", e);
        }
    }
}
