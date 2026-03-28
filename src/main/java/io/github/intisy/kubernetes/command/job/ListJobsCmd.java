package io.github.intisy.kubernetes.command.job;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.Job;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list jobs.
 *
 * @author Finn Birich
 */
public class ListJobsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;

    public ListJobsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListJobsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListJobsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListJobsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public List<Job> exec() {
        try {
            Map<String, String> queryParams = new HashMap<String, String>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);

            String path = namespace != null
                    ? "/apis/batch/v1/namespaces/" + namespace + "/jobs"
                    : "/apis/batch/v1/jobs";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list jobs: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<Job>>() {}.getType();
            KubernetesList<Job> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list jobs", e);
        }
    }
}
