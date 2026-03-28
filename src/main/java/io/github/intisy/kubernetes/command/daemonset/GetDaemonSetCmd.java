package io.github.intisy.kubernetes.command.daemonset;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.DaemonSet;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a daemon set.
 *
 * @author Finn Birich
 */
public class GetDaemonSetCmd {
    private final KubernetesHttpClient client;
    private final String daemonSetName;
    private String namespace = "default";

    public GetDaemonSetCmd(KubernetesHttpClient client, String daemonSetName) {
        this.client = client;
        this.daemonSetName = daemonSetName;
    }

    public GetDaemonSetCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DaemonSet exec() {
        try {
            String path = "/apis/apps/v1/namespaces/" + namespace + "/daemonsets/" + daemonSetName;
            KubernetesResponse response = client.get(path);
            if (response.getStatusCode() == 404) {
                throw new NotFoundException("DaemonSet not found: " + daemonSetName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get daemon set: " + response.getBody(), response.getStatusCode());
            }
            return client.getGson().fromJson(response.getBody(), DaemonSet.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get daemon set", e);
        }
    }
}
