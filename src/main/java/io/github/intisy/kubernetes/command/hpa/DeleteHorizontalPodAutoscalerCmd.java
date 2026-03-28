package io.github.intisy.kubernetes.command.hpa;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to delete a horizontal pod autoscaler.
 *
 * @author Finn Birich
 */
public class DeleteHorizontalPodAutoscalerCmd {
    private final KubernetesHttpClient client;
    private final String hpaName;
    private String namespace = "default";
    private Integer gracePeriodSeconds;

    public DeleteHorizontalPodAutoscalerCmd(KubernetesHttpClient client, String hpaName) {
        this.client = client;
        this.hpaName = hpaName;
    }

    public DeleteHorizontalPodAutoscalerCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DeleteHorizontalPodAutoscalerCmd withGracePeriod(int seconds) {
        this.gracePeriodSeconds = seconds;
        return this;
    }

    public void exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (gracePeriodSeconds != null) {
                queryParams.put("gracePeriodSeconds", String.valueOf(gracePeriodSeconds));
            }

            String path = "/apis/autoscaling/v1/namespaces/" + namespace + "/horizontalpodautoscalers/" + hpaName;
            KubernetesResponse response = client.delete(path, queryParams);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("HorizontalPodAutoscaler not found: " + hpaName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete horizontal pod autoscaler: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete horizontal pod autoscaler", e);
        }
    }
}
