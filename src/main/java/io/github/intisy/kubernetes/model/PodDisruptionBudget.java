package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a Kubernetes PodDisruptionBudget.
 *
 * @author Finn Birich
 */
public class PodDisruptionBudget {
    @SerializedName("apiVersion")
    private String apiVersion = "policy/v1";

    @SerializedName("kind")
    private String kind = "PodDisruptionBudget";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private PDBSpec spec;

    @SerializedName("status")
    private PDBStatus status;

    public PodDisruptionBudget() {}

    public String getApiVersion() { return apiVersion; }
    public PodDisruptionBudget setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public PodDisruptionBudget setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public PDBSpec getSpec() { return spec; }
    public PodDisruptionBudget setSpec(PDBSpec spec) { this.spec = spec; return this; }

    public PDBStatus getStatus() { return status; }

    public static class PDBSpec {
        @SerializedName("minAvailable")
        private Object minAvailable;

        @SerializedName("maxUnavailable")
        private Object maxUnavailable;

        @SerializedName("selector")
        private Deployment.LabelSelector selector;

        public PDBSpec() {}

        public Object getMinAvailable() { return minAvailable; }
        public PDBSpec setMinAvailable(Object minAvailable) { this.minAvailable = minAvailable; return this; }

        public Object getMaxUnavailable() { return maxUnavailable; }
        public PDBSpec setMaxUnavailable(Object maxUnavailable) { this.maxUnavailable = maxUnavailable; return this; }

        public Deployment.LabelSelector getSelector() { return selector; }
        public PDBSpec setSelector(Deployment.LabelSelector selector) { this.selector = selector; return this; }
    }

    public static class PDBStatus {
        @SerializedName("observedGeneration")
        private Long observedGeneration;

        @SerializedName("disruptedPods")
        private java.util.Map<String, String> disruptedPods;

        @SerializedName("disruptionsAllowed")
        private Integer disruptionsAllowed;

        @SerializedName("currentHealthy")
        private Integer currentHealthy;

        @SerializedName("desiredHealthy")
        private Integer desiredHealthy;

        @SerializedName("expectedPods")
        private Integer expectedPods;

        @SerializedName("conditions")
        private List<PDBCondition> conditions;

        public Long getObservedGeneration() { return observedGeneration; }
        public java.util.Map<String, String> getDisruptedPods() { return disruptedPods; }
        public Integer getDisruptionsAllowed() { return disruptionsAllowed; }
        public Integer getCurrentHealthy() { return currentHealthy; }
        public Integer getDesiredHealthy() { return desiredHealthy; }
        public Integer getExpectedPods() { return expectedPods; }
        public List<PDBCondition> getConditions() { return conditions; }
    }

    public static class PDBCondition {
        @SerializedName("type")
        private String type;

        @SerializedName("status")
        private String status;

        @SerializedName("observedGeneration")
        private Long observedGeneration;

        @SerializedName("lastTransitionTime")
        private String lastTransitionTime;

        @SerializedName("reason")
        private String reason;

        @SerializedName("message")
        private String message;

        public String getType() { return type; }
        public String getStatus() { return status; }
        public Long getObservedGeneration() { return observedGeneration; }
        public String getLastTransitionTime() { return lastTransitionTime; }
        public String getReason() { return reason; }
        public String getMessage() { return message; }
    }

    @Override
    public String toString() {
        return "PodDisruptionBudget{" +
                "metadata=" + metadata +
                ", disruptionsAllowed=" + (status != null ? status.disruptionsAllowed : "null") +
                '}';
    }
}
