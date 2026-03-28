package io.github.intisy.kubernetes.command.replicaset;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.ReplicaSet;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list replica sets.
 *
 * @author Finn Birich
 */
public class ListReplicaSetsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;

    public ListReplicaSetsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListReplicaSetsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListReplicaSetsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListReplicaSetsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public List<ReplicaSet> exec() {
        try {
            Map<String, String> queryParams = new HashMap<String, String>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            String path = namespace != null
                    ? "/apis/apps/v1/namespaces/" + namespace + "/replicasets"
                    : "/apis/apps/v1/replicasets";
            KubernetesResponse response = client.get(path, queryParams);
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list replica sets: " + response.getBody(), response.getStatusCode());
            }
            Type listType = new TypeToken<KubernetesList<ReplicaSet>>() {}.getType();
            KubernetesList<ReplicaSet> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list replica sets", e);
        }
    }
}
