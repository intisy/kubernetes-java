package io.github.intisy.kubernetes.command.statefulset;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.StatefulSet;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to scale a stateful set.
 *
 * @author Finn Birich
 */
public class ScaleStatefulSetCmd {
    private final KubernetesHttpClient client;
    private final String statefulSetName;
    private String namespace = "default";
    private int replicas;

    public ScaleStatefulSetCmd(KubernetesHttpClient client, String statefulSetName) {
        this.client = client;
        this.statefulSetName = statefulSetName;
    }

    public ScaleStatefulSetCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ScaleStatefulSetCmd withReplicas(int replicas) {
        this.replicas = replicas;
        return this;
    }

    public StatefulSet exec() {
        try {
            String path = "/apis/apps/v1/namespaces/" + namespace + "/statefulsets/" + statefulSetName;
            String patchBody = "{\"spec\":{\"replicas\":" + replicas + "}}";
            KubernetesResponse response = client.patch(path, patchBody);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("StatefulSet not found: " + statefulSetName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to scale stateful set: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), StatefulSet.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to scale stateful set", e);
        }
    }
}
