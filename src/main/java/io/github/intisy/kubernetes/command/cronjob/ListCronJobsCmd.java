package io.github.intisy.kubernetes.command.cronjob;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.CronJob;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list cron jobs.
 *
 * @author Finn Birich
 */
public class ListCronJobsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;

    public ListCronJobsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListCronJobsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListCronJobsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListCronJobsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public List<CronJob> exec() {
        try {
            Map<String, String> queryParams = new HashMap<String, String>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);

            String path = namespace != null
                    ? "/apis/batch/v1/namespaces/" + namespace + "/cronjobs"
                    : "/apis/batch/v1/cronjobs";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list cron jobs: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<CronJob>>() {}.getType();
            KubernetesList<CronJob> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list cron jobs", e);
        }
    }
}
