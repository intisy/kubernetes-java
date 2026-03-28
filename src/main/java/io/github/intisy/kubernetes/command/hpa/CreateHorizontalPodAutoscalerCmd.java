package io.github.intisy.kubernetes.command.hpa;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.HorizontalPodAutoscaler;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a horizontal pod autoscaler.
 *
 * @author Finn Birich
 */
public class CreateHorizontalPodAutoscalerCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private HorizontalPodAutoscaler hpa;

    public CreateHorizontalPodAutoscalerCmd(KubernetesHttpClient client, HorizontalPodAutoscaler hpa) {
        this.client = client;
        this.hpa = hpa;
    }

    public CreateHorizontalPodAutoscalerCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public HorizontalPodAutoscaler exec() {
        try {
            String ns = namespace;
            if (hpa.getMetadata() != null && hpa.getMetadata().getNamespace() != null) {
                ns = hpa.getMetadata().getNamespace();
            }
            String path = "/apis/autoscaling/v1/namespaces/" + ns + "/horizontalpodautoscalers";
            KubernetesResponse response = client.post(path, hpa);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("HorizontalPodAutoscaler already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create horizontal pod autoscaler: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), HorizontalPodAutoscaler.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create horizontal pod autoscaler", e);
        }
    }
}
