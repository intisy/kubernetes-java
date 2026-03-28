package io.github.intisy.kubernetes;

import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integration tests for Kubernetes operations via KubernetesProvider.
 * Tests pod, deployment, service, configmap, secret, namespace, and node operations.
 *
 * @author Finn Birich
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PodTest {

    private static final Logger log = LoggerFactory.getLogger(PodTest.class);
    private static final String TIMESTAMP = String.valueOf(System.currentTimeMillis());
    private static final String TEST_NAMESPACE = "k8s-java-test-" + TIMESTAMP;
    private static final String TEST_POD_NAME = "test-pod-" + TIMESTAMP;
    private static final String TEST_DEPLOYMENT_NAME = "test-deploy-" + TIMESTAMP;
    private static final String TEST_SERVICE_NAME = "test-svc-" + TIMESTAMP;
    private static final String TEST_CONFIGMAP_NAME = "test-cm-" + TIMESTAMP;
    private static final String TEST_SECRET_NAME = "test-secret-" + TIMESTAMP;

    private static KubernetesProvider provider;
    private static KubernetesClient client;

    @BeforeAll
    static void setUp() throws Exception {
        log.info("Setting up KubernetesProvider...");
        provider = KubernetesProvider.get();
        provider.start();
        client = provider.getClient();

        log.info("Waiting for cluster to become healthy...");
        boolean healthy = false;
        for (int i = 0; i < 10; i++) {
            try {
                client.healthz().exec();
                healthy = true;
                log.info("Cluster is healthy after {} attempt(s)", i + 1);
                break;
            } catch (Exception e) {
                log.warn("Cluster not ready yet (attempt {}), retrying in 1s...", i + 1);
                Thread.sleep(1000);
            }
        }
        Assertions.assertTrue(healthy, "Cluster did not become healthy within 10 attempts");
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (client != null) {
            log.info("Cleaning up test resources...");
            try {
                client.deletePod(TEST_POD_NAME).withNamespace(TEST_NAMESPACE).exec();
            } catch (Exception e) {
                log.debug("Pod cleanup skipped: {}", e.getMessage());
            }
            try {
                client.deleteDeployment(TEST_DEPLOYMENT_NAME).withNamespace(TEST_NAMESPACE).exec();
            } catch (Exception e) {
                log.debug("Deployment cleanup skipped: {}", e.getMessage());
            }
            try {
                client.deleteService(TEST_SERVICE_NAME).withNamespace(TEST_NAMESPACE).exec();
            } catch (Exception e) {
                log.debug("Service cleanup skipped: {}", e.getMessage());
            }
            try {
                client.deleteConfigMap(TEST_CONFIGMAP_NAME).withNamespace(TEST_NAMESPACE).exec();
            } catch (Exception e) {
                log.debug("ConfigMap cleanup skipped: {}", e.getMessage());
            }
            try {
                client.deleteSecret(TEST_SECRET_NAME).withNamespace(TEST_NAMESPACE).exec();
            } catch (Exception e) {
                log.debug("Secret cleanup skipped: {}", e.getMessage());
            }
            try {
                client.deleteNamespace(TEST_NAMESPACE).exec();
            } catch (Exception e) {
                log.debug("Namespace cleanup skipped: {}", e.getMessage());
            }
        }
        if (provider != null) {
            log.info("Stopping KubernetesProvider...");
            provider.stop();
        }
    }

    @Test
    @Order(1)
    void testHealthz() throws Exception {
        log.info("Testing healthz endpoint...");
        Object result = client.healthz().exec();
        Assertions.assertNotNull(result, "Healthz result should not be null");
        log.info("Healthz result: {}", result);
    }

    @Test
    @Order(2)
    void testVersion() throws Exception {
        log.info("Testing version endpoint...");
        VersionInfo versionInfo = client.version().exec();
        Assertions.assertNotNull(versionInfo, "VersionInfo should not be null");
        Assertions.assertNotNull(versionInfo.getGitVersion(), "Git version should not be null");
        Assertions.assertNotNull(versionInfo.getPlatform(), "Platform should not be null");
        log.info("Kubernetes version: {}", versionInfo.getGitVersion());
    }

    @Test
    @Order(3)
    void testListNamespaces() throws Exception {
        log.info("Testing list namespaces...");
        List<Namespace> namespaces = client.listNamespaces().exec();
        Assertions.assertNotNull(namespaces, "Namespaces list should not be null");
        Assertions.assertFalse(namespaces.isEmpty(), "Should have at least one namespace");
        boolean hasDefault = false;
        for (Namespace ns : namespaces) {
            if ("default".equals(ns.getMetadata().getName())) {
                hasDefault = true;
                break;
            }
        }
        Assertions.assertTrue(hasDefault, "Should have 'default' namespace");
        log.info("Found {} namespaces", namespaces.size());
    }

    @Test
    @Order(10)
    void testCreateNamespace() throws Exception {
        log.info("Creating test namespace: {}", TEST_NAMESPACE);

        Namespace created = client.createNamespace().withName(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(created, "Created namespace should not be null");
        Assertions.assertEquals(TEST_NAMESPACE, created.getMetadata().getName());
        log.info("Successfully created namespace: {}", TEST_NAMESPACE);
    }

    @Test
    @Order(11)
    void testGetNamespace() throws Exception {
        log.info("Getting test namespace: {}", TEST_NAMESPACE);
        List<Namespace> namespaces = client.listNamespaces().exec();
        boolean found = false;
        for (Namespace ns : namespaces) {
            if (TEST_NAMESPACE.equals(ns.getMetadata().getName())) {
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found, "Test namespace should exist in namespace list");
        log.info("Verified namespace {} exists", TEST_NAMESPACE);
    }

    @Test
    @Order(20)
    void testCreatePod() throws Exception {
        log.info("Creating test pod: {} in namespace: {}", TEST_POD_NAME, TEST_NAMESPACE);
        Pod pod = new Pod();
        pod.setMetadata(new ObjectMeta());
        pod.getMetadata().setName(TEST_POD_NAME);

        Pod.PodSpec spec = new Pod.PodSpec();
        Pod.Container container = new Pod.Container();
        container.setName("nginx");
        container.setImage("nginx:alpine");
        spec.setContainers(Collections.singletonList(container));
        pod.setSpec(spec);

        Pod created = client.createPod(pod).withNamespace(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(created, "Created pod should not be null");
        Assertions.assertEquals(TEST_POD_NAME, created.getMetadata().getName());
        log.info("Successfully created pod: {}", TEST_POD_NAME);
    }

    @Test
    @Order(21)
    void testGetPod() throws Exception {
        log.info("Getting test pod: {}", TEST_POD_NAME);
        Pod pod = client.getPod(TEST_POD_NAME).withNamespace(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(pod, "Pod should not be null");
        Assertions.assertEquals(TEST_POD_NAME, pod.getMetadata().getName());
        log.info("Retrieved pod: {}", pod.getMetadata().getName());
    }

    @Test
    @Order(22)
    void testListPods() throws Exception {
        log.info("Listing pods in namespace: {}", TEST_NAMESPACE);
        List<Pod> pods = client.listPods().withNamespace(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(pods, "Pods list should not be null");
        Assertions.assertTrue(pods.size() >= 1, "Should have at least 1 pod");
        log.info("Found {} pods in namespace {}", pods.size(), TEST_NAMESPACE);
    }

    @Test
    @Order(23)
    void testDeletePod() throws Exception {
        log.info("Deleting test pod: {}", TEST_POD_NAME);
        client.deletePod(TEST_POD_NAME).withNamespace(TEST_NAMESPACE).exec();
        log.info("Successfully deleted pod: {}", TEST_POD_NAME);
    }

    @Test
    @Order(30)
    void testCreateDeployment() throws Exception {
        log.info("Creating test deployment: {} in namespace: {}", TEST_DEPLOYMENT_NAME, TEST_NAMESPACE);
        Map<String, String> labels = new HashMap<String, String>();
        labels.put("app", "test-nginx");

        Deployment deployment = new Deployment();
        deployment.setMetadata(new ObjectMeta());
        deployment.getMetadata().setName(TEST_DEPLOYMENT_NAME);
        deployment.getMetadata().setLabels(labels);

        Deployment.DeploymentSpec deploySpec = new Deployment.DeploymentSpec();
        deploySpec.setReplicas(1);

        Deployment.LabelSelector selector = new Deployment.LabelSelector();
        selector.setMatchLabels(labels);
        deploySpec.setSelector(selector);

        Deployment.PodTemplateSpec template = new Deployment.PodTemplateSpec();
        template.setMetadata(new ObjectMeta());
        template.getMetadata().setLabels(labels);

        Pod.PodSpec podSpec = new Pod.PodSpec();
        Pod.Container container = new Pod.Container();
        container.setName("nginx");
        container.setImage("nginx:alpine");
        podSpec.setContainers(Collections.singletonList(container));
        template.setSpec(podSpec);

        deploySpec.setTemplate(template);
        deployment.setSpec(deploySpec);

        Deployment created = client.createDeployment(deployment).withNamespace(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(created, "Created deployment should not be null");
        Assertions.assertEquals(TEST_DEPLOYMENT_NAME, created.getMetadata().getName());
        log.info("Successfully created deployment: {}", TEST_DEPLOYMENT_NAME);
    }

    @Test
    @Order(31)
    void testGetDeployment() throws Exception {
        log.info("Getting test deployment: {}", TEST_DEPLOYMENT_NAME);
        Deployment deployment = client.getDeployment(TEST_DEPLOYMENT_NAME).withNamespace(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(deployment, "Deployment should not be null");
        Assertions.assertEquals(TEST_DEPLOYMENT_NAME, deployment.getMetadata().getName());
        log.info("Retrieved deployment: {}", deployment.getMetadata().getName());
    }

    @Test
    @Order(32)
    void testScaleDeployment() throws Exception {
        log.info("Scaling deployment {} to 2 replicas", TEST_DEPLOYMENT_NAME);
        client.scaleDeployment(TEST_DEPLOYMENT_NAME).withNamespace(TEST_NAMESPACE).withReplicas(2).exec();
        log.info("Successfully scaled deployment: {} to 2 replicas", TEST_DEPLOYMENT_NAME);
    }

    @Test
    @Order(33)
    void testListDeployments() throws Exception {
        log.info("Listing deployments in namespace: {}", TEST_NAMESPACE);
        List<Deployment> deployments = client.listDeployments().withNamespace(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(deployments, "Deployments list should not be null");
        Assertions.assertTrue(deployments.size() >= 1, "Should have at least 1 deployment");
        log.info("Found {} deployments in namespace {}", deployments.size(), TEST_NAMESPACE);
    }

    @Test
    @Order(34)
    void testDeleteDeployment() throws Exception {
        log.info("Deleting test deployment: {}", TEST_DEPLOYMENT_NAME);
        client.deleteDeployment(TEST_DEPLOYMENT_NAME).withNamespace(TEST_NAMESPACE).exec();
        log.info("Successfully deleted deployment: {}", TEST_DEPLOYMENT_NAME);
    }

    @Test
    @Order(40)
    void testCreateService() throws Exception {
        log.info("Creating test service: {} in namespace: {}", TEST_SERVICE_NAME, TEST_NAMESPACE);
        Map<String, String> selector = new HashMap<String, String>();
        selector.put("app", "test-nginx");

        Service service = new Service();
        service.setMetadata(new ObjectMeta());
        service.getMetadata().setName(TEST_SERVICE_NAME);

        Service.ServiceSpec serviceSpec = new Service.ServiceSpec();
        serviceSpec.setType("ClusterIP");
        serviceSpec.setSelector(selector);

        Service.ServicePort port = new Service.ServicePort();
        port.setPort(80);
        port.setTargetPort(80);
        port.setProtocol("TCP");
        serviceSpec.setPorts(Collections.singletonList(port));

        service.setSpec(serviceSpec);

        Service created = client.createService(service).withNamespace(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(created, "Created service should not be null");
        Assertions.assertEquals(TEST_SERVICE_NAME, created.getMetadata().getName());
        log.info("Successfully created service: {}", TEST_SERVICE_NAME);
    }

    @Test
    @Order(41)
    void testGetService() throws Exception {
        log.info("Getting test service: {}", TEST_SERVICE_NAME);
        Service service = client.getService(TEST_SERVICE_NAME).withNamespace(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(service, "Service should not be null");
        Assertions.assertEquals(TEST_SERVICE_NAME, service.getMetadata().getName());
        log.info("Retrieved service: {}", service.getMetadata().getName());
    }

    @Test
    @Order(42)
    void testListServices() throws Exception {
        log.info("Listing services in namespace: {}", TEST_NAMESPACE);
        List<Service> services = client.listServices().withNamespace(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(services, "Services list should not be null");
        log.info("Found {} services in namespace {}", services.size(), TEST_NAMESPACE);
    }

    @Test
    @Order(43)
    void testDeleteService() throws Exception {
        log.info("Deleting test service: {}", TEST_SERVICE_NAME);
        client.deleteService(TEST_SERVICE_NAME).withNamespace(TEST_NAMESPACE).exec();
        log.info("Successfully deleted service: {}", TEST_SERVICE_NAME);
    }

    @Test
    @Order(50)
    void testCreateConfigMap() throws Exception {
        log.info("Creating test configmap: {} in namespace: {}", TEST_CONFIGMAP_NAME, TEST_NAMESPACE);
        Map<String, String> data = new HashMap<String, String>();
        data.put("config.properties", "key1=value1\nkey2=value2");
        data.put("app.name", "kubernetes-java-test");

        ConfigMap configMap = new ConfigMap();
        configMap.setMetadata(new ObjectMeta());
        configMap.getMetadata().setName(TEST_CONFIGMAP_NAME);
        configMap.setData(data);

        ConfigMap created = client.createConfigMap(configMap).withNamespace(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(created, "Created configmap should not be null");
        Assertions.assertEquals(TEST_CONFIGMAP_NAME, created.getMetadata().getName());
        log.info("Successfully created configmap: {}", TEST_CONFIGMAP_NAME);
    }

    @Test
    @Order(51)
    void testGetConfigMap() throws Exception {
        log.info("Getting test configmap: {}", TEST_CONFIGMAP_NAME);
        ConfigMap configMap = client.getConfigMap(TEST_CONFIGMAP_NAME).withNamespace(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(configMap, "ConfigMap should not be null");
        Assertions.assertNotNull(configMap.getData(), "ConfigMap data should not be null");
        Assertions.assertEquals("kubernetes-java-test", configMap.getData().get("app.name"));
        log.info("Retrieved configmap with {} data entries", configMap.getData().size());
    }

    @Test
    @Order(52)
    void testDeleteConfigMap() throws Exception {
        log.info("Deleting test configmap: {}", TEST_CONFIGMAP_NAME);
        client.deleteConfigMap(TEST_CONFIGMAP_NAME).withNamespace(TEST_NAMESPACE).exec();
        log.info("Successfully deleted configmap: {}", TEST_CONFIGMAP_NAME);
    }

    @Test
    @Order(60)
    void testCreateSecret() throws Exception {
        log.info("Creating test secret: {} in namespace: {}", TEST_SECRET_NAME, TEST_NAMESPACE);
        Map<String, String> data = new HashMap<String, String>();
        data.put("username", "dGVzdC11c2Vy");
        data.put("password", "dGVzdC1wYXNz");

        Secret secret = new Secret();
        secret.setMetadata(new ObjectMeta());
        secret.getMetadata().setName(TEST_SECRET_NAME);
        secret.setData(data);

        Secret created = client.createSecret(secret).withNamespace(TEST_NAMESPACE).exec();
        Assertions.assertNotNull(created, "Created secret should not be null");
        Assertions.assertEquals(TEST_SECRET_NAME, created.getMetadata().getName());
        log.info("Successfully created secret: {}", TEST_SECRET_NAME);
    }

    @Test
    @Order(61)
    void testDeleteSecret() throws Exception {
        log.info("Deleting test secret: {}", TEST_SECRET_NAME);
        client.deleteSecret(TEST_SECRET_NAME).withNamespace(TEST_NAMESPACE).exec();
        log.info("Successfully deleted secret: {}", TEST_SECRET_NAME);
    }

    @Test
    @Order(70)
    void testListNodes() throws Exception {
        log.info("Listing cluster nodes...");
        List<Node> nodes = client.listNodes().exec();
        Assertions.assertNotNull(nodes, "Nodes list should not be null");
        Assertions.assertTrue(nodes.size() >= 1, "Should have at least 1 node");
        for (Node node : nodes) {
            log.info("Node: {}", node.getMetadata().getName());
        }
    }

    @Test
    @Order(71)
    void testGetNode() throws Exception {
        log.info("Getting a node by name...");
        List<Node> nodes = client.listNodes().exec();
        Assertions.assertFalse(nodes.isEmpty(), "Should have at least 1 node to get");
        String nodeName = nodes.get(0).getMetadata().getName();

        Node node = client.getNode(nodeName).exec();
        Assertions.assertNotNull(node, "Node should not be null");
        Assertions.assertEquals(nodeName, node.getMetadata().getName());
        log.info("Retrieved node: {}", nodeName);
    }

    @Test
    @Order(80)
    void testNotFoundPod() {
        log.info("Testing NotFoundException for non-existent pod...");
        Assertions.assertThrows(NotFoundException.class, () ->
                client.getPod("non-existent-pod-xyz").withNamespace(TEST_NAMESPACE).exec()
        );
        log.info("NotFoundException correctly thrown for non-existent pod");
    }

    @Test
    @Order(81)
    void testNotFoundDeployment() {
        log.info("Testing NotFoundException for non-existent deployment...");
        Assertions.assertThrows(NotFoundException.class, () ->
                client.getDeployment("non-existent-deploy-xyz").withNamespace(TEST_NAMESPACE).exec()
        );
        log.info("NotFoundException correctly thrown for non-existent deployment");
    }

    @Test
    @Order(90)
    void testDeleteTestNamespace() throws Exception {
        log.info("Deleting test namespace: {}", TEST_NAMESPACE);
        client.deleteNamespace(TEST_NAMESPACE).exec();
        log.info("Successfully deleted test namespace: {}", TEST_NAMESPACE);
    }
}
