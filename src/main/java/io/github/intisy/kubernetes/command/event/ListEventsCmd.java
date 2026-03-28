package io.github.intisy.kubernetes.command.event;

import com.google.gson.reflect.TypeToken;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.Event;
import io.github.intisy.kubernetes.model.KubernetesList;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to list events.
 *
 * @author Finn Birich
 */
public class ListEventsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;
    private String fieldSelector;
    private Integer limit;

    public ListEventsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public ListEventsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ListEventsCmd withAllNamespaces() {
        this.namespace = null;
        return this;
    }

    public ListEventsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public ListEventsCmd withFieldSelector(String fieldSelector) {
        this.fieldSelector = fieldSelector;
        return this;
    }

    public ListEventsCmd withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<Event> exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (labelSelector != null) queryParams.put("labelSelector", labelSelector);
            if (fieldSelector != null) queryParams.put("fieldSelector", fieldSelector);
            if (limit != null) queryParams.put("limit", String.valueOf(limit));

            String path = namespace != null
                    ? "/api/v1/namespaces/" + namespace + "/events"
                    : "/api/v1/events";

            KubernetesResponse response = client.get(path, queryParams);

            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to list events: " + response.getBody(), response.getStatusCode());
            }

            Type listType = new TypeToken<KubernetesList<Event>>() {}.getType();
            KubernetesList<Event> list = client.getGson().fromJson(response.getBody(), listType);
            return list.getItems();
        } catch (IOException e) {
            throw new KubernetesException("Failed to list events", e);
        }
    }
}
