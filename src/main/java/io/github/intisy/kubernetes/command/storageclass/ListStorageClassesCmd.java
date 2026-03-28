package io.github.intisy.kubernetes.command.storageclass;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.model.StorageClass;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list storage classes.
 *
 * @author Finn Birich
 */
public class ListStorageClassesCmd {
    private final KubernetesHttpClient client;
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListStorageClassesCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListStorageClassesCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListStorageClassesCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListStorageClassesCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<StorageClass> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = "/apis/storage.k8s.io/v1/storageclasses";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list storage classes: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<StorageClass>>() {}.getType();
            KubernetesList<StorageClass> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list storage classes", e);
        }
    }
}
