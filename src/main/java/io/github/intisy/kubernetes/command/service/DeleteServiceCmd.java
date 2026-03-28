package io.github.intisy.kubernetes.command.service;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to delete a service.
 *
 * @author Finn Birich
 */
public class DeleteServiceCmd {
    private final KubernetesHttpClient client;
    private final String serviceName;
    private String namespace = "default";

    public DeleteServiceCmd(KubernetesHttpClient client, String serviceName) {
        this.client = client;
        this.serviceName = serviceName;
    }

    public DeleteServiceCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public void exec() {
        try {
            String path = "/api/v1/namespaces/" + namespace + "/services/" + serviceName;
            KubernetesResponse response = client.delete(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Service not found: " + serviceName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to delete service: " + response.getBody(), response.getStatusCode());
            }
        } catch (IOException e) {
            throw new KubernetesException("Failed to delete service", e);
        }
    }
}
