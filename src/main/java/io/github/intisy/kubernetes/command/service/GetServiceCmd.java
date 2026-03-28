package io.github.intisy.kubernetes.command.service;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.Service;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) a service.
 *
 * @author Finn Birich
 */
public class GetServiceCmd {
    private final KubernetesHttpClient client;
    private final String serviceName;
    private String namespace = "default";

    public GetServiceCmd(KubernetesHttpClient client, String serviceName) {
        this.client = client;
        this.serviceName = serviceName;
    }

    public GetServiceCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Service exec() {
        try {
            String path = "/api/v1/namespaces/" + namespace + "/services/" + serviceName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Service not found: " + serviceName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get service: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Service.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get service", e);
        }
    }
}
