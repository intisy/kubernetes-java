package io.github.intisy.kubernetes.command.configmap;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.ConfigMap;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a config map.
 *
 * @author Finn Birich
 */
public class GetConfigMapCmd {
    private final KubernetesHttpClient client;
    private final String configMapName;
    private String namespace = "default";

    public GetConfigMapCmd(KubernetesHttpClient client, String configMapName) {
        this.client = client;
        this.configMapName = configMapName;
    }

    public GetConfigMapCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ConfigMap exec() {
        try {
            String path = "/api/v1/namespaces/" + namespace + "/configmaps/" + configMapName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("ConfigMap not found: " + configMapName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get config map: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), ConfigMap.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get config map", e);
        }
    }
}
