package io.github.intisy.kubernetes.command.configmap;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.ConfigMap;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list config maps.
 *
 * @author Finn Birich
 */
public class ListConfigMapsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;

    public ListConfigMapsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListConfigMapsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListConfigMapsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public List<ConfigMap> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);

            String path = "/api/v1/namespaces/" + namespace + "/configmaps";
            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list config maps: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<ConfigMap>>() {}.getType();
            KubernetesList<ConfigMap> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list config maps", e);
        }
    }
}
