package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a Kubernetes LimitRange.
 *
 * @author Finn Birich
 */
public class LimitRange {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "LimitRange";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private LimitRangeSpec spec;

    public LimitRange() {}

    public String getApiVersion() { return apiVersion; }
    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public LimitRange setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public LimitRangeSpec getSpec() { return spec; }
    public LimitRange setSpec(LimitRangeSpec spec) { this.spec = spec; return this; }

    public static class LimitRangeSpec {
        @SerializedName("limits")
        private List<LimitRangeItem> limits;

        public List<LimitRangeItem> getLimits() { return limits; }
        public LimitRangeSpec setLimits(List<LimitRangeItem> limits) { this.limits = limits; return this; }
    }

    public static class LimitRangeItem {
        @SerializedName("type")
        private String type;

        @SerializedName("max")
        private java.util.Map<String, String> max;

        @SerializedName("min")
        private java.util.Map<String, String> min;

        @SerializedName("default")
        private java.util.Map<String, String> defaultLimit;

        @SerializedName("defaultRequest")
        private java.util.Map<String, String> defaultRequest;

        @SerializedName("maxLimitRequestRatio")
        private java.util.Map<String, String> maxLimitRequestRatio;

        public String getType() { return type; }
        public LimitRangeItem setType(String type) { this.type = type; return this; }

        public java.util.Map<String, String> getMax() { return max; }
        public LimitRangeItem setMax(java.util.Map<String, String> max) { this.max = max; return this; }

        public java.util.Map<String, String> getMin() { return min; }
        public LimitRangeItem setMin(java.util.Map<String, String> min) { this.min = min; return this; }

        public java.util.Map<String, String> getDefaultLimit() { return defaultLimit; }
        public LimitRangeItem setDefaultLimit(java.util.Map<String, String> defaultLimit) { this.defaultLimit = defaultLimit; return this; }

        public java.util.Map<String, String> getDefaultRequest() { return defaultRequest; }
        public LimitRangeItem setDefaultRequest(java.util.Map<String, String> defaultRequest) { this.defaultRequest = defaultRequest; return this; }

        public java.util.Map<String, String> getMaxLimitRequestRatio() { return maxLimitRequestRatio; }
    }

    @Override
    public String toString() {
        return "LimitRange{" +
                "metadata=" + metadata +
                '}';
    }
}
