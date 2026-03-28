package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a Kubernetes HorizontalPodAutoscaler.
 *
 * @author Finn Birich
 */
public class HorizontalPodAutoscaler {
    @SerializedName("apiVersion")
    private String apiVersion = "autoscaling/v1";

    @SerializedName("kind")
    private String kind = "HorizontalPodAutoscaler";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private HPASpec spec;

    @SerializedName("status")
    private HPAStatus status;

    public HorizontalPodAutoscaler() {}

    public String getApiVersion() { return apiVersion; }
    public HorizontalPodAutoscaler setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public HorizontalPodAutoscaler setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public HPASpec getSpec() { return spec; }
    public HorizontalPodAutoscaler setSpec(HPASpec spec) { this.spec = spec; return this; }

    public HPAStatus getStatus() { return status; }

    public static class HPASpec {
        @SerializedName("scaleTargetRef")
        private CrossVersionObjectReference scaleTargetRef;

        @SerializedName("minReplicas")
        private Integer minReplicas;

        @SerializedName("maxReplicas")
        private Integer maxReplicas;

        @SerializedName("targetCPUUtilizationPercentage")
        private Integer targetCPUUtilizationPercentage;

        public HPASpec() {}

        public CrossVersionObjectReference getScaleTargetRef() { return scaleTargetRef; }
        public HPASpec setScaleTargetRef(CrossVersionObjectReference ref) { this.scaleTargetRef = ref; return this; }

        public Integer getMinReplicas() { return minReplicas; }
        public HPASpec setMinReplicas(Integer minReplicas) { this.minReplicas = minReplicas; return this; }

        public Integer getMaxReplicas() { return maxReplicas; }
        public HPASpec setMaxReplicas(Integer maxReplicas) { this.maxReplicas = maxReplicas; return this; }

        public Integer getTargetCPUUtilizationPercentage() { return targetCPUUtilizationPercentage; }
        public HPASpec setTargetCPUUtilizationPercentage(Integer pct) { this.targetCPUUtilizationPercentage = pct; return this; }
    }

    public static class CrossVersionObjectReference {
        @SerializedName("apiVersion")
        private String apiVersion;

        @SerializedName("kind")
        private String kind;

        @SerializedName("name")
        private String name;

        public CrossVersionObjectReference() {}

        public String getApiVersion() { return apiVersion; }
        public CrossVersionObjectReference setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

        public String getKind() { return kind; }
        public CrossVersionObjectReference setKind(String kind) { this.kind = kind; return this; }

        public String getName() { return name; }
        public CrossVersionObjectReference setName(String name) { this.name = name; return this; }
    }

    public static class HPAStatus {
        @SerializedName("observedGeneration")
        private Long observedGeneration;

        @SerializedName("lastScaleTime")
        private String lastScaleTime;

        @SerializedName("currentReplicas")
        private Integer currentReplicas;

        @SerializedName("desiredReplicas")
        private Integer desiredReplicas;

        @SerializedName("currentCPUUtilizationPercentage")
        private Integer currentCPUUtilizationPercentage;

        public Long getObservedGeneration() { return observedGeneration; }
        public String getLastScaleTime() { return lastScaleTime; }
        public Integer getCurrentReplicas() { return currentReplicas; }
        public Integer getDesiredReplicas() { return desiredReplicas; }
        public Integer getCurrentCPUUtilizationPercentage() { return currentCPUUtilizationPercentage; }
    }

    @Override
    public String toString() {
        return "HorizontalPodAutoscaler{" +
                "metadata=" + metadata +
                ", minReplicas=" + (spec != null ? spec.minReplicas : "null") +
                ", maxReplicas=" + (spec != null ? spec.maxReplicas : "null") +
                '}';
    }
}
