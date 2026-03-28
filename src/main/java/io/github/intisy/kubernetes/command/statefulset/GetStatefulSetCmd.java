package io.github.intisy.kubernetes.command.statefulset;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.StatefulSet;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a stateful set.
 *
 * @author Finn Birich
 */
public class GetStatefulSetCmd {
    private final KubernetesHttpClient client;
    private final String statefulSetName;
    private String namespace = "default";

    public GetStatefulSetCmd(KubernetesHttpClient client, String statefulSetName) {
        this.client = client;
        this.statefulSetName = statefulSetName;
    }

    public GetStatefulSetCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public StatefulSet exec() {
        try {
            String path = "/apis/apps/v1/namespaces/" + namespace + "/statefulsets/" + statefulSetName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("StatefulSet not found: " + statefulSetName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get stateful set: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), StatefulSet.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get stateful set", e);
        }
    }
}
