package io.github.intisy.kubernetes.command.secret;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to delete a secret.
 *
 * @author Finn Birich
 */
public class DeleteSecretCmd {
    private final KubernetesHttpClient client;
    private final String secretName;
    private String namespace = "default";

    public DeleteSecretCmd(KubernetesHttpClient client, String secretName) {
        this.client = client;
        this.secretName = secretName;
    }

    public DeleteSecretCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public void exec() {
        try {
            String path = "/api/v1/namespaces/" + namespace + "/secrets/" + secretName;
            KubernetesResponse response = client.delete(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Secret not found: " + secretName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete secret: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete secret", e);
        }
    }
}
