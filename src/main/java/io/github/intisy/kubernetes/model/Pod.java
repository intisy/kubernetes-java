package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Kubernetes Pod.
 *
 * @author Finn Birich
 */
public class Pod {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "Pod";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private PodSpec spec;

    @SerializedName("status")
    private PodStatus status;

    public Pod() {}

    public String getApiVersion() { return apiVersion; }
    public Pod setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public Pod setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public PodSpec getSpec() { return spec; }
    public Pod setSpec(PodSpec spec) { this.spec = spec; return this; }

    public PodStatus getStatus() { return status; }

    /**
     * Pod specification.
     */
    public static class PodSpec {
        @SerializedName("containers")
        private List<Container> containers;

        @SerializedName("initContainers")
        private List<Container> initContainers;

        @SerializedName("restartPolicy")
        private String restartPolicy;

        @SerializedName("terminationGracePeriodSeconds")
        private Long terminationGracePeriodSeconds;

        @SerializedName("dnsPolicy")
        private String dnsPolicy;

        @SerializedName("serviceAccountName")
        private String serviceAccountName;

        @SerializedName("serviceAccount")
        private String serviceAccount;

        @SerializedName("nodeName")
        private String nodeName;

        @SerializedName("nodeSelector")
        private Map<String, String> nodeSelector;

        @SerializedName("hostNetwork")
        private Boolean hostNetwork;

        @SerializedName("volumes")
        private List<Volume> volumes;

        public PodSpec() {}

        public List<Container> getContainers() { return containers; }
        public PodSpec setContainers(List<Container> containers) { this.containers = containers; return this; }

        public PodSpec addContainer(Container container) {
            if (this.containers == null) this.containers = new ArrayList<>();
            this.containers.add(container);
            return this;
        }

        public List<Container> getInitContainers() { return initContainers; }
        public PodSpec setInitContainers(List<Container> initContainers) { this.initContainers = initContainers; return this; }

        public String getRestartPolicy() { return restartPolicy; }
        public PodSpec setRestartPolicy(String restartPolicy) { this.restartPolicy = restartPolicy; return this; }

        public Long getTerminationGracePeriodSeconds() { return terminationGracePeriodSeconds; }
        public PodSpec setTerminationGracePeriodSeconds(Long seconds) { this.terminationGracePeriodSeconds = seconds; return this; }

        public String getDnsPolicy() { return dnsPolicy; }
        public PodSpec setDnsPolicy(String dnsPolicy) { this.dnsPolicy = dnsPolicy; return this; }

        public String getServiceAccountName() { return serviceAccountName; }
        public PodSpec setServiceAccountName(String serviceAccountName) { this.serviceAccountName = serviceAccountName; return this; }

        public String getNodeName() { return nodeName; }
        public PodSpec setNodeName(String nodeName) { this.nodeName = nodeName; return this; }

        public Map<String, String> getNodeSelector() { return nodeSelector; }
        public PodSpec setNodeSelector(Map<String, String> nodeSelector) { this.nodeSelector = nodeSelector; return this; }

        public Boolean getHostNetwork() { return hostNetwork; }
        public PodSpec setHostNetwork(Boolean hostNetwork) { this.hostNetwork = hostNetwork; return this; }

        public List<Volume> getVolumes() { return volumes; }
        public PodSpec setVolumes(List<Volume> volumes) { this.volumes = volumes; return this; }
    }

    /**
     * Container specification within a pod.
     */
    public static class Container {
        @SerializedName("name")
        private String name;

        @SerializedName("image")
        private String image;

        @SerializedName("command")
        private List<String> command;

        @SerializedName("args")
        private List<String> args;

        @SerializedName("workingDir")
        private String workingDir;

        @SerializedName("ports")
        private List<ContainerPort> ports;

        @SerializedName("env")
        private List<EnvVar> env;

        @SerializedName("resources")
        private ResourceRequirements resources;

        @SerializedName("volumeMounts")
        private List<VolumeMount> volumeMounts;

        @SerializedName("imagePullPolicy")
        private String imagePullPolicy;

        @SerializedName("stdin")
        private Boolean stdin;

        @SerializedName("tty")
        private Boolean tty;

        public Container() {}

        public String getName() { return name; }
        public Container setName(String name) { this.name = name; return this; }

        public String getImage() { return image; }
        public Container setImage(String image) { this.image = image; return this; }

        public List<String> getCommand() { return command; }
        public Container setCommand(List<String> command) { this.command = command; return this; }

        public List<String> getArgs() { return args; }
        public Container setArgs(List<String> args) { this.args = args; return this; }

        public String getWorkingDir() { return workingDir; }
        public Container setWorkingDir(String workingDir) { this.workingDir = workingDir; return this; }

        public List<ContainerPort> getPorts() { return ports; }
        public Container setPorts(List<ContainerPort> ports) { this.ports = ports; return this; }

        public Container addPort(ContainerPort port) {
            if (this.ports == null) this.ports = new ArrayList<>();
            this.ports.add(port);
            return this;
        }

        public List<EnvVar> getEnv() { return env; }
        public Container setEnv(List<EnvVar> env) { this.env = env; return this; }

        public Container addEnv(String name, String value) {
            if (this.env == null) this.env = new ArrayList<>();
            this.env.add(new EnvVar().setName(name).setValue(value));
            return this;
        }

        public ResourceRequirements getResources() { return resources; }
        public Container setResources(ResourceRequirements resources) { this.resources = resources; return this; }

        public List<VolumeMount> getVolumeMounts() { return volumeMounts; }
        public Container setVolumeMounts(List<VolumeMount> volumeMounts) { this.volumeMounts = volumeMounts; return this; }

        public String getImagePullPolicy() { return imagePullPolicy; }
        public Container setImagePullPolicy(String imagePullPolicy) { this.imagePullPolicy = imagePullPolicy; return this; }

        public Boolean getStdin() { return stdin; }
        public Container setStdin(Boolean stdin) { this.stdin = stdin; return this; }

        public Boolean getTty() { return tty; }
        public Container setTty(Boolean tty) { this.tty = tty; return this; }
    }

    /**
     * Container port configuration.
     */
    public static class ContainerPort {
        @SerializedName("name")
        private String name;

        @SerializedName("containerPort")
        private Integer containerPort;

        @SerializedName("hostPort")
        private Integer hostPort;

        @SerializedName("protocol")
        private String protocol;

        public ContainerPort() {}

        public ContainerPort(int containerPort) {
            this.containerPort = containerPort;
        }

        public String getName() { return name; }
        public ContainerPort setName(String name) { this.name = name; return this; }

        public Integer getContainerPort() { return containerPort; }
        public ContainerPort setContainerPort(Integer containerPort) { this.containerPort = containerPort; return this; }

        public Integer getHostPort() { return hostPort; }
        public ContainerPort setHostPort(Integer hostPort) { this.hostPort = hostPort; return this; }

        public String getProtocol() { return protocol; }
        public ContainerPort setProtocol(String protocol) { this.protocol = protocol; return this; }
    }

    /**
     * Environment variable for a container.
     */
    public static class EnvVar {
        @SerializedName("name")
        private String name;

        @SerializedName("value")
        private String value;

        public EnvVar() {}

        public String getName() { return name; }
        public EnvVar setName(String name) { this.name = name; return this; }

        public String getValue() { return value; }
        public EnvVar setValue(String value) { this.value = value; return this; }
    }

    /**
     * Resource requirements for a container.
     */
    public static class ResourceRequirements {
        @SerializedName("limits")
        private Map<String, String> limits;

        @SerializedName("requests")
        private Map<String, String> requests;

        public ResourceRequirements() {}

        public Map<String, String> getLimits() { return limits; }
        public ResourceRequirements setLimits(Map<String, String> limits) { this.limits = limits; return this; }

        public ResourceRequirements addLimit(String resource, String quantity) {
            if (this.limits == null) this.limits = new HashMap<>();
            this.limits.put(resource, quantity);
            return this;
        }

        public Map<String, String> getRequests() { return requests; }
        public ResourceRequirements setRequests(Map<String, String> requests) { this.requests = requests; return this; }

        public ResourceRequirements addRequest(String resource, String quantity) {
            if (this.requests == null) this.requests = new HashMap<>();
            this.requests.put(resource, quantity);
            return this;
        }
    }

    /**
     * Volume mount for a container.
     */
    public static class VolumeMount {
        @SerializedName("name")
        private String name;

        @SerializedName("mountPath")
        private String mountPath;

        @SerializedName("readOnly")
        private Boolean readOnly;

        @SerializedName("subPath")
        private String subPath;

        public VolumeMount() {}

        public String getName() { return name; }
        public VolumeMount setName(String name) { this.name = name; return this; }

        public String getMountPath() { return mountPath; }
        public VolumeMount setMountPath(String mountPath) { this.mountPath = mountPath; return this; }

        public Boolean getReadOnly() { return readOnly; }
        public VolumeMount setReadOnly(Boolean readOnly) { this.readOnly = readOnly; return this; }

        public String getSubPath() { return subPath; }
        public VolumeMount setSubPath(String subPath) { this.subPath = subPath; return this; }
    }

    /**
     * Volume specification.
     */
    public static class Volume {
        @SerializedName("name")
        private String name;

        @SerializedName("emptyDir")
        private Map<String, Object> emptyDir;

        @SerializedName("hostPath")
        private HostPathVolumeSource hostPath;

        @SerializedName("configMap")
        private ConfigMapVolumeSource configMap;

        @SerializedName("secret")
        private SecretVolumeSource secret;

        public Volume() {}

        public String getName() { return name; }
        public Volume setName(String name) { this.name = name; return this; }

        public Map<String, Object> getEmptyDir() { return emptyDir; }
        public Volume setEmptyDir(Map<String, Object> emptyDir) { this.emptyDir = emptyDir; return this; }

        public HostPathVolumeSource getHostPath() { return hostPath; }
        public Volume setHostPath(HostPathVolumeSource hostPath) { this.hostPath = hostPath; return this; }

        public ConfigMapVolumeSource getConfigMap() { return configMap; }
        public Volume setConfigMap(ConfigMapVolumeSource configMap) { this.configMap = configMap; return this; }

        public SecretVolumeSource getSecret() { return secret; }
        public Volume setSecret(SecretVolumeSource secret) { this.secret = secret; return this; }
    }

    public static class HostPathVolumeSource {
        @SerializedName("path")
        private String path;

        @SerializedName("type")
        private String type;

        public String getPath() { return path; }
        public HostPathVolumeSource setPath(String path) { this.path = path; return this; }

        public String getType() { return type; }
        public HostPathVolumeSource setType(String type) { this.type = type; return this; }
    }

    public static class ConfigMapVolumeSource {
        @SerializedName("name")
        private String name;

        public String getName() { return name; }
        public ConfigMapVolumeSource setName(String name) { this.name = name; return this; }
    }

    public static class SecretVolumeSource {
        @SerializedName("secretName")
        private String secretName;

        public String getSecretName() { return secretName; }
        public SecretVolumeSource setSecretName(String secretName) { this.secretName = secretName; return this; }
    }

    /**
     * Pod status information.
     */
    public static class PodStatus {
        @SerializedName("phase")
        private String phase;

        @SerializedName("conditions")
        private List<PodCondition> conditions;

        @SerializedName("message")
        private String message;

        @SerializedName("reason")
        private String reason;

        @SerializedName("hostIP")
        private String hostIP;

        @SerializedName("podIP")
        private String podIP;

        @SerializedName("startTime")
        private String startTime;

        @SerializedName("containerStatuses")
        private List<ContainerStatus> containerStatuses;

        public String getPhase() { return phase; }
        public List<PodCondition> getConditions() { return conditions; }
        public String getMessage() { return message; }
        public String getReason() { return reason; }
        public String getHostIP() { return hostIP; }
        public String getPodIP() { return podIP; }
        public String getStartTime() { return startTime; }
        public List<ContainerStatus> getContainerStatuses() { return containerStatuses; }
    }

    public static class PodCondition {
        @SerializedName("type")
        private String type;

        @SerializedName("status")
        private String status;

        @SerializedName("lastTransitionTime")
        private String lastTransitionTime;

        @SerializedName("reason")
        private String reason;

        @SerializedName("message")
        private String message;

        public String getType() { return type; }
        public String getStatus() { return status; }
        public String getLastTransitionTime() { return lastTransitionTime; }
        public String getReason() { return reason; }
        public String getMessage() { return message; }
    }

    public static class ContainerStatus {
        @SerializedName("name")
        private String name;

        @SerializedName("ready")
        private Boolean ready;

        @SerializedName("restartCount")
        private Integer restartCount;

        @SerializedName("image")
        private String image;

        @SerializedName("imageID")
        private String imageID;

        @SerializedName("containerID")
        private String containerID;

        @SerializedName("started")
        private Boolean started;

        public String getName() { return name; }
        public Boolean getReady() { return ready; }
        public Integer getRestartCount() { return restartCount; }
        public String getImage() { return image; }
        public String getImageID() { return imageID; }
        public String getContainerID() { return containerID; }
        public Boolean getStarted() { return started; }
    }

    @Override
    public String toString() {
        return "Pod{" +
                "metadata=" + metadata +
                ", status=" + (status != null ? status.phase : "null") +
                '}';
    }
}
