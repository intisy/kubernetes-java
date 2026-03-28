package io.github.intisy.kubernetes.command.node;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.Node;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a node.
 *
 * @author Finn Birich
 */
public class GetNodeCmd {
    private final KubernetesHttpClient client;
    private final String nodeName;

    public GetNodeCmd(KubernetesHttpClient client, String nodeName) {
        this.client = client;
        this.nodeName = nodeName;
    }

    public Node exec() {
        try {
            KubernetesResponse response = client.get("/api/v1/nodes/" + nodeName);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Node not found: " + nodeName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get node: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Node.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get node", e);
        }
    }
}
