package io.github.intisy.kubernetes.command.pvc;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.PersistentVolumeClaim;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a persistent volume claim.
 *
 * @author Finn Birich
 */
public class GetPersistentVolumeClaimCmd {
    private final KubernetesHttpClient client;
    private final String pvcName;
    private String namespace = "default";

    public GetPersistentVolumeClaimCmd(KubernetesHttpClient client, String pvcName) {
        this.client = client;
        this.pvcName = pvcName;
    }

    public GetPersistentVolumeClaimCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public PersistentVolumeClaim exec() {
        try {
            String path = "/api/v1/namespaces/" + namespace + "/persistentvolumeclaims/" + pvcName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("PersistentVolumeClaim not found: " + pvcName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get persistent volume claim: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), PersistentVolumeClaim.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get persistent volume claim", e);
        }
    }
}
