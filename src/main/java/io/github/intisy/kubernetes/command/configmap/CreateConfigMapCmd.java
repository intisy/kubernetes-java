package io.github.intisy.kubernetes.command.configmap;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.ConfigMap;
import io.github.intisy.kubernetes.model.ObjectMeta;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to create a config map.
 *
 * @author Finn Birich
 */
public class CreateConfigMapCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String name;
    private Map<String, String> data;
    private Map<String, String> labels;
    private ConfigMap configMap;

    public CreateConfigMapCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public CreateConfigMapCmd(KubernetesHttpClient client, ConfigMap configMap) {
        this.client = client;
        this.configMap = configMap;
    }

    public CreateConfigMapCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public CreateConfigMapCmd withName(String name) {
        this.name = name;
        return this;
    }

    public CreateConfigMapCmd withData(String key, String value) {
        if (this.data == null) this.data = new HashMap<>();
        this.data.put(key, value);
        return this;
    }

    public CreateConfigMapCmd withLabel(String key, String value) {
        if (this.labels == null) this.labels = new HashMap<>();
        this.labels.put(key, value);
        return this;
    }

    public ConfigMap exec() {
        try {
            ConfigMap cm;
            if (this.configMap != null) {
                cm = this.configMap;
            } else {
                cm = new ConfigMap();
                ObjectMeta meta = new ObjectMeta();
                meta.setName(name);
                meta.setNamespace(namespace);
                if (labels != null) meta.setLabels(labels);
                cm.setMetadata(meta);
                if (data != null) cm.setData(data);
            }

            String ns = namespace;
            if (cm.getMetadata() != null && cm.getMetadata().getNamespace() != null) {
                ns = cm.getMetadata().getNamespace();
            }

            String path = "/api/v1/namespaces/" + ns + "/configmaps";
            KubernetesResponse response = client.post(path, cm);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("ConfigMap already exists: " + name);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create config map: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), ConfigMap.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create config map", e);
        }
    }
}
