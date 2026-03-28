package io.github.intisy.kubernetes;

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

        assertNotNull(client.healthz());
        assertNotNull(client.version());
    }
}
