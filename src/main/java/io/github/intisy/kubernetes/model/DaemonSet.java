package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a Kubernetes DaemonSet.
 *
 * @author Finn Birich
 */
public class DaemonSet {
    @SerializedName("apiVersion")
    private String apiVersion = "apps/v1";

    @SerializedName("kind")
    private String kind = "DaemonSet";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private DaemonSetSpec spec;

    @SerializedName("status")
    private DaemonSetStatus status;

    public DaemonSet() {}

    public String getApiVersion() { return apiVersion; }
    public DaemonSet setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public DaemonSet setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public DaemonSetSpec getSpec() { return spec; }
    public DaemonSet setSpec(DaemonSetSpec spec) { this.spec = spec; return this; }

    public DaemonSetStatus getStatus() { return status; }

    public static class DaemonSetSpec {
        @SerializedName("selector")
        private Deployment.LabelSelector selector;

        @SerializedName("template")
        private Deployment.PodTemplateSpec template;

        @SerializedName("updateStrategy")
        private DaemonSetUpdateStrategy updateStrategy;

        @SerializedName("minReadySeconds")
        private Integer minReadySeconds;

        @SerializedName("revisionHistoryLimit")
        private Integer revisionHistoryLimit;

        public DaemonSetSpec() {}

        public Deployment.LabelSelector getSelector() { return selector; }
        public DaemonSetSpec setSelector(Deployment.LabelSelector selector) { this.selector = selector; return this; }

        public Deployment.PodTemplateSpec getTemplate() { return template; }
        public DaemonSetSpec setTemplate(Deployment.PodTemplateSpec template) { this.template = template; return this; }

        public DaemonSetUpdateStrategy getUpdateStrategy() { return updateStrategy; }
        public DaemonSetSpec setUpdateStrategy(DaemonSetUpdateStrategy strategy) { this.updateStrategy = strategy; return this; }

        public Integer getMinReadySeconds() { return minReadySeconds; }
        public DaemonSetSpec setMinReadySeconds(Integer seconds) { this.minReadySeconds = seconds; return this; }

        public Integer getRevisionHistoryLimit() { return revisionHistoryLimit; }
        public DaemonSetSpec setRevisionHistoryLimit(Integer limit) { this.revisionHistoryLimit = limit; return this; }
    }

    public static class DaemonSetUpdateStrategy {
        @SerializedName("type")
        private String type;

        @SerializedName("rollingUpdate")
        private RollingUpdateDaemonSet rollingUpdate;

        public String getType() { return type; }
        public DaemonSetUpdateStrategy setType(String type) { this.type = type; return this; }

        public RollingUpdateDaemonSet getRollingUpdate() { return rollingUpdate; }
        public DaemonSetUpdateStrategy setRollingUpdate(RollingUpdateDaemonSet rollingUpdate) { this.rollingUpdate = rollingUpdate; return this; }
    }

    public static class RollingUpdateDaemonSet {
        @SerializedName("maxUnavailable")
        private Object maxUnavailable;

        @SerializedName("maxSurge")
        private Object maxSurge;

        public Object getMaxUnavailable() { return maxUnavailable; }
        public RollingUpdateDaemonSet setMaxUnavailable(Object maxUnavailable) { this.maxUnavailable = maxUnavailable; return this; }

        public Object getMaxSurge() { return maxSurge; }
        public RollingUpdateDaemonSet setMaxSurge(Object maxSurge) { this.maxSurge = maxSurge; return this; }
    }

    public static class DaemonSetStatus {
        @SerializedName("currentNumberScheduled")
        private Integer currentNumberScheduled;

        @SerializedName("numberMisscheduled")
        private Integer numberMisscheduled;

        @SerializedName("desiredNumberScheduled")
        private Integer desiredNumberScheduled;

        @SerializedName("numberReady")
        private Integer numberReady;

        @SerializedName("updatedNumberScheduled")
        private Integer updatedNumberScheduled;

        @SerializedName("numberAvailable")
        private Integer numberAvailable;

        @SerializedName("numberUnavailable")
        private Integer numberUnavailable;

        @SerializedName("observedGeneration")
        private Long observedGeneration;

        @SerializedName("conditions")
        private List<DaemonSetCondition> conditions;

        public Integer getCurrentNumberScheduled() { return currentNumberScheduled; }
        public Integer getNumberMisscheduled() { return numberMisscheduled; }
        public Integer getDesiredNumberScheduled() { return desiredNumberScheduled; }
        public Integer getNumberReady() { return numberReady; }
        public Integer getUpdatedNumberScheduled() { return updatedNumberScheduled; }
        public Integer getNumberAvailable() { return numberAvailable; }
        public Integer getNumberUnavailable() { return numberUnavailable; }
        public Long getObservedGeneration() { return observedGeneration; }
        public List<DaemonSetCondition> getConditions() { return conditions; }
    }

    public static class DaemonSetCondition {
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
        return "DaemonSet{" +
                "metadata=" + metadata +
                ", desired=" + (status != null ? status.desiredNumberScheduled : "null") +
                ", ready=" + (status != null ? status.numberReady : "null") +
                '}';
    }
}
