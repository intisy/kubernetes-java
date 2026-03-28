package io.github.intisy.kubernetes.command.deployment;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.Deployment;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list deployments.
 *
 * @author Finn Birich
 */
public class ListDeploymentsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;

    public ListDeploymentsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListDeploymentsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListDeploymentsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListDeploymentsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public List<Deployment> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);

            String path = namespace != null
                    ? "/apis/apps/v1/namespaces/" + namespace + "/deployments"
                    : "/apis/apps/v1/deployments";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list deployments: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<Deployment>>() {}.getType();
            KubernetesList<Deployment> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list deployments", e);
        }
    }
}
