package io.github.intisy.kubernetes.command.limitrange;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to delete a limit range.
 *
 * @author Finn Birich
 */
public class DeleteLimitRangeCmd {
    private final KubernetesHttpClient client;
    private final String limitRangeName;
    private String namespace = "default";
    private Integer gracePeriodSeconds;

    public DeleteLimitRangeCmd(KubernetesHttpClient client, String limitRangeName) {
        this.client = client;
        this.limitRangeName = limitRangeName;
    }

    public DeleteLimitRangeCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DeleteLimitRangeCmd withGracePeriod(int seconds) {
        this.gracePeriodSeconds = seconds;
        return this;
    }

    public void exec() {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (gracePeriodSeconds != null) {
                queryParams.put("gracePeriodSeconds", String.valueOf(gracePeriodSeconds));
            }

            String path = "/api/v1/namespaces/" + namespace + "/limitranges/" + limitRangeName;
            KubernetesResponse response = client.delete(path, queryParams);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("LimitRange not found: " + limitRangeName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete limit range: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete limit range", e);
        }
    }
}
