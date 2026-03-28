package io.github.intisy.kubernetes.command.networkpolicy;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.NetworkPolicy;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list network policies.
 *
 * @author Finn Birich
 */
public class ListNetworkPoliciesCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;

    public ListNetworkPoliciesCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListNetworkPoliciesCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListNetworkPoliciesCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListNetworkPoliciesCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public List<NetworkPolicy> exec() {
        try {
            Map<String, String> queryParams = new HashMap<String, String>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            String path = namespace != null
                    ? "/apis/networking.k8s.io/v1/namespaces/" + namespace + "/networkpolicies"
                    : "/apis/networking.k8s.io/v1/networkpolicies";
            KubernetesResponse response = client.get(path, queryParams);
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list network policies: " + response.getBody(), response.getStatusCode());
            }
            Type listType = new TypeToken<KubernetesList<NetworkPolicy>>() {}.getType();
            KubernetesList<NetworkPolicy> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list network policies", e);
        }
    }
}
