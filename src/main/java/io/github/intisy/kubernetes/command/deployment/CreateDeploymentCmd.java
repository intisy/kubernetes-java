package io.github.intisy.kubernetes.command.deployment;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.Deployment;
import io.github.intisy.kubernetes.model.ObjectMeta;
import io.github.intisy.kubernetes.model.Pod;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.*;

/**
 * Command to create a deployment.
 *
 * @author Finn Birich
 */
public class CreateDeploymentCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String name;
    private String image;
    private Integer replicas = 1;
    private Map<String, String> labels;
    private List<String> command;
    private Deployment deployment;

    public CreateDeploymentCmd(KubernetesHttpClient client, String image) {
        this.client = client;
        this.image = image;
    }

    public CreateDeploymentCmd(KubernetesHttpClient client, Deployment deployment) {
        this.client = client;
        this.deployment = deployment;
    }

    public CreateDeploymentCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public CreateDeploymentCmd withName(String name) {
        this.name = name;
        return this;
    }

    public CreateDeploymentCmd withReplicas(int replicas) {
        this.replicas = replicas;
        return this;
    }

    public CreateDeploymentCmd withLabel(String key, String value) {
        if (this.labels == null) this.labels = new HashMap<>();
        this.labels.put(key, value);
        return this;
    }

    public CreateDeploymentCmd withCmd(String... command) {
        this.command = Arrays.asList(command);
        return this;
    }

    public Deployment exec() {
        try {
            Deployment deploy;
            if (this.deployment != null) {
                deploy = this.deployment;
            } else {
                deploy = buildDeployment();
            }

            String ns = namespace;
            if (deploy.getMetadata() != null && deploy.getMetadata().getNamespace() != null) {
                ns = deploy.getMetadata().getNamespace();
            }

            String path = "/apis/apps/v1/namespaces/" + ns + "/deployments";
            KubernetesResponse response = client.post(path, deploy);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("Deployment already exists: " + name);
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create deployment: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), Deployment.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create deployment", e);
        }
    }

    private Deployment buildDeployment() {
        String deployName = name != null ? name : "deploy-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, String> selectorLabels = new HashMap<>();
        selectorLabels.put("app", deployName);
        if (labels != null) selectorLabels.putAll(labels);

        Deployment deploy = new Deployment();
        ObjectMeta meta = new ObjectMeta();
        meta.setName(deployName);
        meta.setNamespace(namespace);
        meta.setLabels(selectorLabels);
        deploy.setMetadata(meta);

        Deployment.DeploymentSpec spec = new Deployment.DeploymentSpec();
        spec.setReplicas(replicas);

        Deployment.LabelSelector selector = new Deployment.LabelSelector();
        selector.setMatchLabels(selectorLabels);
        spec.setSelector(selector);

        Deployment.PodTemplateSpec template = new Deployment.PodTemplateSpec();
        ObjectMeta templateMeta = new ObjectMeta();
        templateMeta.setLabels(selectorLabels);
        template.setMetadata(templateMeta);

        Pod.Container container = new Pod.Container();
        container.setName("main");
        container.setImage(image);
        if (command != null) container.setCommand(command);

        Pod.PodSpec podSpec = new Pod.PodSpec();
        podSpec.addContainer(container);
        template.setSpec(podSpec);
        spec.setTemplate(template);

        deploy.setSpec(spec);
        return deploy;
    }
}
