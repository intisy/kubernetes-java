package io.github.intisy.kubernetes.command.statefulset;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.StatefulSet;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a stateful set.
 *
 * @author Finn Birich
 */
public class CreateStatefulSetCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private StatefulSet statefulSet;

    public CreateStatefulSetCmd(KubernetesHttpClient client, StatefulSet statefulSet) {
        this.client = client;
        this.statefulSet = statefulSet;
    }

    public CreateStatefulSetCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public StatefulSet exec() {
        try {
            String ns = namespace;
            if (statefulSet.getMetadata() != null && statefulSet.getMetadata().getNamespace() != null) {
                ns = statefulSet.getMetadata().getNamespace();
            }

            String path = "/apis/apps/v1/namespaces/" + ns + "/statefulsets";
            KubernetesResponse response = client.post(path, statefulSet);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("StatefulSet already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create stateful set: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), StatefulSet.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create stateful set", e);
        }
    }
}
