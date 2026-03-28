package io.github.intisy.kubernetes.command.clusterrolebinding;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.ClusterRoleBinding;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list cluster role bindings.
 *
 * @author Finn Birich
 */
public class ListClusterRoleBindingsCmd {
    private final KubernetesHttpClient client;
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListClusterRoleBindingsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListClusterRoleBindingsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListClusterRoleBindingsCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListClusterRoleBindingsCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<ClusterRoleBinding> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = "/apis/rbac.authorization.k8s.io/v1/clusterrolebindings";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list cluster role bindings: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<ClusterRoleBinding>>() {}.getType();
            KubernetesList<ClusterRoleBinding> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list cluster role bindings", e);
        }
    }
}
