package io.github.intisy.kubernetes.command.cronjob;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.CronJob;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a cron job.
 *
 * @author Finn Birich
 */
public class GetCronJobCmd {
    private final KubernetesHttpClient client;
    private final String cronJobName;
    private String namespace = "default";

    public GetCronJobCmd(KubernetesHttpClient client, String cronJobName) {
        this.client = client;
        this.cronJobName = cronJobName;
    }

    public GetCronJobCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public CronJob exec() {
        try {
            String path = "/apis/batch/v1/namespaces/" + namespace + "/cronjobs/" + cronJobName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("CronJob not found: " + cronJobName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get cron job: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), CronJob.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get cron job", e);
        }
    }
}
