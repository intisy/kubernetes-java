package io.github.intisy.kubernetes.command.secret;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.ObjectMeta;
import io.github.intisy.kubernetes.model.Secret;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to create a secret.
 *
 * @author Finn Birich
 */
public class CreateSecretCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String name;
    private String type = "Opaque";
    private Map<String, String> stringData;
    private Map<String, String> labels;
    private Secret secret;

    public CreateSecretCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public CreateSecretCmd(KubernetesHttpClient client, Secret secret) {
        this.client = client;
        this.secret = secret;
    }

    public CreateSecretCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public CreateSecretCmd withName(String name) {
        this.name = name;
        return this;
    }

    public CreateSecretCmd withType(String type) {
        this.type = type;
        return this;
    }

    public CreateSecretCmd withStringData(String key, String value) {
        if (this.stringData == null) this.stringData = new HashMap<>();
        this.stringData.put(key, value);
        return this;
    }

    public CreateSecretCmd withLabel(String key, String value) {
        if (this.labels == null) this.labels = new HashMap<>();
        this.labels.put(key, value);
        return this;
    }

    public Secret exec() {
        try {
            Secret s;
            if (this.secret != null) {
                s = this.secret;
            } else {
                s = new Secret();
                ObjectMeta meta = new ObjectMeta();
                meta.setName(name);
                meta.setNamespace(namespace);
                if (labels != null) meta.setLabels(labels);
                s.setMetadata(meta);
                s.setType(type);
                if (stringData != null) s.setStringData(stringData);
            }

            String ns = namespace;
            if (s.getMetadata() != null && s.getMetadata().getNamespace() != null) {
                ns = s.getMetadata().getNamespace();
            }

            String path = "/api/v1/namespaces/" + ns + "/secrets";
            KubernetesResponse response = client.post(path, s);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("Secret already exists: " + name);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create secret: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Secret.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create secret", e);
        }
    }
}
