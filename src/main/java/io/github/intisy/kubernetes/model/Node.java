package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Represents a Kubernetes Node.
 *
 * @author Finn Birich
 */
public class Node {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "Node";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private NodeSpec spec;

    @SerializedName("status")
    private NodeStatus status;

    public String getApiVersion() { return apiVersion; }
    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public NodeSpec getSpec() { return spec; }
    public NodeStatus getStatus() { return status; }

    public static class NodeSpec {
        @SerializedName("podCIDR")
        private String podCIDR;

        @SerializedName("providerID")
        private String providerID;

        @SerializedName("unschedulable")
        private Boolean unschedulable;

        @SerializedName("taints")
        private List<Taint> taints;

        public String getPodCIDR() { return podCIDR; }
        public String getProviderID() { return providerID; }
        public Boolean getUnschedulable() { return unschedulable; }
        public List<Taint> getTaints() { return taints; }
    }

    public static class Taint {
        @SerializedName("key")
        private String key;

        @SerializedName("value")
        private String value;

        @SerializedName("effect")
        private String effect;

        public String getKey() { return key; }
        public String getValue() { return value; }
        public String getEffect() { return effect; }
    }

    public static class NodeStatus {
        @SerializedName("capacity")
        private Map<String, String> capacity;

        @SerializedName("allocatable")
        private Map<String, String> allocatable;

        @SerializedName("conditions")
        private List<NodeCondition> conditions;

        @SerializedName("addresses")
        private List<NodeAddress> addresses;

        @SerializedName("nodeInfo")
        private NodeSystemInfo nodeInfo;

        public Map<String, String> getCapacity() { return capacity; }
        public Map<String, String> getAllocatable() { return allocatable; }
        public List<NodeCondition> getConditions() { return conditions; }
        public List<NodeAddress> getAddresses() { return addresses; }
        public NodeSystemInfo getNodeInfo() { return nodeInfo; }
    }

    public static class NodeCondition {
        @SerializedName("type")
        private String type;

        @SerializedName("status")
        private String status;

        @SerializedName("lastHeartbeatTime")
        private String lastHeartbeatTime;

        @SerializedName("lastTransitionTime")
        private String lastTransitionTime;

        @SerializedName("reason")
        private String reason;

        @SerializedName("message")
        private String message;

        public String getType() { return type; }
        public String getStatus() { return status; }
        public String getLastHeartbeatTime() { return lastHeartbeatTime; }
        public String getLastTransitionTime() { return lastTransitionTime; }
        public String getReason() { return reason; }
        public String getMessage() { return message; }
    }

    public static class NodeAddress {
        @SerializedName("type")
        private String type;

        @SerializedName("address")
        private String address;

        public String getType() { return type; }
        public String getAddress() { return address; }
    }

    public static class NodeSystemInfo {
        @SerializedName("machineID")
        private String machineID;

        @SerializedName("systemUUID")
        private String systemUUID;

        @SerializedName("bootID")
        private String bootID;

        @SerializedName("kernelVersion")
        private String kernelVersion;

        @SerializedName("osImage")
        private String osImage;

        @SerializedName("containerRuntimeVersion")
        private String containerRuntimeVersion;

        @SerializedName("kubeletVersion")
        private String kubeletVersion;

        @SerializedName("kubeProxyVersion")
        private String kubeProxyVersion;

        @SerializedName("operatingSystem")
        private String operatingSystem;

        @SerializedName("architecture")
        private String architecture;

        public String getMachineID() { return machineID; }
        public String getKernelVersion() { return kernelVersion; }
        public String getOsImage() { return osImage; }
        public String getContainerRuntimeVersion() { return containerRuntimeVersion; }
        public String getKubeletVersion() { return kubeletVersion; }
        public String getOperatingSystem() { return operatingSystem; }
        public String getArchitecture() { return architecture; }
    }

    @Override
    public String toString() {
        return "Node{" +
                "metadata=" + metadata +
                '}';
    }
}
