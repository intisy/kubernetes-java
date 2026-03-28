package io.github.intisy.kubernetes.command.clusterrole;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.ClusterRole;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list cluster roles.
 *
 * @author Finn Birich
 */
public class ListClusterRolesCmd {
    private final KubernetesHttpClient client;
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListClusterRolesCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListClusterRolesCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListClusterRolesCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListClusterRolesCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<ClusterRole> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = "/apis/rbac.authorization.k8s.io/v1/clusterroles";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list cluster roles: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<ClusterRole>>() {}.getType();
            KubernetesList<ClusterRole> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list cluster roles", e);
        }
    }
}
