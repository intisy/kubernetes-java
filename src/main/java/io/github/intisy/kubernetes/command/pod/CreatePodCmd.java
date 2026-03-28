package io.github.intisy.kubernetes.command.pod;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.ObjectMeta;
import io.github.intisy.kubernetes.model.Pod;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.*;

/**
 * Command to create a pod.
 *
 * @author Finn Birich
 */
public class CreatePodCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String name;
    private String image;
    private List<String> command;
    private Map<String, String> labels;
    private Pod pod;

    public CreatePodCmd(KubernetesHttpClient client, String image) {
        this.client = client;
        this.image = image;
    }

    public CreatePodCmd(KubernetesHttpClient client, Pod pod) {
        this.client = client;
        this.pod = pod;
    }

    public CreatePodCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public CreatePodCmd withName(String name) {
        this.name = name;
        return this;
    }

    public CreatePodCmd withCmd(String... command) {
        this.command = Arrays.asList(command);
        return this;
    }

    public CreatePodCmd withLabel(String key, String value) {
        if (this.labels == null) this.labels = new HashMap<>();
        this.labels.put(key, value);
        return this;
    }

    public Pod exec() {
        try {
            Pod podToCreate;
            if (this.pod != null) {
                podToCreate = this.pod;
            } else {
                podToCreate = buildPod();
            }

            String ns = namespace;
            if (podToCreate.getMetadata() != null && podToCreate.getMetadata().getNamespace() != null) {
                ns = podToCreate.getMetadata().getNamespace();
            }

            String path = "/api/v1/namespaces/" + ns + "/pods";
            KubernetesResponse response = client.post(path, podToCreate);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("Pod already exists: " + name);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create pod: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Pod.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create pod", e);
        }
    }

    private Pod buildPod() {
        Pod pod = new Pod();
        ObjectMeta meta = new ObjectMeta();
        meta.setName(name != null ? name : "pod-" + UUID.randomUUID().toString().substring(0, 8));
        meta.setNamespace(namespace);
        if (labels != null) meta.setLabels(labels);
        pod.setMetadata(meta);

        Pod.Container container = new Pod.Container();
        container.setName("main");
        container.setImage(image);
        if (command != null) container.setCommand(command);

        Pod.PodSpec spec = new Pod.PodSpec();
        spec.addContainer(container);
        spec.setRestartPolicy("Never");
        pod.setSpec(spec);

        return pod;
    }
}
