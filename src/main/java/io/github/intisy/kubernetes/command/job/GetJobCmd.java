package io.github.intisy.kubernetes.command.job;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.Job;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a job.
 *
 * @author Finn Birich
 */
public class GetJobCmd {
    private final KubernetesHttpClient client;
    private final String jobName;
    private String namespace = "default";

    public GetJobCmd(KubernetesHttpClient client, String jobName) {
        this.client = client;
        this.jobName = jobName;
    }

    public GetJobCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Job exec() {
        try {
            String path = "/apis/batch/v1/namespaces/" + namespace + "/jobs/" + jobName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Job not found: " + jobName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get job: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Job.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get job", e);
        }
    }
}
