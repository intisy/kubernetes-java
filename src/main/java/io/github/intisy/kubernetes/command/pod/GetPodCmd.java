package io.github.intisy.kubernetes.command.pod;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.Pod;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a pod.
 *
 * @author Finn Birich
 */
public class GetPodCmd {
    private final KubernetesHttpClient client;
    private final String podName;
    private String namespace = "default";

    public GetPodCmd(KubernetesHttpClient client, String podName) {
        this.client = client;
        this.podName = podName;
    }

    public GetPodCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Pod exec() {
        try {
            String path = "/api/v1/namespaces/" + namespace + "/pods/" + podName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Pod not found: " + podName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get pod: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Pod.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get pod", e);
        }
    }
}
