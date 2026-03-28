package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a Kubernetes ReplicaSet.
 *
 * @author Finn Birich
 */
public class ReplicaSet {
    @SerializedName("apiVersion")
    private String apiVersion = "apps/v1";

    @SerializedName("kind")
    private String kind = "ReplicaSet";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private ReplicaSetSpec spec;

    @SerializedName("status")
    private ReplicaSetStatus status;

    public ReplicaSet() {}

    public String getApiVersion() { return apiVersion; }
    public ReplicaSet setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public ReplicaSet setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public ReplicaSetSpec getSpec() { return spec; }
    public ReplicaSet setSpec(ReplicaSetSpec spec) { this.spec = spec; return this; }

    public ReplicaSetStatus getStatus() { return status; }

    public static class ReplicaSetSpec {
        @SerializedName("replicas")
        private Integer replicas;

        @SerializedName("minReadySeconds")
        private Integer minReadySeconds;

        @SerializedName("selector")
        private Deployment.LabelSelector selector;

        @SerializedName("template")
        private Deployment.PodTemplateSpec template;

        public ReplicaSetSpec() {}

        public Integer getReplicas() { return replicas; }
        public ReplicaSetSpec setReplicas(Integer replicas) { this.replicas = replicas; return this; }

        public Integer getMinReadySeconds() { return minReadySeconds; }
        public ReplicaSetSpec setMinReadySeconds(Integer seconds) { this.minReadySeconds = seconds; return this; }

        public Deployment.LabelSelector getSelector() { return selector; }
        public ReplicaSetSpec setSelector(Deployment.LabelSelector selector) { this.selector = selector; return this; }

        public Deployment.PodTemplateSpec getTemplate() { return template; }
        public ReplicaSetSpec setTemplate(Deployment.PodTemplateSpec template) { this.template = template; return this; }
    }

    public static class ReplicaSetStatus {
        @SerializedName("replicas")
        private Integer replicas;

        @SerializedName("fullyLabeledReplicas")
        private Integer fullyLabeledReplicas;

        @SerializedName("readyReplicas")
        private Integer readyReplicas;

        @SerializedName("availableReplicas")
        private Integer availableReplicas;

        @SerializedName("observedGeneration")
        private Long observedGeneration;

        @SerializedName("conditions")
        private List<ReplicaSetCondition> conditions;

        public Integer getReplicas() { return replicas; }
        public Integer getFullyLabeledReplicas() { return fullyLabeledReplicas; }
        public Integer getReadyReplicas() { return readyReplicas; }
        public Integer getAvailableReplicas() { return availableReplicas; }
        public Long getObservedGeneration() { return observedGeneration; }
        public List<ReplicaSetCondition> getConditions() { return conditions; }
    }

    public static class ReplicaSetCondition {
        @SerializedName("type")
        private String type;

        @SerializedName("status")
        private String status;

        @SerializedName("lastTransitionTime")
        private String lastTransitionTime;

        @SerializedName("reason")
        private String reason;

        @SerializedName("message")
        private String message;

        public String getType() { return type; }
        public String getStatus() { return status; }
        public String getLastTransitionTime() { return lastTransitionTime; }
        public String getReason() { return reason; }
        public String getMessage() { return message; }
    }

    @Override
    public String toString() {
        return "ReplicaSet{" +
                "metadata=" + metadata +
                ", replicas=" + (spec != null ? spec.replicas : "null") +
                ", readyReplicas=" + (status != null ? status.readyReplicas : "null") +
                '}';
    }
}
