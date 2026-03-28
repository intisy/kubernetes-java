package io.github.intisy.kubernetes.command.deployment;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to delete a deployment.
 *
 * @author Finn Birich
 */
public class DeleteDeploymentCmd {
    private final KubernetesHttpClient client;
    private final String deploymentName;
    private String namespace = "default";

    public DeleteDeploymentCmd(KubernetesHttpClient client, String deploymentName) {
        this.client = client;
        this.deploymentName = deploymentName;
    }

    public DeleteDeploymentCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public void exec() {
        try {
            String path = "/apis/apps/v1/namespaces/" + namespace + "/deployments/" + deploymentName;
            KubernetesResponse response = client.delete(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Deployment not found: " + deploymentName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete deployment: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete deployment", e);
        }
    }
}
