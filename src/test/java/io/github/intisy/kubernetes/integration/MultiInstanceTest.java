package io.github.intisy.kubernetes.integration;

import io.github.intisy.kubernetes.KubernetesClient;
import io.github.intisy.kubernetes.KubernetesProvider;
import io.github.intisy.kubernetes.model.ObjectMeta;
import io.github.intisy.kubernetes.model.Pod;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Tests for multiple KubernetesClient instances sharing a single cluster.
 * Validates that multiple clients can operate independently on the same Minikube instance.
 * <p>
 * Uses a SINGLE provider/cluster (not two) to keep startup fast and resource-light.
 * Also verifies that unique instance IDs are generated for each provider.
 * <p>
 * Requires a working Docker + Minikube environment.
 *
 * @author Finn Birich
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultiInstanceTest {

    private static final Logger log = LoggerFactory.getLogger(MultiInstanceTest.class);

    private static KubernetesProvider provider;
    private static KubernetesClient client1;
    private static KubernetesClient client2;

    @BeforeAll
    static void setUp() throws Exception {
        log.info("Setting up single provider with two clients...");
        provider = KubernetesProvider.get();
        provider.start();

        client1 = provider.getClient();
        client2 = provider.createClient();

        boolean healthy = false;
        for (int i = 0; i < 10; i++) {
            try {
                client1.healthz().exec();
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
    static void tearDown() {
        if (provider != null) {
            log.info("Stopping provider...");
            try {
                provider.stop();
            } catch (Exception e) {
                log.warn("Failed to stop provider: {}", e.getMessage());
            }
        }
    }

    @Test
    @Order(1)
    void testUniqueInstanceIds() {
        log.info("Testing unique instance IDs for multiple providers...");
        KubernetesProvider p1 = KubernetesProvider.get();
        KubernetesProvider p2 = KubernetesProvider.get();
        KubernetesProvider p3 = KubernetesProvider.get();

        Set<String> ids = new HashSet<String>();
        ids.add(p1.getInstanceId());
        ids.add(p2.getInstanceId());
        ids.add(p3.getInstanceId());

        Assertions.assertEquals(3, ids.size(), "All three providers should have unique instance IDs");
        log.info("Instance IDs: {}, {}, {}", p1.getInstanceId(), p2.getInstanceId(), p3.getInstanceId());
    }

    @Test
    @Order(2)
    void testBothClientsHealthz() throws Exception {
        log.info("Verifying healthz on both clients...");
        Object health1 = client1.healthz().exec();
        Object health2 = client2.healthz().exec();
        Assertions.assertNotNull(health1, "Client 1 healthz should not be null");
        Assertions.assertNotNull(health2, "Client 2 healthz should not be null");
        log.info("Both clients report healthy cluster");
    }

    @Test
    @Order(3)
    void testBothClientsVersion() throws Exception {
        log.info("Verifying version on both clients...");
        Assertions.assertNotNull(client1.version().exec(), "Client 1 version should not be null");
        Assertions.assertNotNull(client2.version().exec(), "Client 2 version should not be null");
        log.info("Both clients successfully retrieved cluster version");
    }

    @Test
    @Order(4)
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testCrossClientPodVisibility() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String podName = "multi-client-pod-" + timestamp;

        log.info("Creating pod {} via client 1...", podName);
        Pod pod = new Pod();
        pod.setMetadata(new ObjectMeta());
        pod.getMetadata().setName(podName);
        Pod.PodSpec spec = new Pod.PodSpec();
        Pod.Container container = new Pod.Container();
        container.setName("nginx");
        container.setImage("nginx:alpine");
        spec.setContainers(Collections.singletonList(container));
        pod.setSpec(spec);

        Pod created = client1.createPod(pod).withNamespace("default").exec();
        Assertions.assertNotNull(created, "Pod created via client 1 should not be null");
        Assertions.assertEquals(podName, created.getMetadata().getName());

        log.info("Verifying pod {} is visible via client 2...", podName);
        Pod retrieved = client2.getPod(podName).withNamespace("default").exec();
        Assertions.assertNotNull(retrieved, "Pod should be visible via client 2");
        Assertions.assertEquals(podName, retrieved.getMetadata().getName());
        log.info("Cross-client pod visibility confirmed");

        log.info("Deleting pod {} via client 2...", podName);
        try {
            client2.deletePod(podName).withNamespace("default").exec();
            log.info("Pod {} deleted successfully", podName);
        } catch (Exception e) {
            log.warn("Failed to delete pod {}: {}", podName, e.getMessage());
        }
    }

    @Test
    @Order(5)
    void testBothClientsListPods() throws Exception {
        log.info("Listing pods via both clients...");
        Assertions.assertNotNull(client1.listPods().withNamespace("default").exec(),
                "Client 1 should list pods");
        Assertions.assertNotNull(client2.listPods().withNamespace("default").exec(),
                "Client 2 should list pods");
        log.info("Both clients can list pods independently");
    }
}
