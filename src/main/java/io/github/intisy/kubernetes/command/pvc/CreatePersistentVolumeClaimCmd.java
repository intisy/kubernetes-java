package io.github.intisy.kubernetes.command.pvc;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.PersistentVolumeClaim;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a persistent volume claim.
 *
 * @author Finn Birich
 */
public class CreatePersistentVolumeClaimCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private PersistentVolumeClaim pvc;

    public CreatePersistentVolumeClaimCmd(KubernetesHttpClient client, PersistentVolumeClaim pvc) {
        this.client = client;
        this.pvc = pvc;
    }

    public CreatePersistentVolumeClaimCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public PersistentVolumeClaim exec() {
        try {
            String ns = namespace;
            if (pvc.getMetadata() != null && pvc.getMetadata().getNamespace() != null) {
                ns = pvc.getMetadata().getNamespace();
            }
            String path = "/api/v1/namespaces/" + ns + "/persistentvolumeclaims";
            KubernetesResponse response = client.post(path, pvc);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("PersistentVolumeClaim already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create persistent volume claim: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), PersistentVolumeClaim.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create persistent volume claim", e);
        }
    }
}
