package io.github.intisy.kubernetes.command.serviceaccount;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.ServiceAccount;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list service accounts.
 *
 * @author Finn Birich
 */
public class ListServiceAccountsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListServiceAccountsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListServiceAccountsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListServiceAccountsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListServiceAccountsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListServiceAccountsCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListServiceAccountsCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<ServiceAccount> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = namespace != null
                    ? "/api/v1/namespaces/" + namespace + "/serviceaccounts"
                    : "/api/v1/serviceaccounts";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list service accounts: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<ServiceAccount>>() {}.getType();
            KubernetesList<ServiceAccount> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list service accounts", e);
        }
    }
}
