package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Kubernetes Service.
 *
 * @author Finn Birich
 */
public class Service {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "Service";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private ServiceSpec spec;

    @SerializedName("status")
    private ServiceStatus status;

    public Service() {}

    public String getApiVersion() { return apiVersion; }
    public Service setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public Service setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public ServiceSpec getSpec() { return spec; }
    public Service setSpec(ServiceSpec spec) { this.spec = spec; return this; }

    public ServiceStatus getStatus() { return status; }

    /**
     * Service specification.
     */
    public static class ServiceSpec {
        @SerializedName("ports")
        private List<ServicePort> ports;

        @SerializedName("selector")
        private Map<String, String> selector;

        @SerializedName("clusterIP")
        private String clusterIP;

        @SerializedName("type")
        private String type;

        @SerializedName("externalIPs")
        private List<String> externalIPs;

        @SerializedName("sessionAffinity")
        private String sessionAffinity;

        @SerializedName("loadBalancerIP")
        private String loadBalancerIP;

        @SerializedName("externalName")
        private String externalName;

        public ServiceSpec() {}

        public List<ServicePort> getPorts() { return ports; }
        public ServiceSpec setPorts(List<ServicePort> ports) { this.ports = ports; return this; }

        public ServiceSpec addPort(ServicePort port) {
            if (this.ports == null) this.ports = new ArrayList<>();
            this.ports.add(port);
            return this;
        }

        public Map<String, String> getSelector() { return selector; }
        public ServiceSpec setSelector(Map<String, String> selector) { this.selector = selector; return this; }

        public ServiceSpec addSelector(String key, String value) {
            if (this.selector == null) this.selector = new HashMap<>();
            this.selector.put(key, value);
            return this;
        }

        public String getClusterIP() { return clusterIP; }
        public ServiceSpec setClusterIP(String clusterIP) { this.clusterIP = clusterIP; return this; }

        public String getType() { return type; }
        public ServiceSpec setType(String type) { this.type = type; return this; }

        public List<String> getExternalIPs() { return externalIPs; }
        public ServiceSpec setExternalIPs(List<String> externalIPs) { this.externalIPs = externalIPs; return this; }

        public String getSessionAffinity() { return sessionAffinity; }
        public ServiceSpec setSessionAffinity(String sessionAffinity) { this.sessionAffinity = sessionAffinity; return this; }

        public String getLoadBalancerIP() { return loadBalancerIP; }
        public ServiceSpec setLoadBalancerIP(String loadBalancerIP) { this.loadBalancerIP = loadBalancerIP; return this; }

        public String getExternalName() { return externalName; }
        public ServiceSpec setExternalName(String externalName) { this.externalName = externalName; return this; }
    }

    /**
     * Service port configuration.
     */
    public static class ServicePort {
        @SerializedName("name")
        private String name;

        @SerializedName("protocol")
        private String protocol;

        @SerializedName("port")
        private Integer port;

        @SerializedName("targetPort")
        private Object targetPort;

        @SerializedName("nodePort")
        private Integer nodePort;

        public ServicePort() {}

        public ServicePort(int port, int targetPort) {
            this.port = port;
            this.targetPort = targetPort;
        }

        public String getName() { return name; }
        public ServicePort setName(String name) { this.name = name; return this; }

        public String getProtocol() { return protocol; }
        public ServicePort setProtocol(String protocol) { this.protocol = protocol; return this; }

        public Integer getPort() { return port; }
        public ServicePort setPort(Integer port) { this.port = port; return this; }

        public Object getTargetPort() { return targetPort; }
        public ServicePort setTargetPort(Object targetPort) { this.targetPort = targetPort; return this; }

        public Integer getNodePort() { return nodePort; }
        public ServicePort setNodePort(Integer nodePort) { this.nodePort = nodePort; return this; }
    }

    /**
     * Service status.
     */
    public static class ServiceStatus {
        @SerializedName("loadBalancer")
        private LoadBalancerStatus loadBalancer;

        public LoadBalancerStatus getLoadBalancer() { return loadBalancer; }
    }

    public static class LoadBalancerStatus {
        @SerializedName("ingress")
        private List<LoadBalancerIngress> ingress;

        public List<LoadBalancerIngress> getIngress() { return ingress; }
    }

    public static class LoadBalancerIngress {
        @SerializedName("ip")
        private String ip;

        @SerializedName("hostname")
        private String hostname;

        public String getIp() { return ip; }
        public String getHostname() { return hostname; }
    }

    @Override
    public String toString() {
        return "Service{" +
                "metadata=" + metadata +
                ", type=" + (spec != null ? spec.type : "null") +
                ", clusterIP=" + (spec != null ? spec.clusterIP : "null") +
                '}';
    }
}
