package io.github.intisy.kubernetes.command.pv;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.PersistentVolume;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a persistent volume.
 *
 * @author Finn Birich
 */
public class GetPersistentVolumeCmd {
    private final KubernetesHttpClient client;
    private final String pvName;

    public GetPersistentVolumeCmd(KubernetesHttpClient client, String pvName) {
        this.client = client;
        this.pvName = pvName;
    }

    public PersistentVolume exec() {
        try {
            String path = "/api/v1/persistentvolumes/" + pvName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("PersistentVolume not found: " + pvName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get persistent volume: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), PersistentVolume.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get persistent volume", e);
        }
    }
}
