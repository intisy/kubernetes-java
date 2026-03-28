package io.github.intisy.kubernetes.command.deployment;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.Deployment;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to scale a deployment.
 *
 * @author Finn Birich
 */
public class ScaleDeploymentCmd {
    private final KubernetesHttpClient client;
    private final String deploymentName;
    private String namespace = "default";
    private int replicas;

    public ScaleDeploymentCmd(KubernetesHttpClient client, String deploymentName) {
        this.client = client;
        this.deploymentName = deploymentName;
    }

    public ScaleDeploymentCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ScaleDeploymentCmd withReplicas(int replicas) {
        this.replicas = replicas;
        return this;
    }

    public Deployment exec() {
        try {
            String path = "/apis/apps/v1/namespaces/" + namespace + "/deployments/" + deploymentName;
            String patchBody = "{\"spec\":{\"replicas\":" + replicas + "}}";
            KubernetesResponse response = client.patch(path, patchBody);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Deployment not found: " + deploymentName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to scale deployment: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Deployment.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to scale deployment", e);
        }
    }
}
