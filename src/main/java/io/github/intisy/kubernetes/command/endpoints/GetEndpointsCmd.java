package io.github.intisy.kubernetes.command.endpoints;

import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.Endpoints;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to get (inspect) endpoints.
 *
 * @author Finn Birich
 */
public class GetEndpointsCmd {
    private final KubernetesHttpClient client;
    private final String endpointsName;
    private String namespace = "default";

    public GetEndpointsCmd(KubernetesHttpClient client, String endpointsName) {
        this.client = client;
        this.endpointsName = endpointsName;
    }

    public GetEndpointsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Endpoints exec() {
        try {
            String path = "/api/v1/namespaces/" + namespace + "/endpoints/" + endpointsName;
            KubernetesResponse response = client.get(path);

            if (response.getStatusCode() == 404) {
                throw new NotFoundException("Endpoints not found: " + endpointsName);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to get endpoints: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Endpoints.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to get endpoints", e);
        }
    }
}
