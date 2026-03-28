package io.github.intisy.kubernetes.command.service;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.ObjectMeta;
import io.github.intisy.kubernetes.model.Service;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.*;

/**
 * Command to create a service.
 *
 * @author Finn Birich
 */
public class CreateServiceCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String name;
    private String type = "ClusterIP";
    private Map<String, String> selector;
    private List<Service.ServicePort> ports;
    private Map<String, String> labels;
    private Service service;

    public CreateServiceCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public CreateServiceCmd(KubernetesHttpClient client, Service service) {
        this.client = client;
        this.service = service;
    }

    public CreateServiceCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public CreateServiceCmd withName(String name) {
        this.name = name;
        return this;
    }

    public CreateServiceCmd withType(String type) {
        this.type = type;
        return this;
    }

    public CreateServiceCmd withSelector(String key, String value) {
        if (this.selector == null) this.selector = new HashMap<>();
        this.selector.put(key, value);
        return this;
    }

    public CreateServiceCmd withPort(int port, int targetPort) {
        if (this.ports == null) this.ports = new ArrayList<>();
        this.ports.add(new Service.ServicePort(port, targetPort));
        return this;
    }

    public CreateServiceCmd withLabel(String key, String value) {
        if (this.labels == null) this.labels = new HashMap<>();
        this.labels.put(key, value);
        return this;
    }

    public Service exec() {
        try {
            Service svc;
            if (this.service != null) {
                svc = this.service;
            } else {
                svc = buildService();
            }

            String ns = namespace;
            if (svc.getMetadata() != null && svc.getMetadata().getNamespace() != null) {
                ns = svc.getMetadata().getNamespace();
            }

            String path = "/api/v1/namespaces/" + ns + "/services";
            KubernetesResponse response = client.post(path, svc);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("Service already exists: " + name);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create service: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Service.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create service", e);
        }
    }

    private Service buildService() {
        Service svc = new Service();
        ObjectMeta meta = new ObjectMeta();
        meta.setName(name != null ? name : "svc-" + UUID.randomUUID().toString().substring(0, 8));
        meta.setNamespace(namespace);
        if (labels != null) meta.setLabels(labels);
        svc.setMetadata(meta);

        Service.ServiceSpec spec = new Service.ServiceSpec();
        spec.setType(type);
        if (selector != null) spec.setSelector(selector);
        if (ports != null) spec.setPorts(ports);
        svc.setSpec(spec);

        return svc;
    }
}
