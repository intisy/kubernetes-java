package io.github.intisy.kubernetes.command.pvc;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.PersistentVolumeClaim;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list persistent volume claims.
 *
 * @author Finn Birich
 */
public class ListPersistentVolumeClaimsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListPersistentVolumeClaimsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListPersistentVolumeClaimsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListPersistentVolumeClaimsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListPersistentVolumeClaimsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListPersistentVolumeClaimsCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListPersistentVolumeClaimsCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<PersistentVolumeClaim> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = namespace != null
                    ? "/api/v1/namespaces/" + namespace + "/persistentvolumeclaims"
                    : "/api/v1/persistentvolumeclaims";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list persistent volume claims: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<PersistentVolumeClaim>>() {}.getType();
            KubernetesList<PersistentVolumeClaim> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list persistent volume claims", e);
        }
    }
}
