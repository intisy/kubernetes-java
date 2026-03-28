package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Kubernetes Role (RBAC).
 *
 * @author Finn Birich
 */
public class Role {
    @SerializedName("apiVersion")
    private String apiVersion = "rbac.authorization.k8s.io/v1";

    @SerializedName("kind")
    private String kind = "Role";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("rules")
    private List<PolicyRule> rules;

    public Role() {}

    public String getApiVersion() { return apiVersion; }
    public Role setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public Role setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public List<PolicyRule> getRules() { return rules; }
    public Role setRules(List<PolicyRule> rules) { this.rules = rules; return this; }

    public Role addRule(PolicyRule rule) {
        if (this.rules == null) this.rules = new ArrayList<PolicyRule>();
        this.rules.add(rule);
        return this;
    }

    /**
     * PolicyRule holds information that describes a policy rule.
     * Shared by Role and ClusterRole.
     */
    public static class PolicyRule {
        @SerializedName("apiGroups")
        private List<String> apiGroups;

        @SerializedName("resources")
        private List<String> resources;

        @SerializedName("verbs")
        private List<String> verbs;

        @SerializedName("resourceNames")
        private List<String> resourceNames;

        @SerializedName("nonResourceURLs")
        private List<String> nonResourceURLs;

        public PolicyRule() {}

        public List<String> getApiGroups() { return apiGroups; }
        public PolicyRule setApiGroups(List<String> apiGroups) { this.apiGroups = apiGroups; return this; }

        public List<String> getResources() { return resources; }
        public PolicyRule setResources(List<String> resources) { this.resources = resources; return this; }

        public List<String> getVerbs() { return verbs; }
        public PolicyRule setVerbs(List<String> verbs) { this.verbs = verbs; return this; }

        public List<String> getResourceNames() { return resourceNames; }
        public PolicyRule setResourceNames(List<String> resourceNames) { this.resourceNames = resourceNames; return this; }

        public List<String> getNonResourceURLs() { return nonResourceURLs; }
        public PolicyRule setNonResourceURLs(List<String> nonResourceURLs) { this.nonResourceURLs = nonResourceURLs; return this; }
    }

    @Override
    public String toString() {
        return "Role{" +
                "metadata=" + metadata +
                ", rulesCount=" + (rules != null ? rules.size() : 0) +
                '}';
    }
}
