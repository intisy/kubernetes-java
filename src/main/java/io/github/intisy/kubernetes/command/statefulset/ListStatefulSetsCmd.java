package io.github.intisy.kubernetes.command.statefulset;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.StatefulSet;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list stateful sets.
 *
 * @author Finn Birich
 */
public class ListStatefulSetsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;

    public ListStatefulSetsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListStatefulSetsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListStatefulSetsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListStatefulSetsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public List<StatefulSet> exec() {
        try {
            Map<String, String> queryParams = new HashMap<String, String>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);

            String path = namespace != null
                    ? "/apis/apps/v1/namespaces/" + namespace + "/statefulsets"
                    : "/apis/apps/v1/statefulsets";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list stateful sets: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<StatefulSet>>() {}.getType();
            KubernetesList<StatefulSet> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list stateful sets", e);
        }
    }
}
