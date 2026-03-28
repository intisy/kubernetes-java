package io.github.intisy.kubernetes.command.storageclass;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.StorageClass;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a storage class.
 *
 * @author Finn Birich
 */
public class GetStorageClassCmd {
    private final KubernetesHttpClient client;
    private final String storageClassName;

    public GetStorageClassCmd(KubernetesHttpClient client, String storageClassName) {
        this.client = client;
        this.storageClassName = storageClassName;
    }

    public StorageClass exec() {
        try {
            String path = "/apis/storage.k8s.io/v1/storageclasses/" + storageClassName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("StorageClass not found: " + storageClassName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get storage class: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), StorageClass.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get storage class", e);
        }
    }
}
