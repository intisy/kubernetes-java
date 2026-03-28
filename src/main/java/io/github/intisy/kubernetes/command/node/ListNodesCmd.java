package io.github.intisy.kubernetes.command.node;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.Node;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Command to list nodes.
 *
 * @author Finn Birich
 */
public class ListNodesCmd {
    private final KubernetesHttpClient client;

    public ListNodesCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public List<Node> exec() {
        try {
            KubernetesResponse response = client.get("/api/v1/nodes");

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list nodes: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<Node>>() {}.getType();
            KubernetesList<Node> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list nodes", e);
        }
    }
}
