package io.github.intisy.kubernetes.command.secret;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.Secret;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a secret.
 *
 * @author Finn Birich
 */
public class GetSecretCmd {
    private final KubernetesHttpClient client;
    private final String secretName;
    private String namespace = "default";

    public GetSecretCmd(KubernetesHttpClient client, String secretName) {
        this.client = client;
        this.secretName = secretName;
    }

    public GetSecretCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Secret exec() {
        try {
            String path = "/api/v1/namespaces/" + namespace + "/secrets/" + secretName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Secret not found: " + secretName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get secret: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Secret.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get secret", e);
        }
    }
}
