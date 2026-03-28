package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a Kubernetes Endpoints resource.
 *
 * @author Finn Birich
 */
public class Endpoints {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "Endpoints";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("subsets")
    private List<EndpointSubset> subsets;

    public Endpoints() {}

    public String getApiVersion() { return apiVersion; }
    public Endpoints setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public Endpoints setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public List<EndpointSubset> getSubsets() { return subsets; }
    public Endpoints setSubsets(List<EndpointSubset> subsets) { this.subsets = subsets; return this; }

    public static class EndpointSubset {
        @SerializedName("addresses")
        private List<EndpointAddress> addresses;

        @SerializedName("notReadyAddresses")
        private List<EndpointAddress> notReadyAddresses;

        @SerializedName("ports")
        private List<EndpointPort> ports;

        public List<EndpointAddress> getAddresses() { return addresses; }
        public EndpointSubset setAddresses(List<EndpointAddress> addresses) { this.addresses = addresses; return this; }

        public List<EndpointAddress> getNotReadyAddresses() { return notReadyAddresses; }
        public EndpointSubset setNotReadyAddresses(List<EndpointAddress> notReadyAddresses) { this.notReadyAddresses = notReadyAddresses; return this; }

        public List<EndpointPort> getPorts() { return ports; }
        public EndpointSubset setPorts(List<EndpointPort> ports) { this.ports = ports; return this; }
    }

    public static class EndpointAddress {
        @SerializedName("ip")
        private String ip;

        @SerializedName("hostname")
        private String hostname;

        @SerializedName("nodeName")
        private String nodeName;

        public String getIp() { return ip; }
        public EndpointAddress setIp(String ip) { this.ip = ip; return this; }

        public String getHostname() { return hostname; }
        public EndpointAddress setHostname(String hostname) { this.hostname = hostname; return this; }

        public String getNodeName() { return nodeName; }
        public EndpointAddress setNodeName(String nodeName) { this.nodeName = nodeName; return this; }
    }

    public static class EndpointPort {
        @SerializedName("name")
        private String name;

        @SerializedName("port")
        private Integer port;

        @SerializedName("protocol")
        private String protocol;

        public String getName() { return name; }
        public EndpointPort setName(String name) { this.name = name; return this; }

        public Integer getPort() { return port; }
        public EndpointPort setPort(Integer port) { this.port = port; return this; }

        public String getProtocol() { return protocol; }
        public EndpointPort setProtocol(String protocol) { this.protocol = protocol; return this; }
    }

    @Override
    public String toString() {
        return "Endpoints{" +
                "metadata=" + metadata +
                ", subsets=" + (subsets != null ? subsets.size() : 0) +
                '}';
    }
}
