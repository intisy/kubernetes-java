package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Kubernetes Ingress.
 *
 * @author Finn Birich
 */
public class Ingress {
    @SerializedName("apiVersion")
    private String apiVersion = "networking.k8s.io/v1";

    @SerializedName("kind")
    private String kind = "Ingress";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private IngressSpec spec;

    @SerializedName("status")
    private IngressStatus status;

    public Ingress() {}

    public String getApiVersion() { return apiVersion; }
    public Ingress setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public Ingress setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public IngressSpec getSpec() { return spec; }
    public Ingress setSpec(IngressSpec spec) { this.spec = spec; return this; }

    public IngressStatus getStatus() { return status; }

    public static class IngressSpec {
        @SerializedName("ingressClassName")
        private String ingressClassName;

        @SerializedName("defaultBackend")
        private IngressBackend defaultBackend;

        @SerializedName("tls")
        private List<IngressTLS> tls;

        @SerializedName("rules")
        private List<IngressRule> rules;

        public IngressSpec() {}

        public String getIngressClassName() { return ingressClassName; }
        public IngressSpec setIngressClassName(String ingressClassName) { this.ingressClassName = ingressClassName; return this; }

        public IngressBackend getDefaultBackend() { return defaultBackend; }
        public IngressSpec setDefaultBackend(IngressBackend defaultBackend) { this.defaultBackend = defaultBackend; return this; }

        public List<IngressTLS> getTls() { return tls; }
        public IngressSpec setTls(List<IngressTLS> tls) { this.tls = tls; return this; }

        public List<IngressRule> getRules() { return rules; }
        public IngressSpec setRules(List<IngressRule> rules) { this.rules = rules; return this; }

        public IngressSpec addRule(IngressRule rule) {
            if (this.rules == null) this.rules = new ArrayList<IngressRule>();
            this.rules.add(rule);
            return this;
        }
    }

    public static class IngressRule {
        @SerializedName("host")
        private String host;

        @SerializedName("http")
        private HTTPIngressRuleValue http;

        public IngressRule() {}

        public String getHost() { return host; }
        public IngressRule setHost(String host) { this.host = host; return this; }

        public HTTPIngressRuleValue getHttp() { return http; }
        public IngressRule setHttp(HTTPIngressRuleValue http) { this.http = http; return this; }
    }

    public static class HTTPIngressRuleValue {
        @SerializedName("paths")
        private List<HTTPIngressPath> paths;

        public HTTPIngressRuleValue() {}

        public List<HTTPIngressPath> getPaths() { return paths; }
        public HTTPIngressRuleValue setPaths(List<HTTPIngressPath> paths) { this.paths = paths; return this; }

        public HTTPIngressRuleValue addPath(HTTPIngressPath path) {
            if (this.paths == null) this.paths = new ArrayList<HTTPIngressPath>();
            this.paths.add(path);
            return this;
        }
    }

    public static class HTTPIngressPath {
        @SerializedName("path")
        private String path;

        @SerializedName("pathType")
        private String pathType;

        @SerializedName("backend")
        private IngressBackend backend;

        public HTTPIngressPath() {}

        public String getPath() { return path; }
        public HTTPIngressPath setPath(String path) { this.path = path; return this; }

        public String getPathType() { return pathType; }
        public HTTPIngressPath setPathType(String pathType) { this.pathType = pathType; return this; }

        public IngressBackend getBackend() { return backend; }
        public HTTPIngressPath setBackend(IngressBackend backend) { this.backend = backend; return this; }
    }

    public static class IngressBackend {
        @SerializedName("service")
        private IngressServiceBackend service;

        public IngressBackend() {}

        public IngressServiceBackend getService() { return service; }
        public IngressBackend setService(IngressServiceBackend service) { this.service = service; return this; }
    }

    public static class IngressServiceBackend {
        @SerializedName("name")
        private String name;

        @SerializedName("port")
        private ServiceBackendPort port;

        public IngressServiceBackend() {}

        public String getName() { return name; }
        public IngressServiceBackend setName(String name) { this.name = name; return this; }

        public ServiceBackendPort getPort() { return port; }
        public IngressServiceBackend setPort(ServiceBackendPort port) { this.port = port; return this; }
    }

    public static class ServiceBackendPort {
        @SerializedName("name")
        private String name;

        @SerializedName("number")
        private Integer number;

        public ServiceBackendPort() {}

        public String getName() { return name; }
        public ServiceBackendPort setName(String name) { this.name = name; return this; }

        public Integer getNumber() { return number; }
        public ServiceBackendPort setNumber(Integer number) { this.number = number; return this; }
    }

    public static class IngressTLS {
        @SerializedName("hosts")
        private List<String> hosts;

        @SerializedName("secretName")
        private String secretName;

        public IngressTLS() {}

        public List<String> getHosts() { return hosts; }
        public IngressTLS setHosts(List<String> hosts) { this.hosts = hosts; return this; }

        public String getSecretName() { return secretName; }
        public IngressTLS setSecretName(String secretName) { this.secretName = secretName; return this; }
    }

    public static class IngressStatus {
        @SerializedName("loadBalancer")
        private Service.LoadBalancerStatus loadBalancer;

        public Service.LoadBalancerStatus getLoadBalancer() { return loadBalancer; }
    }

    @Override
    public String toString() {
        return "Ingress{" +
                "metadata=" + metadata +
                ", rulesCount=" + (spec != null && spec.rules != null ? spec.rules.size() : 0) +
                '}';
    }
}
