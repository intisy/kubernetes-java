package io.github.intisy.kubernetes.command.replicaset;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.ReplicaSet;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a replica set.
 *
 * @author Finn Birich
 */
public class GetReplicaSetCmd {
    private final KubernetesHttpClient client;
    private final String replicaSetName;
    private String namespace = "default";

    public GetReplicaSetCmd(KubernetesHttpClient client, String replicaSetName) {
        this.client = client;
        this.replicaSetName = replicaSetName;
    }

    public GetReplicaSetCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ReplicaSet exec() {
        try {
            String path = "/apis/apps/v1/namespaces/" + namespace + "/replicasets/" + replicaSetName;
            KubernetesResponse response = client.get(path);
            if (response.getStatusCode() == 404) {
                throw new NotFoundException("ReplicaSet not found: " + replicaSetName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get replica set: " + response.getBody(), response.getStatusCode());
            }
            return client.getGson().fromJson(response.getBody(), ReplicaSet.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get replica set", e);
        }
    }
}
