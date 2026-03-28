package io.github.intisy.kubernetes.command.pv;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to delete a persistent volume.
 *
 * @author Finn Birich
 */
public class DeletePersistentVolumeCmd {
    private final KubernetesHttpClient client;
    private final String pvName;
    private Integer gracePeriodSeconds;

    public DeletePersistentVolumeCmd(KubernetesHttpClient client, String pvName) {
        this.client = client;
        this.pvName = pvName;
    }

    public DeletePersistentVolumeCmd withGracePeriod(int seconds) {
        this.gracePeriodSeconds = seconds;
        return this;
    }

    public void exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (gracePeriodSeconds != null) {
                queryParams.put("gracePeriodSeconds", String.valueOf(gracePeriodSeconds));
            }

            String path = "/api/v1/persistentvolumes/" + pvName;
            KubernetesResponse response = client.delete(path, queryParams);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("PersistentVolume not found: " + pvName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete persistent volume: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete persistent volume", e);
        }
    }
}
