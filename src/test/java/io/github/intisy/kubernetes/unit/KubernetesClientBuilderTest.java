package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.KubernetesClient;
import io.github.intisy.kubernetes.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightweight unit tests for KubernetesClient.Builder.
 * No cluster needed.
 *
 * @author Finn Birich
 */
public class KubernetesClientBuilderTest {

    @Test
    @DisplayName("KubernetesClient.builder().build() creates non-null client with default apiServer")
    void testDefaultBuilder() {
        KubernetesClient client = KubernetesClient.builder().build();
        assertNotNull(client);
    }

    @Test
    @DisplayName(".withApiServer(\"https://192.168.49.2:8443\").build() creates non-null client")
    void testWithApiServer() {
        KubernetesClient client = KubernetesClient.builder()
                .withApiServer("https://192.168.49.2:8443")
                .build();
        assertNotNull(client);
    }

    @Test
    @DisplayName(".withBearerToken(\"test-token\").build() creates non-null client")
    void testWithBearerToken() {
        KubernetesClient client = KubernetesClient.builder()
                .withBearerToken("test-token")
                .build();
        assertNotNull(client);
    }

    @Test
    @DisplayName(".withCaCert(path to a real CA file) verifies against it and succeeds")
    void testWithCaCert(@TempDir Path tempDir) throws Exception {
        Path caFile = tempDir.resolve("ca.pem");
        Files.write(caFile, fixture("tls-cert.pem"));

        KubernetesClient client = KubernetesClient.builder()
                .withCaCert(caFile.toString())
                .build();
        assertNotNull(client);
    }

    @Test
    @DisplayName(".withCaCert(path to an unparsable CA file) fails loudly instead of silently ignoring it")
    void testWithCaCertFailsLoudlyOnAnUnparsableFile(@TempDir Path tempDir) throws Exception {
        Path caFile = tempDir.resolve("ca.pem");
        Files.write(caFile, "not a certificate".getBytes(StandardCharsets.UTF_8));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                KubernetesClient.builder()
                        .withCaCert(caFile.toString())
                        .build());
        assertTrue(thrown.getMessage().contains("TLS"));
    }

    @Test
    @DisplayName(".withClientCert(path).withClientKey(path) with no CA still verifies the credentials and succeeds")
    void testWithClientCertAndKey(@TempDir Path tempDir) throws Exception {
        Path certFile = tempDir.resolve("client.crt");
        Path keyFile = tempDir.resolve("client.key");
        Files.write(certFile, fixture("tls-cert.pem"));
        Files.write(keyFile, fixture("tls-key-sec1.pem"));

        KubernetesClient client = KubernetesClient.builder()
                .withClientCert(certFile.toString())
                .withClientKey(keyFile.toString())
                .build();
        assertNotNull(client);
    }

    @Test
    @DisplayName(".withClientCert(path).withClientKey(path) fails loudly when the key cannot be parsed, instead of silently dropping the credentials")
    void testWithClientCertAndKeyFailsLoudlyOnAnUnparsableKey(@TempDir Path tempDir) throws Exception {
        Path certFile = tempDir.resolve("client.crt");
        Path keyFile = tempDir.resolve("client.key");
        Files.write(certFile, fixture("tls-cert.pem"));
        Files.write(keyFile, "-----BEGIN EC PRIVATE KEY-----\nnot a key\n-----END EC PRIVATE KEY-----"
                .getBytes(StandardCharsets.UTF_8));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                KubernetesClient.builder()
                        .withClientCert(certFile.toString())
                        .withClientKey(keyFile.toString())
                        .build());
        assertTrue(thrown.getMessage().contains("TLS"));
    }

    private static byte[] fixture(String resource) throws Exception {
        try (InputStream in = KubernetesClientBuilderTest.class.getClassLoader().getResourceAsStream(resource)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName(".withTimeout(60000).build() works")
    void testWithTimeout() {
        KubernetesClient client = KubernetesClient.builder()
                .withTimeout(60000)
                .build();
        assertNotNull(client);
    }

    @Test
    @DisplayName("client.close() via assertDoesNotThrow")
    void testClose() {
        KubernetesClient client = KubernetesClient.builder().build();
        assertDoesNotThrow(client::close);
    }

    @Test
    @DisplayName("Command factory methods all return non-null")
    void testCommandFactoryMethods() {
        KubernetesClient client = KubernetesClient.builder().build();

        assertNotNull(client.listPods());
        assertNotNull(client.createPod("nginx"));
        assertNotNull(client.createPod(new Pod()));
        assertNotNull(client.deletePod("name"));
        assertNotNull(client.getPod("name"));
        assertNotNull(client.logs("name"));

        assertNotNull(client.listDeployments());
        assertNotNull(client.createDeployment("nginx"));
        assertNotNull(client.createDeployment(new Deployment()));
        assertNotNull(client.deleteDeployment("name"));
        assertNotNull(client.getDeployment("name"));
        assertNotNull(client.scaleDeployment("name"));

        assertNotNull(client.listServices());
        assertNotNull(client.createService());
        assertNotNull(client.createService(new Service()));
        assertNotNull(client.deleteService("name"));
        assertNotNull(client.getService("name"));

        assertNotNull(client.listNamespaces());
        assertNotNull(client.createNamespace());
        assertNotNull(client.deleteNamespace("name"));
        assertNotNull(client.getNamespace("name"));

        assertNotNull(client.listNodes());
        assertNotNull(client.getNode("name"));

        assertNotNull(client.listConfigMaps());
        assertNotNull(client.createConfigMap());
        assertNotNull(client.createConfigMap(new ConfigMap()));
        assertNotNull(client.deleteConfigMap("name"));
        assertNotNull(client.getConfigMap("name"));

        assertNotNull(client.listSecrets());
        assertNotNull(client.createSecret());
        assertNotNull(client.createSecret(new Secret()));
        assertNotNull(client.deleteSecret("name"));
        assertNotNull(client.getSecret("name"));

        assertNotNull(client.listJobs());
        assertNotNull(client.createJob(new Job()));
        assertNotNull(client.deleteJob("name"));
        assertNotNull(client.getJob("name"));

        assertNotNull(client.listCronJobs());
        assertNotNull(client.createCronJob(new CronJob()));
        assertNotNull(client.deleteCronJob("name"));
        assertNotNull(client.getCronJob("name"));

        assertNotNull(client.listStatefulSets());
        assertNotNull(client.createStatefulSet(new StatefulSet()));
        assertNotNull(client.deleteStatefulSet("name"));
        assertNotNull(client.getStatefulSet("name"));
        assertNotNull(client.scaleStatefulSet("name"));

        assertNotNull(client.listDaemonSets());
        assertNotNull(client.createDaemonSet(new DaemonSet()));
        assertNotNull(client.deleteDaemonSet("name"));
        assertNotNull(client.getDaemonSet("name"));

        assertNotNull(client.listReplicaSets());
        assertNotNull(client.getReplicaSet("name"));

        assertNotNull(client.listIngresses());
        assertNotNull(client.createIngress(new Ingress()));
        assertNotNull(client.deleteIngress("name"));
        assertNotNull(client.getIngress("name"));

        assertNotNull(client.listNetworkPolicies());
        assertNotNull(client.createNetworkPolicy(new NetworkPolicy()));
        assertNotNull(client.deleteNetworkPolicy("name"));
        assertNotNull(client.getNetworkPolicy("name"));

        assertNotNull(client.listPersistentVolumeClaims());
        assertNotNull(client.createPersistentVolumeClaim(new PersistentVolumeClaim()));
        assertNotNull(client.deletePersistentVolumeClaim("name"));
        assertNotNull(client.getPersistentVolumeClaim("name"));

        assertNotNull(client.listPersistentVolumes());
        assertNotNull(client.createPersistentVolume(new PersistentVolume()));
        assertNotNull(client.deletePersistentVolume("name"));
        assertNotNull(client.getPersistentVolume("name"));

        assertNotNull(client.listServiceAccounts());
        assertNotNull(client.createServiceAccount(new ServiceAccount()));
        assertNotNull(client.deleteServiceAccount("name"));
        assertNotNull(client.getServiceAccount("name"));

        assertNotNull(client.listEndpoints());
        assertNotNull(client.getEndpoints("name"));

        assertNotNull(client.listEvents());

        assertNotNull(client.listHorizontalPodAutoscalers());
        assertNotNull(client.createHorizontalPodAutoscaler(new HorizontalPodAutoscaler()));
        assertNotNull(client.deleteHorizontalPodAutoscaler("name"));
        assertNotNull(client.getHorizontalPodAutoscaler("name"));

        assertNotNull(client.listRoles());
        assertNotNull(client.createRole(new Role()));
        assertNotNull(client.deleteRole("name"));

        assertNotNull(client.listClusterRoles());
        assertNotNull(client.createClusterRole(new ClusterRole()));
        assertNotNull(client.deleteClusterRole("name"));

        assertNotNull(client.listRoleBindings());
        assertNotNull(client.createRoleBinding(new RoleBinding()));
        assertNotNull(client.deleteRoleBinding("name"));

        assertNotNull(client.listClusterRoleBindings());
        assertNotNull(client.createClusterRoleBinding(new ClusterRoleBinding()));
        assertNotNull(client.deleteClusterRoleBinding("name"));

        assertNotNull(client.listResourceQuotas());
        assertNotNull(client.createResourceQuota(new ResourceQuota()));
        assertNotNull(client.deleteResourceQuota("name"));

        assertNotNull(client.listLimitRanges());
        assertNotNull(client.createLimitRange(new LimitRange()));
        assertNotNull(client.deleteLimitRange("name"));

        assertNotNull(client.listStorageClasses());
        assertNotNull(client.getStorageClass("name"));

        assertNotNull(client.listPodDisruptionBudgets());
        assertNotNull(client.createPodDisruptionBudget(new PodDisruptionBudget()));
        assertNotNull(client.deletePodDisruptionBudget("name"));

        assertNotNull(client.healthz());
        assertNotNull(client.version());
    }
}
