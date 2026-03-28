package io.github.intisy.kubernetes;

import io.github.intisy.kubernetes.command.configmap.*;
import io.github.intisy.kubernetes.command.deployment.*;
import io.github.intisy.kubernetes.command.namespace.*;
import io.github.intisy.kubernetes.command.node.*;
import io.github.intisy.kubernetes.command.pod.*;
import io.github.intisy.kubernetes.command.secret.*;
import io.github.intisy.kubernetes.command.service.*;
import io.github.intisy.kubernetes.command.system.*;
import io.github.intisy.kubernetes.model.*;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * Kubernetes client for communicating with the Kubernetes API server.
 * <p>
 * Example usage:
 * <pre>{@code
 * KubernetesClient client = KubernetesClient.builder()
 *     .withApiServer("https://localhost:8443")
 *     .withBearerToken("my-token")
 *     .build();
 *
 * // List pods
 * List<Pod> pods = client.listPods().withNamespace("default").exec();
 *
 * // Create and run a pod
 * Pod pod = client.createPod("nginx:alpine")
 *     .withName("my-nginx")
 *     .exec();
 *
 * // Clean up
 * client.deletePod("my-nginx").exec();
 * }</pre>
 *
 * @author Finn Birich
 */
public class KubernetesClient implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(KubernetesClient.class);

    private final KubernetesHttpClient httpClient;

    private KubernetesClient(KubernetesHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ==================== Pod Commands ====================

    public ListPodsCmd listPods() {
        return new ListPodsCmd(httpClient);
    }

    public CreatePodCmd createPod(String image) {
        return new CreatePodCmd(httpClient, image);
    }

    public CreatePodCmd createPod(Pod pod) {
        return new CreatePodCmd(httpClient, pod);
    }

    public DeletePodCmd deletePod(String podName) {
        return new DeletePodCmd(httpClient, podName);
    }

    public GetPodCmd getPod(String podName) {
        return new GetPodCmd(httpClient, podName);
    }

    public LogsPodCmd logs(String podName) {
        return new LogsPodCmd(httpClient, podName);
    }

    // ==================== Deployment Commands ====================

    public ListDeploymentsCmd listDeployments() {
        return new ListDeploymentsCmd(httpClient);
    }

    public CreateDeploymentCmd createDeployment(String image) {
        return new CreateDeploymentCmd(httpClient, image);
    }

    public CreateDeploymentCmd createDeployment(Deployment deployment) {
        return new CreateDeploymentCmd(httpClient, deployment);
    }

    public DeleteDeploymentCmd deleteDeployment(String deploymentName) {
        return new DeleteDeploymentCmd(httpClient, deploymentName);
    }

    public GetDeploymentCmd getDeployment(String deploymentName) {
        return new GetDeploymentCmd(httpClient, deploymentName);
    }

    public ScaleDeploymentCmd scaleDeployment(String deploymentName) {
        return new ScaleDeploymentCmd(httpClient, deploymentName);
    }

    // ==================== Service Commands ====================

    public ListServicesCmd listServices() {
        return new ListServicesCmd(httpClient);
    }

    public CreateServiceCmd createService() {
        return new CreateServiceCmd(httpClient);
    }

    public CreateServiceCmd createService(Service service) {
        return new CreateServiceCmd(httpClient, service);
    }

    public DeleteServiceCmd deleteService(String serviceName) {
        return new DeleteServiceCmd(httpClient, serviceName);
    }

    public GetServiceCmd getService(String serviceName) {
        return new GetServiceCmd(httpClient, serviceName);
    }

    // ==================== Namespace Commands ====================

    public ListNamespacesCmd listNamespaces() {
        return new ListNamespacesCmd(httpClient);
    }

    public CreateNamespaceCmd createNamespace() {
        return new CreateNamespaceCmd(httpClient);
    }

    public DeleteNamespaceCmd deleteNamespace(String namespaceName) {
        return new DeleteNamespaceCmd(httpClient, namespaceName);
    }

    // ==================== Node Commands ====================

    public ListNodesCmd listNodes() {
        return new ListNodesCmd(httpClient);
    }

    public GetNodeCmd getNode(String nodeName) {
        return new GetNodeCmd(httpClient, nodeName);
    }

    // ==================== ConfigMap Commands ====================

    public ListConfigMapsCmd listConfigMaps() {
        return new ListConfigMapsCmd(httpClient);
    }

    public CreateConfigMapCmd createConfigMap() {
        return new CreateConfigMapCmd(httpClient);
    }

    public CreateConfigMapCmd createConfigMap(ConfigMap configMap) {
        return new CreateConfigMapCmd(httpClient, configMap);
    }

    public DeleteConfigMapCmd deleteConfigMap(String configMapName) {
        return new DeleteConfigMapCmd(httpClient, configMapName);
    }

    public GetConfigMapCmd getConfigMap(String configMapName) {
        return new GetConfigMapCmd(httpClient, configMapName);
    }

    // ==================== Secret Commands ====================

    public ListSecretsCmd listSecrets() {
        return new ListSecretsCmd(httpClient);
    }

    public CreateSecretCmd createSecret() {
        return new CreateSecretCmd(httpClient);
    }

    public CreateSecretCmd createSecret(Secret secret) {
        return new CreateSecretCmd(httpClient, secret);
    }

    public DeleteSecretCmd deleteSecret(String secretName) {
        return new DeleteSecretCmd(httpClient, secretName);
    }

    // ==================== System Commands ====================

    public HealthzCmd healthz() {
        return new HealthzCmd(httpClient);
    }

    public VersionCmd version() {
        return new VersionCmd(httpClient);
    }

    @Override
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    public static class Builder {
        private String apiServerUrl;
        private String bearerToken;
        private String caCertPath;
        private String clientCertPath;
        private String clientKeyPath;
        private int timeout = 30000;

        private Builder() {
        }

        public Builder withApiServer(String apiServerUrl) {
            this.apiServerUrl = apiServerUrl;
            return this;
        }

        public Builder withBearerToken(String bearerToken) {
            this.bearerToken = bearerToken;
            return this;
        }

        public Builder withCaCert(String caCertPath) {
            this.caCertPath = caCertPath;
            return this;
        }

        public Builder withClientCert(String clientCertPath) {
            this.clientCertPath = clientCertPath;
            return this;
        }

        public Builder withClientKey(String clientKeyPath) {
            this.clientKeyPath = clientKeyPath;
            return this;
        }

        public Builder withTimeout(int timeoutMs) {
            this.timeout = timeoutMs;
            return this;
        }

        public KubernetesClient build() {
            if (apiServerUrl == null) {
                apiServerUrl = "https://localhost:8443";
            }
            log.debug("Building KubernetesClient for server: {}", apiServerUrl);

            KubernetesHttpClient httpClient;
            if (bearerToken != null) {
                httpClient = new KubernetesHttpClient(apiServerUrl, bearerToken, caCertPath, timeout);
            } else if (clientCertPath != null && clientKeyPath != null) {
                httpClient = new KubernetesHttpClient(apiServerUrl, caCertPath, clientCertPath, clientKeyPath, timeout);
            } else {
                httpClient = new KubernetesHttpClient(apiServerUrl, timeout);
            }
            return new KubernetesClient(httpClient);
        }
    }
}
