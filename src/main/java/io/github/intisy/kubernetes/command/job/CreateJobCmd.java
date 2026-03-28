package io.github.intisy.kubernetes.command.job;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.Job;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a job.
 *
 * @author Finn Birich
 */
public class CreateJobCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private Job job;

    public CreateJobCmd(KubernetesHttpClient client, Job job) {
        this.client = client;
        this.job = job;
    }

    public CreateJobCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Job exec() {
        try {
            String ns = namespace;
            if (job.getMetadata() != null && job.getMetadata().getNamespace() != null) {
                ns = job.getMetadata().getNamespace();
            }

            String path = "/apis/batch/v1/namespaces/" + ns + "/jobs";
            KubernetesResponse response = client.post(path, job);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("Job already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create job: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Job.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create job", e);
        }
    }
}
