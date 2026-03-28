package io.github.intisy.kubernetes;

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
 * Tests for multiple KubernetesProvider instances running simultaneously.
 *
 * @author Finn Birich
 */
public class MultiInstanceTest {

    private static final Logger log = LoggerFactory.getLogger(MultiInstanceTest.class);

    @Test
    void testUniqueInstanceIds() {
        log.info("Testing unique instance IDs for multiple providers...");
        KubernetesProvider provider1 = KubernetesProvider.get();
        KubernetesProvider provider2 = KubernetesProvider.get();
        KubernetesProvider provider3 = KubernetesProvider.get();

        Set<String> ids = new HashSet<String>();
        ids.add(provider1.getInstanceId());
        ids.add(provider2.getInstanceId());
        ids.add(provider3.getInstanceId());

        Assertions.assertEquals(3, ids.size(), "All three providers should have unique instance IDs");
        log.info("Instance IDs: {}, {}, {}", provider1.getInstanceId(), provider2.getInstanceId(), provider3.getInstanceId());
    }

    @Test
    @Timeout(value = 900, unit = TimeUnit.SECONDS)
    void testTwoProvidersSimultaneously() throws Exception {
        log.info("Testing two providers running simultaneously...");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String podName1 = "multi-test-pod1-" + timestamp;
        String podName2 = "multi-test-pod2-" + timestamp;

        KubernetesProvider provider1 = KubernetesProvider.get();
        KubernetesProvider provider2 = KubernetesProvider.get();

        try {
            provider1.start();
            provider2.start();

            KubernetesClient client1 = provider1.getClient();
            KubernetesClient client2 = provider2.getClient();

            // Verify both clients can call healthz
            log.info("Verifying healthz on both clients...");
            Object health1 = client1.healthz().exec();
            Object health2 = client2.healthz().exec();
            Assertions.assertNotNull(health1, "Client 1 healthz should not be null");
            Assertions.assertNotNull(health2, "Client 2 healthz should not be null");

            // Verify both clients can call version
            log.info("Verifying version on both clients...");
            Assertions.assertNotNull(client1.version().exec(), "Client 1 version should not be null");
            Assertions.assertNotNull(client2.version().exec(), "Client 2 version should not be null");

            // Create a pod on each client
            log.info("Creating pod {} via client 1...", podName1);
            Pod pod1 = new Pod();
            pod1.setMetadata(new ObjectMeta());
            pod1.getMetadata().setName(podName1);
            Pod.PodSpec spec1 = new Pod.PodSpec();
            Pod.Container container1 = new Pod.Container();
            container1.setName("nginx");
            container1.setImage("nginx:alpine");
            spec1.setContainers(Collections.singletonList(container1));
            pod1.setSpec(spec1);
            Pod created1 = client1.createPod(pod1).withNamespace("default").exec();
            Assertions.assertNotNull(created1, "Pod created via client 1 should not be null");

            log.info("Creating pod {} via client 2...", podName2);
            Pod pod2 = new Pod();
            pod2.setMetadata(new ObjectMeta());
            pod2.getMetadata().setName(podName2);
            Pod.PodSpec spec2 = new Pod.PodSpec();
            Pod.Container container2 = new Pod.Container();
            container2.setName("nginx");
            container2.setImage("nginx:alpine");
            spec2.setContainers(Collections.singletonList(container2));
            pod2.setSpec(spec2);
            Pod created2 = client2.createPod(pod2).withNamespace("default").exec();
            Assertions.assertNotNull(created2, "Pod created via client 2 should not be null");

            log.info("Both pods created successfully");

            // Cleanup pods
            log.info("Cleaning up test pods...");
            try {
                client1.deletePod(podName1).withNamespace("default").exec();
            } catch (Exception e) {
                log.warn("Failed to delete pod {}: {}", podName1, e.getMessage());
            }
            try {
                client2.deletePod(podName2).withNamespace("default").exec();
            } catch (Exception e) {
                log.warn("Failed to delete pod {}: {}", podName2, e.getMessage());
            }
        } finally {
            log.info("Stopping both providers...");
            try {
                provider1.stop();
            } catch (Exception e) {
                log.warn("Failed to stop provider 1: {}", e.getMessage());
            }
            try {
                provider2.stop();
            } catch (Exception e) {
                log.warn("Failed to stop provider 2: {}", e.getMessage());
            }
        }
        log.info("Multi-instance test completed successfully");
    }
}
