package io.github.intisy.kubernetes.command.hpa;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.HorizontalPodAutoscaler;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a horizontal pod autoscaler.
 *
 * @author Finn Birich
 */
public class GetHorizontalPodAutoscalerCmd {
    private final KubernetesHttpClient client;
    private final String hpaName;
    private String namespace = "default";

    public GetHorizontalPodAutoscalerCmd(KubernetesHttpClient client, String hpaName) {
        this.client = client;
        this.hpaName = hpaName;
    }

    public GetHorizontalPodAutoscalerCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public HorizontalPodAutoscaler exec() {
        try {
            String path = "/apis/autoscaling/v1/namespaces/" + namespace + "/horizontalpodautoscalers/" + hpaName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("HorizontalPodAutoscaler not found: " + hpaName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get horizontal pod autoscaler: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), HorizontalPodAutoscaler.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get horizontal pod autoscaler", e);
        }
    }
}
