package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.KubernetesClient;
import io.github.intisy.kubernetes.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName(".withCaCert(\"/path/to/ca.crt\").build() works")
    void testWithCaCert() {
        KubernetesClient client = KubernetesClient.builder()
                .withCaCert("/path/to/ca.crt")
                .build();
        assertNotNull(client);
    }

    @Test
    @DisplayName(".withClientCert(\"/path/to/client.crt\").withClientKey(\"/path/to/client.key\").build() works")
    void testWithClientCertAndKey() {
        KubernetesClient client = KubernetesClient.builder()
                .withClientCert("/path/to/client.crt")
                .withClientKey("/path/to/client.key")
                .build();
        assertNotNull(client);
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
