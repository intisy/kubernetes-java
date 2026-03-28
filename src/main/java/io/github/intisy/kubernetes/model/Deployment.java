package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Kubernetes Deployment.
 *
 * @author Finn Birich
 */
public class Deployment {
    @SerializedName("apiVersion")
    private String apiVersion = "apps/v1";

    @SerializedName("kind")
    private String kind = "Deployment";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private DeploymentSpec spec;

    @SerializedName("status")
    private DeploymentStatus status;

    public Deployment() {}

    public String getApiVersion() { return apiVersion; }
    public Deployment setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public Deployment setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public DeploymentSpec getSpec() { return spec; }
    public Deployment setSpec(DeploymentSpec spec) { this.spec = spec; return this; }

    public DeploymentStatus getStatus() { return status; }

    /**
     * Deployment specification.
     */
    public static class DeploymentSpec {
        @SerializedName("replicas")
        private Integer replicas;

        @SerializedName("selector")
        private LabelSelector selector;

        @SerializedName("template")
        private PodTemplateSpec template;

        @SerializedName("strategy")
        private DeploymentStrategy strategy;

        @SerializedName("minReadySeconds")
        private Integer minReadySeconds;

        @SerializedName("revisionHistoryLimit")
        private Integer revisionHistoryLimit;

        @SerializedName("paused")
        private Boolean paused;

        public DeploymentSpec() {}

        public Integer getReplicas() { return replicas; }
        public DeploymentSpec setReplicas(Integer replicas) { this.replicas = replicas; return this; }

        public LabelSelector getSelector() { return selector; }
        public DeploymentSpec setSelector(LabelSelector selector) { this.selector = selector; return this; }

        public PodTemplateSpec getTemplate() { return template; }
        public DeploymentSpec setTemplate(PodTemplateSpec template) { this.template = template; return this; }

        public DeploymentStrategy getStrategy() { return strategy; }
        public DeploymentSpec setStrategy(DeploymentStrategy strategy) { this.strategy = strategy; return this; }

        public Integer getMinReadySeconds() { return minReadySeconds; }
        public DeploymentSpec setMinReadySeconds(Integer minReadySeconds) { this.minReadySeconds = minReadySeconds; return this; }

        public Integer getRevisionHistoryLimit() { return revisionHistoryLimit; }
        public DeploymentSpec setRevisionHistoryLimit(Integer revisionHistoryLimit) { this.revisionHistoryLimit = revisionHistoryLimit; return this; }

        public Boolean getPaused() { return paused; }
        public DeploymentSpec setPaused(Boolean paused) { this.paused = paused; return this; }
    }

    /**
     * Label selector for matching pods.
     */
    public static class LabelSelector {
        @SerializedName("matchLabels")
        private Map<String, String> matchLabels;

        public LabelSelector() {}

        public Map<String, String> getMatchLabels() { return matchLabels; }
        public LabelSelector setMatchLabels(Map<String, String> matchLabels) { this.matchLabels = matchLabels; return this; }

        public LabelSelector addMatchLabel(String key, String value) {
            if (this.matchLabels == null) this.matchLabels = new HashMap<>();
            this.matchLabels.put(key, value);
            return this;
        }
    }

    /**
     * Pod template specification.
     */
    public static class PodTemplateSpec {
        @SerializedName("metadata")
        private ObjectMeta metadata;

        @SerializedName("spec")
        private Pod.PodSpec spec;

        public PodTemplateSpec() {}

        public ObjectMeta getMetadata() { return metadata; }
        public PodTemplateSpec setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

        public Pod.PodSpec getSpec() { return spec; }
        public PodTemplateSpec setSpec(Pod.PodSpec spec) { this.spec = spec; return this; }
    }

    /**
     * Deployment strategy.
     */
    public static class DeploymentStrategy {
        @SerializedName("type")
        private String type;

        @SerializedName("rollingUpdate")
        private RollingUpdateDeployment rollingUpdate;

        public String getType() { return type; }
        public DeploymentStrategy setType(String type) { this.type = type; return this; }

        public RollingUpdateDeployment getRollingUpdate() { return rollingUpdate; }
        public DeploymentStrategy setRollingUpdate(RollingUpdateDeployment rollingUpdate) { this.rollingUpdate = rollingUpdate; return this; }
    }

    public static class RollingUpdateDeployment {
        @SerializedName("maxUnavailable")
        private Object maxUnavailable;

        @SerializedName("maxSurge")
        private Object maxSurge;

        public Object getMaxUnavailable() { return maxUnavailable; }
        public RollingUpdateDeployment setMaxUnavailable(Object maxUnavailable) { this.maxUnavailable = maxUnavailable; return this; }

        public Object getMaxSurge() { return maxSurge; }
        public RollingUpdateDeployment setMaxSurge(Object maxSurge) { this.maxSurge = maxSurge; return this; }
    }

    /**
     * Deployment status.
     */
    public static class DeploymentStatus {
        @SerializedName("observedGeneration")
        private Long observedGeneration;

        @SerializedName("replicas")
        private Integer replicas;

        @SerializedName("updatedReplicas")
        private Integer updatedReplicas;

        @SerializedName("readyReplicas")
        private Integer readyReplicas;

        @SerializedName("availableReplicas")
        private Integer availableReplicas;

        @SerializedName("unavailableReplicas")
        private Integer unavailableReplicas;

        @SerializedName("conditions")
        private List<DeploymentCondition> conditions;

        public Long getObservedGeneration() { return observedGeneration; }
        public Integer getReplicas() { return replicas; }
        public Integer getUpdatedReplicas() { return updatedReplicas; }
        public Integer getReadyReplicas() { return readyReplicas; }
        public Integer getAvailableReplicas() { return availableReplicas; }
        public Integer getUnavailableReplicas() { return unavailableReplicas; }
        public List<DeploymentCondition> getConditions() { return conditions; }
    }

    public static class DeploymentCondition {
        @SerializedName("type")
        private String type;

        @SerializedName("status")
        private String status;

        @SerializedName("lastUpdateTime")
        private String lastUpdateTime;

        @SerializedName("lastTransitionTime")
        private String lastTransitionTime;

        @SerializedName("reason")
        private String reason;

        @SerializedName("message")
        private String message;

        public String getType() { return type; }
        public String getStatus() { return status; }
        public String getLastUpdateTime() { return lastUpdateTime; }
        public String getLastTransitionTime() { return lastTransitionTime; }
        public String getReason() { return reason; }
        public String getMessage() { return message; }
    }

    @Override
    public String toString() {
        return "Deployment{" +
                "metadata=" + metadata +
                ", replicas=" + (spec != null ? spec.replicas : "null") +
                ", readyReplicas=" + (status != null ? status.readyReplicas : "null") +
                '}';
    }
}
