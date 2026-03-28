package io.github.intisy.kubernetes.command.namespace;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.Namespace;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Command to list namespaces.
 *
 * @author Finn Birich
 */
public class ListNamespacesCmd {
    private final KubernetesHttpClient client;

    public ListNamespacesCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public List<Namespace> exec() {
        try {
            KubernetesResponse response = client.get("/api/v1/namespaces");

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list namespaces: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<Namespace>>() {}.getType();
            KubernetesList<Namespace> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list namespaces", e);
        }
    }
}
