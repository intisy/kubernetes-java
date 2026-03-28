package io.github.intisy.kubernetes.command.rolebinding;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.RoleBinding;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list role bindings.
 *
 * @author Finn Birich
 */
public class ListRoleBindingsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListRoleBindingsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListRoleBindingsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListRoleBindingsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListRoleBindingsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListRoleBindingsCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListRoleBindingsCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<RoleBinding> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = namespace != null
                    ? "/apis/rbac.authorization.k8s.io/v1/namespaces/" + namespace + "/rolebindings"
                    : "/apis/rbac.authorization.k8s.io/v1/rolebindings";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list role bindings: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<RoleBinding>>() {}.getType();
            KubernetesList<RoleBinding> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list role bindings", e);
        }
    }
}
