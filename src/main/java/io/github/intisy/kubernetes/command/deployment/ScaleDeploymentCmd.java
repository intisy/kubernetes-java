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
        String deployPath = "/apis/apps/v1/namespaces/" + namespace + "/deployments/" + deploymentName;
        int maxRetries = 5;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                KubernetesResponse getResponse = client.get(deployPath);
                if (getResponse.getStatusCode() == 404) {
                    throw new NotFoundException("Deployment not found: " + deploymentName);
                }
                if (!getResponse.isSuccessful()) {
                    throw new KubernetesException("Failed to get deployment: " + getResponse.getBody(), getResponse.getStatusCode());
                }

                com.google.gson.JsonObject deployment = client.getGson().fromJson(getResponse.getBody(), com.google.gson.JsonObject.class);
                deployment.getAsJsonObject("spec").addProperty("replicas", replicas);

                KubernetesResponse putResponse = client.put(deployPath, deployment);

                if (putResponse.getStatusCode() == 409 && attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(100L * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new KubernetesException("Interrupted while retrying scale", ie);
                    }
                    continue;
                }

                if (!putResponse.isSuccessful()) {
                    throw new KubernetesException("Failed to scale deployment: " + putResponse.getBody(), putResponse.getStatusCode());
                }

                return client.getGson().fromJson(putResponse.getBody(), Deployment.class);
            } catch (IOException e) {
                throw new KubernetesException("Failed to scale deployment", e);
            }
        }

        throw new KubernetesException("Failed to scale deployment after " + maxRetries + " retries due to conflicts", 409);
    }
}
