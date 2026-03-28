package io.github.intisy.kubernetes.command.deployment;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.Deployment;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a deployment.
 *
 * @author Finn Birich
 */
public class GetDeploymentCmd {
    private final KubernetesHttpClient client;
    private final String deploymentName;
    private String namespace = "default";

    public GetDeploymentCmd(KubernetesHttpClient client, String deploymentName) {
        this.client = client;
        this.deploymentName = deploymentName;
    }

    public GetDeploymentCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Deployment exec() {
        try {
            String path = "/apis/apps/v1/namespaces/" + namespace + "/deployments/" + deploymentName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Deployment not found: " + deploymentName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get deployment: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Deployment.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get deployment", e);
        }
    }
}
