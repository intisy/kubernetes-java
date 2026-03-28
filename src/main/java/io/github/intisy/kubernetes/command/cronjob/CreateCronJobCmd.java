package io.github.intisy.kubernetes.command.cronjob;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.CronJob;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a cron job.
 *
 * @author Finn Birich
 */
public class CreateCronJobCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private CronJob cronJob;

    public CreateCronJobCmd(KubernetesHttpClient client, CronJob cronJob) {
        this.client = client;
        this.cronJob = cronJob;
    }

    public CreateCronJobCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public CronJob exec() {
        try {
            String ns = namespace;
            if (cronJob.getMetadata() != null && cronJob.getMetadata().getNamespace() != null) {
                ns = cronJob.getMetadata().getNamespace();
            }

            String path = "/apis/batch/v1/namespaces/" + ns + "/cronjobs";
            KubernetesResponse response = client.post(path, cronJob);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("CronJob already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create cron job: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), CronJob.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create cron job", e);
        }
    }
}
