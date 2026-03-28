package io.github.intisy.kubernetes;

import io.github.intisy.kubernetes.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Kubernetes model classes.
 *
 * @author Finn Birich
 */
public class ModelTest {

    @Test
    @DisplayName("Pod: default values and basic fields")
    public void testPodDefaults() {
        Pod pod = new Pod();
        assertEquals("v1", pod.getApiVersion());
        assertEquals("Pod", pod.getKind());
        assertNull(pod.getMetadata());
        assertNull(pod.getSpec());
        assertNull(pod.getStatus());

        ObjectMeta meta = new ObjectMeta().setName("test-pod");
        pod.setMetadata(meta);
        assertEquals(meta, pod.getMetadata());
        assertTrue(pod.toString().contains("test-pod"));
    }

    @Test
    @DisplayName("Pod: spec and containers")
    public void testPodSpec() {
        Pod.Container container = new Pod.Container()
                .setName("nginx")
                .setImage("nginx:latest")
                .addPort(new Pod.ContainerPort(80))
                .addEnv("ENV_VAR", "value");

        Pod.PodSpec spec = new Pod.PodSpec()
                .addContainer(container)
                .setRestartPolicy("Always");

        Pod pod = new Pod().setSpec(spec);

        assertEquals(1, pod.getSpec().getContainers().size());
        assertEquals("nginx", pod.getSpec().getContainers().get(0).getName());
        assertEquals("nginx:latest", pod.getSpec().getContainers().get(0).getImage());
        assertEquals(80, pod.getSpec().getContainers().get(0).getPorts().get(0).getContainerPort());
        assertEquals("ENV_VAR", pod.getSpec().getContainers().get(0).getEnv().get(0).getName());
        assertEquals("value", pod.getSpec().getContainers().get(0).getEnv().get(0).getValue());
        assertEquals("Always", pod.getSpec().getRestartPolicy());
    }

    @Test
    @DisplayName("Pod: resources and volumes")
    public void testPodResourcesAndVolumes() {
        Pod.ResourceRequirements resources = new Pod.ResourceRequirements()
                .addLimit("cpu", "500m")
                .addRequest("memory", "128Mi");

        Pod.Volume volume = new Pod.Volume()
                .setName("config-vol")
                .setConfigMap(new Pod.ConfigMapVolumeSource().setName("my-config"));

        Pod.Container container = new Pod.Container()
                .setResources(resources)
                .setVolumeMounts(Collections.singletonList(
                        new Pod.VolumeMount().setName("config-vol").setMountPath("/etc/config")
                ));

        Pod.PodSpec spec = new Pod.PodSpec()
                .addContainer(container)
                .setVolumes(Collections.singletonList(volume));

        assertEquals("500m", container.getResources().getLimits().get("cpu"));
        assertEquals("128Mi", container.getResources().getRequests().get("memory"));
        assertEquals("config-vol", spec.getVolumes().get(0).getName());
        assertEquals("my-config", spec.getVolumes().get(0).getConfigMap().getName());
    }

    @Test
    @DisplayName("Pod: status fields")
    public void testPodStatus() {
        Pod pod = new Pod();
        assertNull(pod.getStatus());
        // PodStatus has no setters, usually populated by Gson
    }

    @Test
    @DisplayName("Deployment: default values and spec")
    public void testDeployment() {
        Deployment deployment = new Deployment();
        assertEquals("apps/v1", deployment.getApiVersion());
        assertEquals("Deployment", deployment.getKind());

        Deployment.DeploymentSpec spec = new Deployment.DeploymentSpec()
                .setReplicas(3)
                .setSelector(new Deployment.LabelSelector().addMatchLabel("app", "nginx"))
                .setTemplate(new Deployment.PodTemplateSpec()
                        .setMetadata(new ObjectMeta().setLabels(Collections.singletonMap("app", "nginx")))
                        .setSpec(new Pod.PodSpec().addContainer(new Pod.Container().setName("nginx").setImage("nginx")))
                );

        deployment.setSpec(spec);
        assertEquals(3, deployment.getSpec().getReplicas());
        assertEquals("nginx", deployment.getSpec().getSelector().getMatchLabels().get("app"));
        assertTrue(deployment.toString().contains("replicas=3"));
    }

    @Test
    @DisplayName("Deployment: strategy")
    public void testDeploymentStrategy() {
        Deployment.RollingUpdateDeployment rollingUpdate = new Deployment.RollingUpdateDeployment()
                .setMaxSurge("25%")
                .setMaxUnavailable(1);

        Deployment.DeploymentStrategy strategy = new Deployment.DeploymentStrategy()
                .setType("RollingUpdate")
                .setRollingUpdate(rollingUpdate);

        assertEquals("RollingUpdate", strategy.getType());
        assertEquals("25%", strategy.getRollingUpdate().getMaxSurge());
        assertEquals(1, strategy.getRollingUpdate().getMaxUnavailable());
    }

    @Test
    @DisplayName("Service: default values and spec")
    public void testService() {
        Service service = new Service();
        assertEquals("v1", service.getApiVersion());
        assertEquals("Service", service.getKind());

        Service.ServiceSpec spec = new Service.ServiceSpec()
                .addPort(new Service.ServicePort(80, 8080))
                .addSelector("app", "nginx")
                .setType("ClusterIP");

        service.setSpec(spec);
        assertEquals(80, service.getSpec().getPorts().get(0).getPort());
        assertEquals(8080, service.getSpec().getPorts().get(0).getTargetPort());
        assertEquals("nginx", service.getSpec().getSelector().get("app"));
        assertEquals("ClusterIP", service.getSpec().getType());
        assertTrue(service.toString().contains("type=ClusterIP"));
    }

    @Test
    @DisplayName("ConfigMap: data and binaryData")
    public void testConfigMap() {
        ConfigMap configMap = new ConfigMap();
        assertEquals("v1", configMap.getApiVersion());
        assertEquals("ConfigMap", configMap.getKind());

        configMap.addData("key1", "value1")
                .setBinaryData(Collections.singletonMap("key2", "dmFsdWUy"));

        assertEquals("value1", configMap.getData().get("key1"));
        assertEquals("dmFsdWUy", configMap.getBinaryData().get("key2"));
        assertTrue(configMap.toString().contains("key1"));
    }

    @Test
    @DisplayName("Secret: data and stringData")
    public void testSecret() {
        Secret secret = new Secret();
        assertEquals("v1", secret.getApiVersion());
        assertEquals("Secret", secret.getKind());

        secret.addData("key1", "dmFsdWUy")
                .addStringData("key2", "value2")
                .setType("Opaque");

        assertEquals("dmFsdWUy", secret.getData().get("key1"));
        assertEquals("value2", secret.getStringData().get("key2"));
        assertEquals("Opaque", secret.getType());
        assertTrue(secret.toString().contains("type='Opaque'"));
    }

    @Test
    @DisplayName("ObjectMeta: fields and owner references")
    public void testObjectMeta() {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", "test");

        ObjectMeta meta = new ObjectMeta()
                .setName("test-name")
                .setNamespace("test-ns")
                .setGenerateName("test-gen-")
                .setLabels(labels)
                .setAnnotations(Collections.singletonMap("ann", "val"));

        assertEquals("test-name", meta.getName());
        assertEquals("test-ns", meta.getNamespace());
        assertEquals("test-gen-", meta.getGenerateName());
        assertEquals("test", meta.getLabels().get("app"));
        assertEquals("val", meta.getAnnotations().get("ann"));
        assertTrue(meta.toString().contains("test-name"));
    }

    @Test
    @DisplayName("VersionInfo: default nulls")
    public void testVersionInfo() {
        VersionInfo info = new VersionInfo();
        assertNull(info.getMajor());
        assertNull(info.getMinor());
        assertNull(info.getGitVersion());
        assertNull(info.getGitCommit());
        assertNull(info.getGitTreeState());
        assertNull(info.getBuildDate());
        assertNull(info.getGoVersion());
        assertNull(info.getCompiler());
        assertNull(info.getPlatform());
        assertTrue(info.toString().contains("VersionInfo"));
    }

    @Test
    @DisplayName("Namespace: basic construction")
    public void testNamespace() {
        Namespace ns = new Namespace();
        assertEquals("v1", ns.getApiVersion());
        assertEquals("Namespace", ns.getKind());
        ns.setMetadata(new ObjectMeta().setName("test-ns"));
        assertEquals("test-ns", ns.getMetadata().getName());
        assertTrue(ns.toString().contains("Namespace"));
    }

    @Test
    @DisplayName("Node: basic construction")
    public void testNode() {
        Node node = new Node();
        assertEquals("v1", node.getApiVersion());
        assertEquals("Node", node.getKind());
        assertTrue(node.toString().contains("Node"));
    }

    @Test
    @DisplayName("KubernetesList: basic construction")
    public void testKubernetesList() {
        KubernetesList<Pod> list = new KubernetesList<>();
        assertNull(list.getApiVersion());
        assertNull(list.getKind());
        assertNull(list.getItems());
        assertTrue(list.toString().contains("items=0"));
    }

    @Test
    @DisplayName("Pod: Volume sources")
    public void testVolumeSources() {
        Pod.Volume volume = new Pod.Volume()
                .setHostPath(new Pod.HostPathVolumeSource().setPath("/var/log").setType("Directory"))
                .setSecret(new Pod.SecretVolumeSource().setSecretName("my-secret"));

        assertEquals("/var/log", volume.getHostPath().getPath());
        assertEquals("Directory", volume.getHostPath().getType());
        assertEquals("my-secret", volume.getSecret().getSecretName());
    }

    @Test
    @DisplayName("Pod: ContainerPort and EnvVar")
    public void testContainerPortAndEnvVar() {
        Pod.ContainerPort port = new Pod.ContainerPort(8080)
                .setName("http")
                .setHostPort(80)
                .setProtocol("TCP");

        assertEquals(8080, port.getContainerPort());
        assertEquals("http", port.getName());
        assertEquals(80, port.getHostPort());
        assertEquals("TCP", port.getProtocol());

        Pod.EnvVar env = new Pod.EnvVar().setName("K1").setValue("V1");
        assertEquals("K1", env.getName());
        assertEquals("V1", env.getValue());
    }

    @Test
    @DisplayName("Deployment: LabelSelector")
    public void testLabelSelector() {
        Deployment.LabelSelector selector = new Deployment.LabelSelector()
                .addMatchLabel("k1", "v1")
                .setMatchLabels(Collections.singletonMap("k2", "v2"));

        assertEquals("v2", selector.getMatchLabels().get("k2"));
        assertNull(selector.getMatchLabels().get("k1")); // setMatchLabels overwrites
    }

    @Test
    @DisplayName("Service: ServicePort")
    public void testServicePort() {
        Service.ServicePort port = new Service.ServicePort(80, 8080)
                .setName("web")
                .setProtocol("UDP")
                .setNodePort(30001);

        assertEquals(80, port.getPort());
        assertEquals(8080, port.getTargetPort());
        assertEquals("web", port.getName());
        assertEquals("UDP", port.getProtocol());
        assertEquals(30001, port.getNodePort());
    }

    @Test
    @DisplayName("ObjectMeta: OwnerReference")
    public void testOwnerReference() {
        // OwnerReference has no setters in the model, usually populated by Gson
        ObjectMeta.OwnerReference ref = new ObjectMeta.OwnerReference();
        assertNull(ref.getApiVersion());
        assertNull(ref.getKind());
        assertNull(ref.getName());
        assertNull(ref.getUid());
        assertNull(ref.getController());
        assertNull(ref.getBlockOwnerDeletion());
    }
}
