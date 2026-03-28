package io.github.intisy.kubernetes.command.secret;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.Secret;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list secrets.
 *
 * @author Finn Birich
 */
public class ListSecretsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;

    public ListSecretsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListSecretsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListSecretsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public List<Secret> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);

            String path = "/api/v1/namespaces/" + namespace + "/secrets";
            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list secrets: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<Secret>>() {}.getType();
            KubernetesList<Secret> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list secrets", e);
        }
    }
}
