package io.github.intisy.kubernetes.command.ingress;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.Ingress;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list ingresses.
 *
 * @author Finn Birich
 */
public class ListIngressesCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;

    public ListIngressesCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListIngressesCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListIngressesCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListIngressesCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public List<Ingress> exec() {
        try {
            Map<String, String> queryParams = new HashMap<String, String>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            String path = namespace != null
                    ? "/apis/networking.k8s.io/v1/namespaces/" + namespace + "/ingresses"
                    : "/apis/networking.k8s.io/v1/ingresses";
            KubernetesResponse response = client.get(path, queryParams);
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list ingresses: " + response.getBody(), response.getStatusCode());
            }
            Type listType = new TypeToken<KubernetesList<Ingress>>() {}.getType();
            KubernetesList<Ingress> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list ingresses", e);
        }
    }
}
