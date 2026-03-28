package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a Kubernetes NetworkPolicy.
 *
 * @author Finn Birich
 */
public class NetworkPolicy {
    @SerializedName("apiVersion")
    private String apiVersion = "networking.k8s.io/v1";

    @SerializedName("kind")
    private String kind = "NetworkPolicy";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private NetworkPolicySpec spec;

    public NetworkPolicy() {}

    public String getApiVersion() { return apiVersion; }
    public NetworkPolicy setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public NetworkPolicy setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public NetworkPolicySpec getSpec() { return spec; }
    public NetworkPolicy setSpec(NetworkPolicySpec spec) { this.spec = spec; return this; }

    public static class NetworkPolicySpec {
        @SerializedName("podSelector")
        private Deployment.LabelSelector podSelector;

        @SerializedName("ingress")
        private List<NetworkPolicyIngressRule> ingress;

        @SerializedName("egress")
        private List<NetworkPolicyEgressRule> egress;

        @SerializedName("policyTypes")
        private List<String> policyTypes;

        public NetworkPolicySpec() {}

        public Deployment.LabelSelector getPodSelector() { return podSelector; }
        public NetworkPolicySpec setPodSelector(Deployment.LabelSelector podSelector) { this.podSelector = podSelector; return this; }

        public List<NetworkPolicyIngressRule> getIngress() { return ingress; }
        public NetworkPolicySpec setIngress(List<NetworkPolicyIngressRule> ingress) { this.ingress = ingress; return this; }

        public List<NetworkPolicyEgressRule> getEgress() { return egress; }
        public NetworkPolicySpec setEgress(List<NetworkPolicyEgressRule> egress) { this.egress = egress; return this; }

        public List<String> getPolicyTypes() { return policyTypes; }
        public NetworkPolicySpec setPolicyTypes(List<String> policyTypes) { this.policyTypes = policyTypes; return this; }
    }

    public static class NetworkPolicyIngressRule {
        @SerializedName("ports")
        private List<NetworkPolicyPort> ports;

        @SerializedName("from")
        private List<NetworkPolicyPeer> from;

        public List<NetworkPolicyPort> getPorts() { return ports; }
        public NetworkPolicyIngressRule setPorts(List<NetworkPolicyPort> ports) { this.ports = ports; return this; }

        public List<NetworkPolicyPeer> getFrom() { return from; }
        public NetworkPolicyIngressRule setFrom(List<NetworkPolicyPeer> from) { this.from = from; return this; }
    }

    public static class NetworkPolicyEgressRule {
        @SerializedName("ports")
        private List<NetworkPolicyPort> ports;

        @SerializedName("to")
        private List<NetworkPolicyPeer> to;

        public List<NetworkPolicyPort> getPorts() { return ports; }
        public NetworkPolicyEgressRule setPorts(List<NetworkPolicyPort> ports) { this.ports = ports; return this; }

        public List<NetworkPolicyPeer> getTo() { return to; }
        public NetworkPolicyEgressRule setTo(List<NetworkPolicyPeer> to) { this.to = to; return this; }
    }

    public static class NetworkPolicyPort {
        @SerializedName("protocol")
        private String protocol;

        @SerializedName("port")
        private Object port;

        public String getProtocol() { return protocol; }
        public NetworkPolicyPort setProtocol(String protocol) { this.protocol = protocol; return this; }

        public Object getPort() { return port; }
        public NetworkPolicyPort setPort(Object port) { this.port = port; return this; }
    }

    public static class NetworkPolicyPeer {
        @SerializedName("podSelector")
        private Deployment.LabelSelector podSelector;

        @SerializedName("namespaceSelector")
        private Deployment.LabelSelector namespaceSelector;

        @SerializedName("ipBlock")
        private IPBlock ipBlock;

        public Deployment.LabelSelector getPodSelector() { return podSelector; }
        public NetworkPolicyPeer setPodSelector(Deployment.LabelSelector podSelector) { this.podSelector = podSelector; return this; }

        public Deployment.LabelSelector getNamespaceSelector() { return namespaceSelector; }
        public NetworkPolicyPeer setNamespaceSelector(Deployment.LabelSelector namespaceSelector) { this.namespaceSelector = namespaceSelector; return this; }

        public IPBlock getIpBlock() { return ipBlock; }
        public NetworkPolicyPeer setIpBlock(IPBlock ipBlock) { this.ipBlock = ipBlock; return this; }
    }

    public static class IPBlock {
        @SerializedName("cidr")
        private String cidr;

        @SerializedName("except")
        private List<String> except;

        public String getCidr() { return cidr; }
        public IPBlock setCidr(String cidr) { this.cidr = cidr; return this; }

        public List<String> getExcept() { return except; }
        public IPBlock setExcept(List<String> except) { this.except = except; return this; }
    }

    @Override
    public String toString() {
        return "NetworkPolicy{" +
                "metadata=" + metadata +
                ", policyTypes=" + (spec != null ? spec.policyTypes : "null") +
                '}';
    }
}
