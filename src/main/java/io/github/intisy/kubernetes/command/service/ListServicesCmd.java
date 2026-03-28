package io.github.intisy.kubernetes.command.service;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.Service;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list services.
 *
 * @author Finn Birich
 */
public class ListServicesCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;

    public ListServicesCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListServicesCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListServicesCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListServicesCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public List<Service> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);

            String path = namespace != null
                    ? "/api/v1/namespaces/" + namespace + "/services"
                    : "/api/v1/services";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list services: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<Service>>() {}.getType();
            KubernetesList<Service> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list services", e);
        }
    }
}
