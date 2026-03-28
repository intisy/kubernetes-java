package io.github.intisy.kubernetes.command.limitrange;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.LimitRange;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list limit ranges.
 *
 * @author Finn Birich
 */
public class ListLimitRangesCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListLimitRangesCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListLimitRangesCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListLimitRangesCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListLimitRangesCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListLimitRangesCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListLimitRangesCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<LimitRange> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = namespace != null
                    ? "/api/v1/namespaces/" + namespace + "/limitranges"
                    : "/api/v1/limitranges";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list limit ranges: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<LimitRange>>() {}.getType();
            KubernetesList<LimitRange> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list limit ranges", e);
        }
    }
}
