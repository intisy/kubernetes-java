package io.github.intisy.kubernetes.command.cronjob;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to delete a cron job.
 *
 * @author Finn Birich
 */
public class DeleteCronJobCmd {
    private final KubernetesHttpClient client;
    private final String cronJobName;
    private String namespace = "default";

    public DeleteCronJobCmd(KubernetesHttpClient client, String cronJobName) {
        this.client = client;
        this.cronJobName = cronJobName;
    }

    public DeleteCronJobCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public void exec() {
        try {
            String path = "/apis/batch/v1/namespaces/" + namespace + "/cronjobs/" + cronJobName;
            KubernetesResponse response = client.delete(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("CronJob not found: " + cronJobName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete cron job: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete cron job", e);
        }
    }
}
