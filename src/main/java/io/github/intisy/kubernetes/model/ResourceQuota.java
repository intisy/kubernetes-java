package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Represents a Kubernetes ResourceQuota.
 *
 * @author Finn Birich
 */
public class ResourceQuota {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "ResourceQuota";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private ResourceQuotaSpec spec;

    @SerializedName("status")
    private ResourceQuotaStatus status;

    public ResourceQuota() {}

    public String getApiVersion() { return apiVersion; }
    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public ResourceQuota setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public ResourceQuotaSpec getSpec() { return spec; }
    public ResourceQuota setSpec(ResourceQuotaSpec spec) { this.spec = spec; return this; }

    public ResourceQuotaStatus getStatus() { return status; }

    public static class ResourceQuotaSpec {
        @SerializedName("hard")
        private Map<String, String> hard;

        @SerializedName("scopeSelector")
        private ScopeSelector scopeSelector;

        public Map<String, String> getHard() { return hard; }
        public ResourceQuotaSpec setHard(Map<String, String> hard) { this.hard = hard; return this; }

        public ScopeSelector getScopeSelector() { return scopeSelector; }
        public ResourceQuotaSpec setScopeSelector(ScopeSelector scopeSelector) { this.scopeSelector = scopeSelector; return this; }
    }

    public static class ScopeSelector {
        @SerializedName("matchExpressions")
        private java.util.List<ScopedResourceSelectorRequirement> matchExpressions;

        public java.util.List<ScopedResourceSelectorRequirement> getMatchExpressions() { return matchExpressions; }
    }

    public static class ScopedResourceSelectorRequirement {
        @SerializedName("scopeName")
        private String scopeName;

        @SerializedName("operator")
        private String operator;

        @SerializedName("values")
        private java.util.List<String> values;

        public String getScopeName() { return scopeName; }
        public String getOperator() { return operator; }
        public java.util.List<String> getValues() { return values; }
    }

    public static class ResourceQuotaStatus {
        @SerializedName("hard")
        private Map<String, String> hard;

        @SerializedName("used")
        private Map<String, String> used;

        public Map<String, String> getHard() { return hard; }
        public Map<String, String> getUsed() { return used; }
    }

    @Override
    public String toString() {
        return "ResourceQuota{" +
                "metadata=" + metadata +
                '}';
    }
}
