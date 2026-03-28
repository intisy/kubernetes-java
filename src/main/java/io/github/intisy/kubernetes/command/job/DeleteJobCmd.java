package io.github.intisy.kubernetes.command.job;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to delete a job.
 *
 * @author Finn Birich
 */
public class DeleteJobCmd {
    private final KubernetesHttpClient client;
    private final String jobName;
    private String namespace = "default";

    public DeleteJobCmd(KubernetesHttpClient client, String jobName) {
        this.client = client;
        this.jobName = jobName;
    }

    public DeleteJobCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public void exec() {
        try {
            String path = "/apis/batch/v1/namespaces/" + namespace + "/jobs/" + jobName;
            KubernetesResponse response = client.delete(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Job not found: " + jobName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete job: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete job", e);
        }
    }
}
