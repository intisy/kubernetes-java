package io.github.intisy.kubernetes.command.role;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.Role;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list roles.
 *
 * @author Finn Birich
 */
public class ListRolesCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListRolesCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListRolesCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListRolesCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListRolesCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListRolesCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListRolesCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<Role> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = namespace != null
                    ? "/apis/rbac.authorization.k8s.io/v1/namespaces/" + namespace + "/roles"
                    : "/apis/rbac.authorization.k8s.io/v1/roles";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list roles: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<Role>>() {}.getType();
            KubernetesList<Role> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list roles", e);
        }
    }
}
