package io.github.intisy.kubernetes.command.daemonset;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.DaemonSet;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list daemon sets.
 *
 * @author Finn Birich
 */
public class ListDaemonSetsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;

    public ListDaemonSetsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListDaemonSetsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListDaemonSetsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListDaemonSetsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public List<DaemonSet> exec() {
        try {
            Map<String, String> queryParams = new HashMap<String, String>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            String path = namespace != null
                    ? "/apis/apps/v1/namespaces/" + namespace + "/daemonsets"
                    : "/apis/apps/v1/daemonsets";
            KubernetesResponse response = client.get(path, queryParams);
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list daemon sets: " + response.getBody(), response.getStatusCode());
            }
            Type listType = new TypeToken<KubernetesList<DaemonSet>>() {}.getType();
            KubernetesList<DaemonSet> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list daemon sets", e);
        }
    }
}
