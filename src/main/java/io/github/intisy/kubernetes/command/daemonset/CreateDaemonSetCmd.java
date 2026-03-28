package io.github.intisy.kubernetes.command.daemonset;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.DaemonSet;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a daemon set.
 *
 * @author Finn Birich
 */
public class CreateDaemonSetCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private DaemonSet daemonSet;

    public CreateDaemonSetCmd(KubernetesHttpClient client, DaemonSet daemonSet) {
        this.client = client;
        this.daemonSet = daemonSet;
    }

    public CreateDaemonSetCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DaemonSet exec() {
        try {
            String ns = namespace;
            if (daemonSet.getMetadata() != null && daemonSet.getMetadata().getNamespace() != null) {
                ns = daemonSet.getMetadata().getNamespace();
            }
            String path = "/apis/apps/v1/namespaces/" + ns + "/daemonsets";
            KubernetesResponse response = client.post(path, daemonSet);
            if (response.getStatusCode() == 409) {
                throw new ConflictException("DaemonSet already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create daemon set: " + response.getBody(), response.getStatusCode());
            }
            return client.getGson().fromJson(response.getBody(), DaemonSet.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create daemon set", e);
        }
    }
}
