package io.github.intisy.kubernetes.command.pv;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.PersistentVolume;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a persistent volume.
 *
 * @author Finn Birich
 */
public class CreatePersistentVolumeCmd {
    private final KubernetesHttpClient client;
    private PersistentVolume pv;

    public CreatePersistentVolumeCmd(KubernetesHttpClient client, PersistentVolume pv) {
        this.client = client;
        this.pv = pv;
    }

    public PersistentVolume exec() {
        try {
            String path = "/api/v1/persistentvolumes";
            KubernetesResponse response = client.post(path, pv);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("PersistentVolume already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create persistent volume: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), PersistentVolume.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create persistent volume", e);
        }
    }
}
