package io.github.intisy.kubernetes.command.pod;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.Pod;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to delete a pod.
 *
 * @author Finn Birich
 */
public class DeletePodCmd {
    private final KubernetesHttpClient client;
    private final String podName;
    private String namespace = "default";
    private Integer gracePeriodSeconds;

    public DeletePodCmd(KubernetesHttpClient client, String podName) {
        this.client = client;
        this.podName = podName;
    }

    public DeletePodCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DeletePodCmd withGracePeriod(int seconds) {
        this.gracePeriodSeconds = seconds;
        return this;
    }

    public DeletePodCmd withForce(boolean force) {
        if (force) {
            this.gracePeriodSeconds = 0;
        }
        return this;
    }

    public void exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (gracePeriodSeconds != null) {
                queryParams.put("gracePeriodSeconds", String.valueOf(gracePeriodSeconds));
            }

            String path = "/api/v1/namespaces/" + namespace + "/pods/" + podName;
            KubernetesResponse response = client.delete(path, queryParams);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Pod not found: " + podName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete pod: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete pod", e);
        }
    }
}
