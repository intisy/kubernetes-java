package io.github.intisy.kubernetes.command.pdb;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to delete a pod disruption budget.
 *
 * @author Finn Birich
 */
public class DeletePodDisruptionBudgetCmd {
    private final KubernetesHttpClient client;
    private final String pdbName;
    private String namespace = "default";
    private Integer gracePeriodSeconds;

    public DeletePodDisruptionBudgetCmd(KubernetesHttpClient client, String pdbName) {
        this.client = client;
        this.pdbName = pdbName;
    }

    public DeletePodDisruptionBudgetCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DeletePodDisruptionBudgetCmd withGracePeriod(int seconds) {
        this.gracePeriodSeconds = seconds;
        return this;
    }

    public void exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (gracePeriodSeconds != null) {
                queryParams.put("gracePeriodSeconds", String.valueOf(gracePeriodSeconds));
            }

            String path = "/apis/policy/v1/namespaces/" + namespace + "/poddisruptionbudgets/" + pdbName;
            KubernetesResponse response = client.delete(path, queryParams);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("PodDisruptionBudget not found: " + pdbName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete pod disruption budget: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete pod disruption budget", e);
        }
    }
}
