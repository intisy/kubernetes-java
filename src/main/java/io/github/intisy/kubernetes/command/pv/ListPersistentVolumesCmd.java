package io.github.intisy.kubernetes.command.pv;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.PersistentVolume;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list persistent volumes.
 *
 * @author Finn Birich
 */
public class ListPersistentVolumesCmd {
    private final KubernetesHttpClient client;
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListPersistentVolumesCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListPersistentVolumesCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListPersistentVolumesCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListPersistentVolumesCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<PersistentVolume> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = "/api/v1/persistentvolumes";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list persistent volumes: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<PersistentVolume>>() {}.getType();
            KubernetesList<PersistentVolume> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list persistent volumes", e);
        }
    }
}
