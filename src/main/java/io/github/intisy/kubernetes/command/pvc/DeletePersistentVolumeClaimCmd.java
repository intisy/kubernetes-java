package io.github.intisy.kubernetes.command.pvc;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to delete a persistent volume claim.
 *
 * @author Finn Birich
 */
public class DeletePersistentVolumeClaimCmd {
    private final KubernetesHttpClient client;
    private final String pvcName;
    private String namespace = "default";
    private Integer gracePeriodSeconds;

    public DeletePersistentVolumeClaimCmd(KubernetesHttpClient client, String pvcName) {
        this.client = client;
        this.pvcName = pvcName;
    }

    public DeletePersistentVolumeClaimCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DeletePersistentVolumeClaimCmd withGracePeriod(int seconds) {
        this.gracePeriodSeconds = seconds;
        return this;
    }

    public void exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (gracePeriodSeconds != null) {
                queryParams.put("gracePeriodSeconds", String.valueOf(gracePeriodSeconds));
            }

            String path = "/api/v1/namespaces/" + namespace + "/persistentvolumeclaims/" + pvcName;
            KubernetesResponse response = client.delete(path, queryParams);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("PersistentVolumeClaim not found: " + pvcName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete persistent volume claim: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete persistent volume claim", e);
        }
    }
}
