package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
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
        ObjectMeta.OwnerReference ref = new ObjectMeta.OwnerReference();
        assertNull(ref.getApiVersion());
        assertNull(ref.getKind());
        assertNull(ref.getName());
        assertNull(ref.getUid());
        assertNull(ref.getController());
        assertNull(ref.getBlockOwnerDeletion());
    }

    @Test
    @DisplayName("Job: default values and spec")
    public void testJob() {
        Job job = new Job();
        assertEquals("batch/v1", job.getApiVersion());
        assertEquals("Job", job.getKind());
        assertNull(job.getMetadata());
        assertNull(job.getSpec());
        assertNull(job.getStatus());

        Job.JobSpec spec = new Job.JobSpec()
                .setParallelism(2)
                .setCompletions(5)
                .setBackoffLimit(3)
                .setActiveDeadlineSeconds(600L)
                .setTtlSecondsAfterFinished(120);

        job.setSpec(spec);
        job.setMetadata(new ObjectMeta().setName("my-job"));

        assertEquals(2, job.getSpec().getParallelism());
        assertEquals(5, job.getSpec().getCompletions());
        assertEquals(3, job.getSpec().getBackoffLimit());
        assertEquals(600L, job.getSpec().getActiveDeadlineSeconds());
        assertEquals(120, job.getSpec().getTtlSecondsAfterFinished());
        assertTrue(job.toString().contains("Job"));
    }

    @Test
    @DisplayName("CronJob: default values and spec")
    public void testCronJob() {
        CronJob cronJob = new CronJob();
        assertEquals("batch/v1", cronJob.getApiVersion());
        assertEquals("CronJob", cronJob.getKind());

        CronJob.CronJobSpec spec = new CronJob.CronJobSpec()
                .setSchedule("*/5 * * * *")
                .setConcurrencyPolicy("Forbid")
                .setSuspend(false)
                .setSuccessfulJobsHistoryLimit(3)
                .setFailedJobsHistoryLimit(1)
                .setStartingDeadlineSeconds(100L);

        cronJob.setSpec(spec);
        cronJob.setMetadata(new ObjectMeta().setName("my-cronjob"));

        assertEquals("*/5 * * * *", cronJob.getSpec().getSchedule());
        assertEquals("Forbid", cronJob.getSpec().getConcurrencyPolicy());
        assertFalse(cronJob.getSpec().getSuspend());
        assertEquals(3, cronJob.getSpec().getSuccessfulJobsHistoryLimit());
        assertEquals(1, cronJob.getSpec().getFailedJobsHistoryLimit());
        assertEquals(100L, cronJob.getSpec().getStartingDeadlineSeconds());
        assertTrue(cronJob.toString().contains("CronJob"));
    }

    @Test
    @DisplayName("StatefulSet: default values and spec")
    public void testStatefulSet() {
        StatefulSet ss = new StatefulSet();
        assertEquals("apps/v1", ss.getApiVersion());
        assertEquals("StatefulSet", ss.getKind());

        StatefulSet.StatefulSetSpec spec = new StatefulSet.StatefulSetSpec()
                .setReplicas(3)
                .setServiceName("my-service")
                .setPodManagementPolicy("OrderedReady")
                .setRevisionHistoryLimit(10);

        ss.setSpec(spec);
        ss.setMetadata(new ObjectMeta().setName("my-statefulset"));

        assertEquals(3, ss.getSpec().getReplicas());
        assertEquals("my-service", ss.getSpec().getServiceName());
        assertEquals("OrderedReady", ss.getSpec().getPodManagementPolicy());
        assertEquals(10, ss.getSpec().getRevisionHistoryLimit());
        assertTrue(ss.toString().contains("StatefulSet"));
    }

    @Test
    @DisplayName("StatefulSet: update strategy")
    public void testStatefulSetUpdateStrategy() {
        StatefulSet.RollingUpdateStatefulSetStrategy rolling =
                new StatefulSet.RollingUpdateStatefulSetStrategy().setPartition(2);

        StatefulSet.UpdateStrategy strategy = new StatefulSet.UpdateStrategy()
                .setType("RollingUpdate")
                .setRollingUpdate(rolling);

        assertEquals("RollingUpdate", strategy.getType());
        assertEquals(2, strategy.getRollingUpdate().getPartition());
    }

    @Test
    @DisplayName("DaemonSet: default values and spec")
    public void testDaemonSet() {
        DaemonSet ds = new DaemonSet();
        assertEquals("apps/v1", ds.getApiVersion());
        assertEquals("DaemonSet", ds.getKind());

        DaemonSet.DaemonSetSpec spec = new DaemonSet.DaemonSetSpec()
                .setMinReadySeconds(10)
                .setRevisionHistoryLimit(5);

        ds.setSpec(spec);
        ds.setMetadata(new ObjectMeta().setName("my-daemonset"));

        assertEquals(10, ds.getSpec().getMinReadySeconds());
        assertEquals(5, ds.getSpec().getRevisionHistoryLimit());
        assertTrue(ds.toString().contains("DaemonSet"));
    }

    @Test
    @DisplayName("ReplicaSet: default values")
    public void testReplicaSet() {
        ReplicaSet rs = new ReplicaSet();
        assertEquals("apps/v1", rs.getApiVersion());
        assertEquals("ReplicaSet", rs.getKind());
        rs.setMetadata(new ObjectMeta().setName("my-replicaset"));
        assertEquals("my-replicaset", rs.getMetadata().getName());
        assertTrue(rs.toString().contains("ReplicaSet"));
    }

    @Test
    @DisplayName("Ingress: default values and spec")
    public void testIngress() {
        Ingress ingress = new Ingress();
        assertEquals("networking.k8s.io/v1", ingress.getApiVersion());
        assertEquals("Ingress", ingress.getKind());

        Ingress.IngressSpec spec = new Ingress.IngressSpec()
                .setIngressClassName("nginx")
                .addRule(new Ingress.IngressRule()
                        .setHost("example.com")
                        .setHttp(new Ingress.HTTPIngressRuleValue()
                                .addPath(new Ingress.HTTPIngressPath()
                                        .setPath("/")
                                        .setPathType("Prefix")
                                        .setBackend(new Ingress.IngressBackend()
                                                .setService(new Ingress.IngressServiceBackend()
                                                        .setName("my-svc")
                                                        .setPort(new Ingress.ServiceBackendPort().setNumber(80))
                                                )
                                        )
                                )
                        )
                );

        ingress.setSpec(spec);
        ingress.setMetadata(new ObjectMeta().setName("my-ingress"));

        assertEquals("nginx", ingress.getSpec().getIngressClassName());
        assertEquals(1, ingress.getSpec().getRules().size());
        assertEquals("example.com", ingress.getSpec().getRules().get(0).getHost());
        assertEquals("/", ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPath());
        assertEquals("my-svc", ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getBackend().getService().getName());
        assertEquals(80, ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getBackend().getService().getPort().getNumber());
        assertTrue(ingress.toString().contains("rulesCount=1"));
    }

    @Test
    @DisplayName("NetworkPolicy: default values and spec")
    public void testNetworkPolicy() {
        NetworkPolicy np = new NetworkPolicy();
        assertEquals("networking.k8s.io/v1", np.getApiVersion());
        assertEquals("NetworkPolicy", np.getKind());

        NetworkPolicy.NetworkPolicySpec spec = new NetworkPolicy.NetworkPolicySpec()
                .setPodSelector(new Deployment.LabelSelector().addMatchLabel("app", "web"))
                .setPolicyTypes(Arrays.asList("Ingress", "Egress"));

        np.setSpec(spec);
        np.setMetadata(new ObjectMeta().setName("my-netpol"));

        assertEquals("web", np.getSpec().getPodSelector().getMatchLabels().get("app"));
        assertEquals(2, np.getSpec().getPolicyTypes().size());
        assertTrue(np.toString().contains("NetworkPolicy"));
    }

    @Test
    @DisplayName("PersistentVolumeClaim: default values and spec")
    public void testPersistentVolumeClaim() {
        PersistentVolumeClaim pvc = new PersistentVolumeClaim();
        assertEquals("v1", pvc.getApiVersion());
        assertEquals("PersistentVolumeClaim", pvc.getKind());

        PersistentVolumeClaim.PVCSpec spec = new PersistentVolumeClaim.PVCSpec()
                .setAccessModes(Collections.singletonList("ReadWriteOnce"))
                .setStorageClassName("standard")
                .setVolumeName("my-vol")
                .setVolumeMode("Filesystem");

        PersistentVolumeClaim.ResourceRequirements resources = new PersistentVolumeClaim.ResourceRequirements()
                .setRequests(Collections.singletonMap("storage", "10Gi"));
        spec.setResources(resources);

        pvc.setSpec(spec);
        pvc.setMetadata(new ObjectMeta().setName("my-pvc"));

        assertEquals(1, pvc.getSpec().getAccessModes().size());
        assertEquals("ReadWriteOnce", pvc.getSpec().getAccessModes().get(0));
        assertEquals("standard", pvc.getSpec().getStorageClassName());
        assertEquals("10Gi", pvc.getSpec().getResources().getRequests().get("storage"));
        assertTrue(pvc.toString().contains("PersistentVolumeClaim"));
    }

    @Test
    @DisplayName("PersistentVolume: default values")
    public void testPersistentVolume() {
        PersistentVolume pv = new PersistentVolume();
        assertEquals("v1", pv.getApiVersion());
        assertEquals("PersistentVolume", pv.getKind());
        pv.setMetadata(new ObjectMeta().setName("my-pv"));
        assertEquals("my-pv", pv.getMetadata().getName());
        assertTrue(pv.toString().contains("PersistentVolume"));
    }

    @Test
    @DisplayName("ServiceAccount: default values")
    public void testServiceAccount() {
        ServiceAccount sa = new ServiceAccount();
        assertEquals("v1", sa.getApiVersion());
        assertEquals("ServiceAccount", sa.getKind());
        sa.setMetadata(new ObjectMeta().setName("my-sa"));
        assertEquals("my-sa", sa.getMetadata().getName());
        assertTrue(sa.toString().contains("ServiceAccount"));
    }

    @Test
    @DisplayName("Endpoints: default values")
    public void testEndpoints() {
        Endpoints ep = new Endpoints();
        assertEquals("v1", ep.getApiVersion());
        assertEquals("Endpoints", ep.getKind());
        ep.setMetadata(new ObjectMeta().setName("my-ep"));
        assertEquals("my-ep", ep.getMetadata().getName());
        assertTrue(ep.toString().contains("Endpoints"));
    }

    @Test
    @DisplayName("HorizontalPodAutoscaler: default values and spec")
    public void testHorizontalPodAutoscaler() {
        HorizontalPodAutoscaler hpa = new HorizontalPodAutoscaler();
        assertEquals("autoscaling/v1", hpa.getApiVersion());
        assertEquals("HorizontalPodAutoscaler", hpa.getKind());

        HorizontalPodAutoscaler.CrossVersionObjectReference ref =
                new HorizontalPodAutoscaler.CrossVersionObjectReference()
                        .setApiVersion("apps/v1")
                        .setKind("Deployment")
                        .setName("my-deploy");

        HorizontalPodAutoscaler.HPASpec spec = new HorizontalPodAutoscaler.HPASpec()
                .setScaleTargetRef(ref)
                .setMinReplicas(1)
                .setMaxReplicas(10)
                .setTargetCPUUtilizationPercentage(80);

        hpa.setSpec(spec);
        hpa.setMetadata(new ObjectMeta().setName("my-hpa"));

        assertEquals("apps/v1", hpa.getSpec().getScaleTargetRef().getApiVersion());
        assertEquals(1, hpa.getSpec().getMinReplicas());
        assertEquals(10, hpa.getSpec().getMaxReplicas());
        assertEquals(80, hpa.getSpec().getTargetCPUUtilizationPercentage());
    }

    @Test
    @DisplayName("Role: default values and rules")
    public void testRole() {
        Role role = new Role();
        assertEquals("rbac.authorization.k8s.io/v1", role.getApiVersion());
        assertEquals("Role", role.getKind());

        Role.PolicyRule rule = new Role.PolicyRule()
                .setApiGroups(Collections.singletonList(""))
                .setResources(Arrays.asList("pods", "services"))
                .setVerbs(Arrays.asList("get", "list", "watch"));

        role.addRule(rule);
        role.setMetadata(new ObjectMeta().setName("my-role"));

        assertEquals(1, role.getRules().size());
        assertEquals(2, role.getRules().get(0).getResources().size());
        assertEquals(3, role.getRules().get(0).getVerbs().size());
        assertTrue(role.toString().contains("rulesCount=1"));
    }

    @Test
    @DisplayName("ClusterRole: default values")
    public void testClusterRole() {
        ClusterRole cr = new ClusterRole();
        assertEquals("rbac.authorization.k8s.io/v1", cr.getApiVersion());
        assertEquals("ClusterRole", cr.getKind());
        cr.setMetadata(new ObjectMeta().setName("my-clusterrole"));
        assertEquals("my-clusterrole", cr.getMetadata().getName());
    }

    @Test
    @DisplayName("RoleBinding: default values and subjects")
    public void testRoleBinding() {
        RoleBinding rb = new RoleBinding();
        assertEquals("rbac.authorization.k8s.io/v1", rb.getApiVersion());
        assertEquals("RoleBinding", rb.getKind());

        RoleBinding.Subject subject = new RoleBinding.Subject()
                .setKind("User")
                .setName("jane")
                .setApiGroup("rbac.authorization.k8s.io");

        RoleBinding.RoleRef roleRef = new RoleBinding.RoleRef()
                .setApiGroup("rbac.authorization.k8s.io")
                .setKind("Role")
                .setName("pod-reader");

        rb.addSubject(subject);
        rb.setRoleRef(roleRef);
        rb.setMetadata(new ObjectMeta().setName("my-rolebinding"));

        assertEquals(1, rb.getSubjects().size());
        assertEquals("User", rb.getSubjects().get(0).getKind());
        assertEquals("pod-reader", rb.getRoleRef().getName());
    }

    @Test
    @DisplayName("ClusterRoleBinding: default values")
    public void testClusterRoleBinding() {
        ClusterRoleBinding crb = new ClusterRoleBinding();
        assertEquals("rbac.authorization.k8s.io/v1", crb.getApiVersion());
        assertEquals("ClusterRoleBinding", crb.getKind());

        RoleBinding.Subject subject = new RoleBinding.Subject()
                .setKind("ServiceAccount")
                .setName("default")
                .setNamespace("kube-system");

        RoleBinding.RoleRef roleRef = new RoleBinding.RoleRef()
                .setApiGroup("rbac.authorization.k8s.io")
                .setKind("ClusterRole")
                .setName("cluster-admin");

        crb.addSubject(subject);
        crb.setRoleRef(roleRef);
        crb.setMetadata(new ObjectMeta().setName("my-crb"));

        assertEquals(1, crb.getSubjects().size());
        assertEquals("cluster-admin", crb.getRoleRef().getName());
    }

    @Test
    @DisplayName("ResourceQuota: default values")
    public void testResourceQuota() {
        ResourceQuota rq = new ResourceQuota();
        assertEquals("v1", rq.getApiVersion());
        assertEquals("ResourceQuota", rq.getKind());
        rq.setMetadata(new ObjectMeta().setName("my-quota"));
        assertEquals("my-quota", rq.getMetadata().getName());
    }

    @Test
    @DisplayName("LimitRange: default values")
    public void testLimitRange() {
        LimitRange lr = new LimitRange();
        assertEquals("v1", lr.getApiVersion());
        assertEquals("LimitRange", lr.getKind());
        lr.setMetadata(new ObjectMeta().setName("my-limitrange"));
        assertEquals("my-limitrange", lr.getMetadata().getName());
    }

    @Test
    @DisplayName("StorageClass: default values and fields")
    public void testStorageClass() {
        StorageClass sc = new StorageClass();
        assertEquals("storage.k8s.io/v1", sc.getApiVersion());
        assertEquals("StorageClass", sc.getKind());

        sc.setProvisioner("kubernetes.io/aws-ebs")
                .setReclaimPolicy("Delete")
                .setVolumeBindingMode("WaitForFirstConsumer")
                .setAllowVolumeExpansion(true)
                .setParameters(Collections.singletonMap("type", "gp2"))
                .setMountOptions(Arrays.asList("debug", "discard"));

        sc.setMetadata(new ObjectMeta().setName("my-sc"));

        assertEquals("kubernetes.io/aws-ebs", sc.getProvisioner());
        assertEquals("Delete", sc.getReclaimPolicy());
        assertTrue(sc.getAllowVolumeExpansion());
        assertEquals(2, sc.getMountOptions().size());
    }

    @Test
    @DisplayName("PodDisruptionBudget: default values and spec")
    public void testPodDisruptionBudget() {
        PodDisruptionBudget pdb = new PodDisruptionBudget();
        assertEquals("policy/v1", pdb.getApiVersion());
        assertEquals("PodDisruptionBudget", pdb.getKind());

        PodDisruptionBudget.PDBSpec spec = new PodDisruptionBudget.PDBSpec()
                .setMinAvailable(1)
                .setMaxUnavailable("50%")
                .setSelector(new Deployment.LabelSelector().addMatchLabel("app", "web"));

        pdb.setSpec(spec);
        pdb.setMetadata(new ObjectMeta().setName("my-pdb"));

        assertEquals(1, pdb.getSpec().getMinAvailable());
        assertEquals("50%", pdb.getSpec().getMaxUnavailable());
        assertEquals("web", pdb.getSpec().getSelector().getMatchLabels().get("app"));
    }

    @Test
    @DisplayName("Event: default values")
    public void testEvent() {
        Event event = new Event();
        assertEquals("v1", event.getApiVersion());
        assertEquals("Event", event.getKind());
        assertNull(event.getReason());
        assertNull(event.getMessage());
        assertNull(event.getType());

        event.setMetadata(new ObjectMeta().setName("my-event"));
        assertEquals("my-event", event.getMetadata().getName());
    }
}
