package io.github.intisy.kubernetes;

import io.github.intisy.kubernetes.command.clusterrole.*;
import io.github.intisy.kubernetes.command.clusterrolebinding.*;
import io.github.intisy.kubernetes.command.configmap.*;
import io.github.intisy.kubernetes.command.cronjob.*;
import io.github.intisy.kubernetes.command.daemonset.*;
import io.github.intisy.kubernetes.command.deployment.*;
import io.github.intisy.kubernetes.command.endpoints.*;
import io.github.intisy.kubernetes.command.event.*;
import io.github.intisy.kubernetes.command.hpa.*;
import io.github.intisy.kubernetes.command.ingress.*;
import io.github.intisy.kubernetes.command.job.*;
import io.github.intisy.kubernetes.command.limitrange.*;
import io.github.intisy.kubernetes.command.namespace.*;
import io.github.intisy.kubernetes.command.networkpolicy.*;
import io.github.intisy.kubernetes.command.node.*;
import io.github.intisy.kubernetes.command.pdb.*;
import io.github.intisy.kubernetes.command.pod.*;
import io.github.intisy.kubernetes.command.pv.*;
import io.github.intisy.kubernetes.command.pvc.*;
import io.github.intisy.kubernetes.command.replicaset.*;
import io.github.intisy.kubernetes.command.resourcequota.*;
import io.github.intisy.kubernetes.command.role.*;
import io.github.intisy.kubernetes.command.rolebinding.*;
import io.github.intisy.kubernetes.command.secret.*;
import io.github.intisy.kubernetes.command.service.*;
import io.github.intisy.kubernetes.command.serviceaccount.*;
import io.github.intisy.kubernetes.command.statefulset.*;
import io.github.intisy.kubernetes.command.storageclass.*;
import io.github.intisy.kubernetes.command.system.*;
import io.github.intisy.kubernetes.apply.ManifestApply;
import io.github.intisy.kubernetes.apply.ResourceResolver;
import io.github.intisy.kubernetes.config.KubeConfig;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.exec.ExecFailedException;
import io.github.intisy.kubernetes.exec.ExecResult;
import io.github.intisy.kubernetes.exec.PodExec;
import io.github.intisy.kubernetes.model.*;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;
import io.github.intisy.kubernetes.wait.Waiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Predicate;

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
    private final ManifestApply manifestApply;
    private final ResourceResolver resourceResolver;

    private KubernetesClient(KubernetesHttpClient httpClient) {
        this.httpClient = httpClient;
        this.resourceResolver = new ResourceResolver(httpClient);
        this.manifestApply = new ManifestApply(httpClient, resourceResolver);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static KubernetesClient fromKubeConfig(KubeConfig config) {
        return builder()
                .withApiServer(config.server())
                .withCaCertPem(config.caCertPem())
                .withClientCertPem(config.clientCertPem())
                .withClientKeyPem(config.clientKeyPem())
                .build();
    }


    public void applyYaml(String yaml) throws IOException {
        manifestApply.applyYaml(yaml);
    }

    /**
     * Polls the resource's status document every 2 seconds until the predicate holds or
     * {@code timeoutMillis} passes, the replacement for {@code kubectl wait} and
     * {@code kubectl rollout status}. Use {@link io.github.intisy.kubernetes.wait.Conditions#isReady}
     * for a Pod or a CloudNativePG {@code Cluster}, and {@link io.github.intisy.kubernetes.wait.Conditions#isRolloutComplete}
     * for a Deployment, DaemonSet or StatefulSet.
     */
    public void waitFor(final String apiVersion, final String kind, final String namespace, final String name,
                         Predicate<String> predicate, long timeoutMillis) throws IOException {
        final String path = resourcePath(apiVersion, kind, namespace, name);
        Waiter waiter = new Waiter(new Waiter.Fetcher() {
            @Override
            public String fetch() throws IOException {
                return fetchAt(path, kind, name);
            }
        });
        waiter.waitFor(kind, namespace, name, predicate, timeoutMillis);
    }

    /**
     * Reads a resource's current document once, the replacement for {@code kubectl get -o json}.
     * Use it to report on a resource; use {@link #waitFor} to block until one becomes ready.
     *
     * @throws NotFoundException if the resource does not exist
     * @throws KubernetesException if the read fails for any other reason
     */
    public String fetchResource(String apiVersion, String kind, String namespace, String name) {
        try {
            return fetchAt(resourcePath(apiVersion, kind, namespace, name), kind, name);
        } catch (Waiter.FetchFailedException e) {
            if (e.getStatusCode() == 404) {
                throw new NotFoundException(e.getMessage(), e);
            }
            throw new KubernetesException(e.getMessage(), e.getStatusCode(), e);
        } catch (IOException e) {
            throw new KubernetesException("failed to fetch " + kind + " " + name, e);
        }
    }

    private String resourcePath(String apiVersion, String kind, String namespace, String name) throws IOException {
        return ManifestApply.resourcePath(apiVersion, resourceResolver.resolvePlural(apiVersion, kind), namespace, name);
    }

    /**
     * @implNote signals a failed read as {@link Waiter.FetchFailedException} rather than a plain
     * {@link IOException} because it carries the HTTP status code, which is what lets {@link Waiter}
     * retry a transient server error but fail fast on a 401 or 403, and what lets
     * {@link #fetchResource} recognise a 404.
     */
    private String fetchAt(String path, String kind, String name) throws IOException {
        KubernetesResponse response = httpClient.get(path);
        if (!response.isSuccessful()) {
            throw new Waiter.FetchFailedException("failed to fetch " + kind + " " + name + " at " + path
                    + ": HTTP " + response.getStatusCode() + ": " + response.getBody(), response.getStatusCode());
        }
        return response.getBody();
    }

    /**
     * Runs {@code command} in {@code pod}, the replacement for {@code kubectl exec}, waiting up
     * to {@link PodExec#DEFAULT_SESSION_TIMEOUT_MILLIS} for it to finish.
     *
     * @implNote Throws on a non-zero exit rather than returning it silently: the consuming CLI
     * uses this to read files, run {@code psql}, and check metadata rows, so a failed command
     * must surface as a failure here just as it did as a failed {@code kubectl exec} process.
     */
    public ExecResult exec(String namespace, String pod, String[] command) throws IOException {
        return exec(namespace, pod, command, PodExec.DEFAULT_SESSION_TIMEOUT_MILLIS);
    }

    public ExecResult exec(String namespace, String pod, String[] command, int sessionTimeoutMillis) throws IOException {
        PodExec podExec = new PodExec(httpClient.getApiServerUrl(), httpClient.getSslSocketFactory(),
                httpClient.isVerifyHostname(), httpClient.getTimeout(), sessionTimeoutMillis);
        ExecResult result = podExec.exec(namespace, pod, command);
        if (result.exitCode() != 0) {
            throw new ExecFailedException("command " + Arrays.toString(command) + " in pod " + namespace + "/" + pod
                    + " exited with code " + result.exitCode()
                    + "; stdout: " + result.stdout() + "; stderr: " + result.stderr(),
                    result.exitCode());
        }
        return result;
    }


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


    public ListNamespacesCmd listNamespaces() {
        return new ListNamespacesCmd(httpClient);
    }

    public CreateNamespaceCmd createNamespace() {
        return new CreateNamespaceCmd(httpClient);
    }

    public DeleteNamespaceCmd deleteNamespace(String namespaceName) {
        return new DeleteNamespaceCmd(httpClient, namespaceName);
    }

    public GetNamespaceCmd getNamespace(String namespaceName) {
        return new GetNamespaceCmd(httpClient, namespaceName);
    }


    public ListNodesCmd listNodes() {
        return new ListNodesCmd(httpClient);
    }

    public GetNodeCmd getNode(String nodeName) {
        return new GetNodeCmd(httpClient, nodeName);
    }


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

    public GetSecretCmd getSecret(String secretName) {
        return new GetSecretCmd(httpClient, secretName);
    }


    public ListJobsCmd listJobs() {
        return new ListJobsCmd(httpClient);
    }

    public CreateJobCmd createJob(Job job) {
        return new CreateJobCmd(httpClient, job);
    }

    public DeleteJobCmd deleteJob(String jobName) {
        return new DeleteJobCmd(httpClient, jobName);
    }

    public GetJobCmd getJob(String jobName) {
        return new GetJobCmd(httpClient, jobName);
    }


    public ListCronJobsCmd listCronJobs() {
        return new ListCronJobsCmd(httpClient);
    }

    public CreateCronJobCmd createCronJob(CronJob cronJob) {
        return new CreateCronJobCmd(httpClient, cronJob);
    }

    public DeleteCronJobCmd deleteCronJob(String cronJobName) {
        return new DeleteCronJobCmd(httpClient, cronJobName);
    }

    public GetCronJobCmd getCronJob(String cronJobName) {
        return new GetCronJobCmd(httpClient, cronJobName);
    }


    public ListStatefulSetsCmd listStatefulSets() {
        return new ListStatefulSetsCmd(httpClient);
    }

    public CreateStatefulSetCmd createStatefulSet(StatefulSet statefulSet) {
        return new CreateStatefulSetCmd(httpClient, statefulSet);
    }

    public DeleteStatefulSetCmd deleteStatefulSet(String statefulSetName) {
        return new DeleteStatefulSetCmd(httpClient, statefulSetName);
    }

    public GetStatefulSetCmd getStatefulSet(String statefulSetName) {
        return new GetStatefulSetCmd(httpClient, statefulSetName);
    }

    public ScaleStatefulSetCmd scaleStatefulSet(String statefulSetName) {
        return new ScaleStatefulSetCmd(httpClient, statefulSetName);
    }


    public ListDaemonSetsCmd listDaemonSets() {
        return new ListDaemonSetsCmd(httpClient);
    }

    public CreateDaemonSetCmd createDaemonSet(DaemonSet daemonSet) {
        return new CreateDaemonSetCmd(httpClient, daemonSet);
    }

    public DeleteDaemonSetCmd deleteDaemonSet(String daemonSetName) {
        return new DeleteDaemonSetCmd(httpClient, daemonSetName);
    }

    public GetDaemonSetCmd getDaemonSet(String daemonSetName) {
        return new GetDaemonSetCmd(httpClient, daemonSetName);
    }


    public ListReplicaSetsCmd listReplicaSets() {
        return new ListReplicaSetsCmd(httpClient);
    }

    public GetReplicaSetCmd getReplicaSet(String replicaSetName) {
        return new GetReplicaSetCmd(httpClient, replicaSetName);
    }


    public ListIngressesCmd listIngresses() {
        return new ListIngressesCmd(httpClient);
    }

    public CreateIngressCmd createIngress(Ingress ingress) {
        return new CreateIngressCmd(httpClient, ingress);
    }

    public DeleteIngressCmd deleteIngress(String ingressName) {
        return new DeleteIngressCmd(httpClient, ingressName);
    }

    public GetIngressCmd getIngress(String ingressName) {
        return new GetIngressCmd(httpClient, ingressName);
    }


    public ListNetworkPoliciesCmd listNetworkPolicies() {
        return new ListNetworkPoliciesCmd(httpClient);
    }

    public CreateNetworkPolicyCmd createNetworkPolicy(NetworkPolicy networkPolicy) {
        return new CreateNetworkPolicyCmd(httpClient, networkPolicy);
    }

    public DeleteNetworkPolicyCmd deleteNetworkPolicy(String networkPolicyName) {
        return new DeleteNetworkPolicyCmd(httpClient, networkPolicyName);
    }

    public GetNetworkPolicyCmd getNetworkPolicy(String networkPolicyName) {
        return new GetNetworkPolicyCmd(httpClient, networkPolicyName);
    }


    public ListPersistentVolumeClaimsCmd listPersistentVolumeClaims() {
        return new ListPersistentVolumeClaimsCmd(httpClient);
    }

    public CreatePersistentVolumeClaimCmd createPersistentVolumeClaim(PersistentVolumeClaim pvc) {
        return new CreatePersistentVolumeClaimCmd(httpClient, pvc);
    }

    public DeletePersistentVolumeClaimCmd deletePersistentVolumeClaim(String pvcName) {
        return new DeletePersistentVolumeClaimCmd(httpClient, pvcName);
    }

    public GetPersistentVolumeClaimCmd getPersistentVolumeClaim(String pvcName) {
        return new GetPersistentVolumeClaimCmd(httpClient, pvcName);
    }


    public ListPersistentVolumesCmd listPersistentVolumes() {
        return new ListPersistentVolumesCmd(httpClient);
    }

    public CreatePersistentVolumeCmd createPersistentVolume(PersistentVolume pv) {
        return new CreatePersistentVolumeCmd(httpClient, pv);
    }

    public DeletePersistentVolumeCmd deletePersistentVolume(String pvName) {
        return new DeletePersistentVolumeCmd(httpClient, pvName);
    }

    public GetPersistentVolumeCmd getPersistentVolume(String pvName) {
        return new GetPersistentVolumeCmd(httpClient, pvName);
    }


    public ListServiceAccountsCmd listServiceAccounts() {
        return new ListServiceAccountsCmd(httpClient);
    }

    public CreateServiceAccountCmd createServiceAccount(ServiceAccount serviceAccount) {
        return new CreateServiceAccountCmd(httpClient, serviceAccount);
    }

    public DeleteServiceAccountCmd deleteServiceAccount(String serviceAccountName) {
        return new DeleteServiceAccountCmd(httpClient, serviceAccountName);
    }

    public GetServiceAccountCmd getServiceAccount(String serviceAccountName) {
        return new GetServiceAccountCmd(httpClient, serviceAccountName);
    }


    public ListEndpointsCmd listEndpoints() {
        return new ListEndpointsCmd(httpClient);
    }

    public GetEndpointsCmd getEndpoints(String endpointsName) {
        return new GetEndpointsCmd(httpClient, endpointsName);
    }


    public ListEventsCmd listEvents() {
        return new ListEventsCmd(httpClient);
    }


    public ListHorizontalPodAutoscalersCmd listHorizontalPodAutoscalers() {
        return new ListHorizontalPodAutoscalersCmd(httpClient);
    }

    public CreateHorizontalPodAutoscalerCmd createHorizontalPodAutoscaler(HorizontalPodAutoscaler hpa) {
        return new CreateHorizontalPodAutoscalerCmd(httpClient, hpa);
    }

    public DeleteHorizontalPodAutoscalerCmd deleteHorizontalPodAutoscaler(String hpaName) {
        return new DeleteHorizontalPodAutoscalerCmd(httpClient, hpaName);
    }

    public GetHorizontalPodAutoscalerCmd getHorizontalPodAutoscaler(String hpaName) {
        return new GetHorizontalPodAutoscalerCmd(httpClient, hpaName);
    }


    public ListRolesCmd listRoles() {
        return new ListRolesCmd(httpClient);
    }

    public CreateRoleCmd createRole(Role role) {
        return new CreateRoleCmd(httpClient, role);
    }

    public DeleteRoleCmd deleteRole(String roleName) {
        return new DeleteRoleCmd(httpClient, roleName);
    }


    public ListClusterRolesCmd listClusterRoles() {
        return new ListClusterRolesCmd(httpClient);
    }

    public CreateClusterRoleCmd createClusterRole(ClusterRole clusterRole) {
        return new CreateClusterRoleCmd(httpClient, clusterRole);
    }

    public DeleteClusterRoleCmd deleteClusterRole(String clusterRoleName) {
        return new DeleteClusterRoleCmd(httpClient, clusterRoleName);
    }


    public ListRoleBindingsCmd listRoleBindings() {
        return new ListRoleBindingsCmd(httpClient);
    }

    public CreateRoleBindingCmd createRoleBinding(RoleBinding roleBinding) {
        return new CreateRoleBindingCmd(httpClient, roleBinding);
    }

    public DeleteRoleBindingCmd deleteRoleBinding(String roleBindingName) {
        return new DeleteRoleBindingCmd(httpClient, roleBindingName);
    }


    public ListClusterRoleBindingsCmd listClusterRoleBindings() {
        return new ListClusterRoleBindingsCmd(httpClient);
    }

    public CreateClusterRoleBindingCmd createClusterRoleBinding(ClusterRoleBinding clusterRoleBinding) {
        return new CreateClusterRoleBindingCmd(httpClient, clusterRoleBinding);
    }

    public DeleteClusterRoleBindingCmd deleteClusterRoleBinding(String clusterRoleBindingName) {
        return new DeleteClusterRoleBindingCmd(httpClient, clusterRoleBindingName);
    }


    public ListResourceQuotasCmd listResourceQuotas() {
        return new ListResourceQuotasCmd(httpClient);
    }

    public CreateResourceQuotaCmd createResourceQuota(ResourceQuota resourceQuota) {
        return new CreateResourceQuotaCmd(httpClient, resourceQuota);
    }

    public DeleteResourceQuotaCmd deleteResourceQuota(String resourceQuotaName) {
        return new DeleteResourceQuotaCmd(httpClient, resourceQuotaName);
    }


    public ListLimitRangesCmd listLimitRanges() {
        return new ListLimitRangesCmd(httpClient);
    }

    public CreateLimitRangeCmd createLimitRange(LimitRange limitRange) {
        return new CreateLimitRangeCmd(httpClient, limitRange);
    }

    public DeleteLimitRangeCmd deleteLimitRange(String limitRangeName) {
        return new DeleteLimitRangeCmd(httpClient, limitRangeName);
    }


    public ListStorageClassesCmd listStorageClasses() {
        return new ListStorageClassesCmd(httpClient);
    }

    public GetStorageClassCmd getStorageClass(String storageClassName) {
        return new GetStorageClassCmd(httpClient, storageClassName);
    }


    public ListPodDisruptionBudgetsCmd listPodDisruptionBudgets() {
        return new ListPodDisruptionBudgetsCmd(httpClient);
    }

    public CreatePodDisruptionBudgetCmd createPodDisruptionBudget(PodDisruptionBudget pdb) {
        return new CreatePodDisruptionBudgetCmd(httpClient, pdb);
    }

    public DeletePodDisruptionBudgetCmd deletePodDisruptionBudget(String pdbName) {
        return new DeletePodDisruptionBudgetCmd(httpClient, pdbName);
    }


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
        private String caCertPem;
        private String clientCertPem;
        private String clientKeyPem;
        private boolean insecureTrustAll;
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

        public Builder withCaCertPem(String caCertPem) {
            this.caCertPem = caCertPem;
            return this;
        }

        public Builder withClientCertPem(String clientCertPem) {
            this.clientCertPem = clientCertPem;
            return this;
        }

        public Builder withClientKeyPem(String clientKeyPem) {
            this.clientKeyPem = clientKeyPem;
            return this;
        }

        public Builder withInsecureTrustAll(boolean insecureTrustAll) {
            this.insecureTrustAll = insecureTrustAll;
            return this;
        }

        public Builder withTimeout(int timeoutMs) {
            this.timeout = timeoutMs;
            return this;
        }

        /**
         * @implNote Paths are read into PEM content here and every combination is routed through
         * the PEM-taking constructors, rather than branching between path-based and PEM-based
         * constructors. A caller supplying only a CA (PEM or path) must reach real TLS
         * verification, not the insecure fallback; keeping one code path here is what makes that
         * (and {@code withInsecureTrustAll}) apply uniformly regardless of whether the caller
         * used the path-based or PEM-based builder methods. A client cert and key with no CA is
         * rejected rather than falling back to the JVM's system trust store: a Kubernetes
         * cluster's CA is essentially never in that store, so that fallback would only defer the
         * same failure to first request, as a bare PKIX error with no hint that a CA or
         * {@code withInsecureTrustAll} exists.
         */
        public KubernetesClient build() {
            if (apiServerUrl == null) {
                apiServerUrl = "https://localhost:8443";
            }
            log.debug("Building KubernetesClient for server: {}", apiServerUrl);

            String effectiveCaCertPem = caCertPem != null ? caCertPem : readPemFile(caCertPath);
            String effectiveClientCertPem = clientCertPem != null ? clientCertPem : readPemFile(clientCertPath);
            String effectiveClientKeyPem = clientKeyPem != null ? clientKeyPem : readPemFile(clientKeyPath);

            if ((effectiveClientCertPem == null) != (effectiveClientKeyPem == null)) {
                throw new IllegalStateException("client certificate and client key must both be supplied, or neither");
            }
            if (effectiveClientCertPem != null && effectiveCaCertPem == null && !insecureTrustAll) {
                throw new IllegalStateException("client certificate supplied without a CA certificate; the server cannot be verified, "
                        + "pass withCaCertPem or withCaCert to verify against your cluster's CA, "
                        + "or withInsecureTrustAll(true) to accept an unverified connection deliberately");
            }

            KubernetesHttpClient httpClient;
            if (bearerToken != null) {
                httpClient = new KubernetesHttpClient(apiServerUrl, bearerToken, effectiveCaCertPem, timeout, insecureTrustAll);
            } else {
                httpClient = new KubernetesHttpClient(apiServerUrl, effectiveCaCertPem, effectiveClientCertPem, effectiveClientKeyPem, timeout, insecureTrustAll);
            }
            return new KubernetesClient(httpClient);
        }

        private static String readPemFile(String path) {
            if (path == null) {
                return null;
            }
            try {
                return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("failed to read PEM file at " + path, e);
            }
        }
    }
}
