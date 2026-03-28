package io.github.intisy.kubernetes.command.pdb;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.PodDisruptionBudget;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a pod disruption budget.
 *
 * @author Finn Birich
 */
public class CreatePodDisruptionBudgetCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private PodDisruptionBudget pdb;

    public CreatePodDisruptionBudgetCmd(KubernetesHttpClient client, PodDisruptionBudget pdb) {
        this.client = client;
        this.pdb = pdb;
    }

    public CreatePodDisruptionBudgetCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public PodDisruptionBudget exec() {
        try {
            String ns = namespace;
            if (pdb.getMetadata() != null && pdb.getMetadata().getNamespace() != null) {
                ns = pdb.getMetadata().getNamespace();
            }
            String path = "/apis/policy/v1/namespaces/" + ns + "/poddisruptionbudgets";
            KubernetesResponse response = client.post(path, pdb);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("PodDisruptionBudget already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create pod disruption budget: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), PodDisruptionBudget.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create pod disruption budget", e);
        }
    }
}
