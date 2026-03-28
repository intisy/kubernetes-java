package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a Kubernetes StatefulSet.
 *
 * @author Finn Birich
 */
public class StatefulSet {
    @SerializedName("apiVersion")
    private String apiVersion = "apps/v1";

    @SerializedName("kind")
    private String kind = "StatefulSet";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private StatefulSetSpec spec;

    @SerializedName("status")
    private StatefulSetStatus status;

    public StatefulSet() {}

    public String getApiVersion() { return apiVersion; }
    public StatefulSet setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public StatefulSet setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public StatefulSetSpec getSpec() { return spec; }
    public StatefulSet setSpec(StatefulSetSpec spec) { this.spec = spec; return this; }

    public StatefulSetStatus getStatus() { return status; }

    public static class StatefulSetSpec {
        @SerializedName("replicas")
        private Integer replicas;

        @SerializedName("selector")
        private Deployment.LabelSelector selector;

        @SerializedName("template")
        private Deployment.PodTemplateSpec template;

        @SerializedName("serviceName")
        private String serviceName;

        @SerializedName("podManagementPolicy")
        private String podManagementPolicy;

        @SerializedName("updateStrategy")
        private UpdateStrategy updateStrategy;

        @SerializedName("revisionHistoryLimit")
        private Integer revisionHistoryLimit;

        public StatefulSetSpec() {}

        public Integer getReplicas() { return replicas; }
        public StatefulSetSpec setReplicas(Integer replicas) { this.replicas = replicas; return this; }

        public Deployment.LabelSelector getSelector() { return selector; }
        public StatefulSetSpec setSelector(Deployment.LabelSelector selector) { this.selector = selector; return this; }

        public Deployment.PodTemplateSpec getTemplate() { return template; }
        public StatefulSetSpec setTemplate(Deployment.PodTemplateSpec template) { this.template = template; return this; }

        public String getServiceName() { return serviceName; }
        public StatefulSetSpec setServiceName(String serviceName) { this.serviceName = serviceName; return this; }

        public String getPodManagementPolicy() { return podManagementPolicy; }
        public StatefulSetSpec setPodManagementPolicy(String policy) { this.podManagementPolicy = policy; return this; }

        public UpdateStrategy getUpdateStrategy() { return updateStrategy; }
        public StatefulSetSpec setUpdateStrategy(UpdateStrategy updateStrategy) { this.updateStrategy = updateStrategy; return this; }

        public Integer getRevisionHistoryLimit() { return revisionHistoryLimit; }
        public StatefulSetSpec setRevisionHistoryLimit(Integer limit) { this.revisionHistoryLimit = limit; return this; }
    }

    public static class UpdateStrategy {
        @SerializedName("type")
        private String type;

        @SerializedName("rollingUpdate")
        private RollingUpdateStatefulSetStrategy rollingUpdate;

        public String getType() { return type; }
        public UpdateStrategy setType(String type) { this.type = type; return this; }

        public RollingUpdateStatefulSetStrategy getRollingUpdate() { return rollingUpdate; }
        public UpdateStrategy setRollingUpdate(RollingUpdateStatefulSetStrategy rollingUpdate) { this.rollingUpdate = rollingUpdate; return this; }
    }

    public static class RollingUpdateStatefulSetStrategy {
        @SerializedName("partition")
        private Integer partition;

        public Integer getPartition() { return partition; }
        public RollingUpdateStatefulSetStrategy setPartition(Integer partition) { this.partition = partition; return this; }
    }

    public static class StatefulSetStatus {
        @SerializedName("observedGeneration")
        private Long observedGeneration;

        @SerializedName("replicas")
        private Integer replicas;

        @SerializedName("readyReplicas")
        private Integer readyReplicas;

        @SerializedName("currentReplicas")
        private Integer currentReplicas;

        @SerializedName("updatedReplicas")
        private Integer updatedReplicas;

        @SerializedName("currentRevision")
        private String currentRevision;

        @SerializedName("updateRevision")
        private String updateRevision;

        @SerializedName("collisionCount")
        private Integer collisionCount;

        @SerializedName("conditions")
        private List<StatefulSetCondition> conditions;

        public Long getObservedGeneration() { return observedGeneration; }
        public Integer getReplicas() { return replicas; }
        public Integer getReadyReplicas() { return readyReplicas; }
        public Integer getCurrentReplicas() { return currentReplicas; }
        public Integer getUpdatedReplicas() { return updatedReplicas; }
        public String getCurrentRevision() { return currentRevision; }
        public String getUpdateRevision() { return updateRevision; }
        public Integer getCollisionCount() { return collisionCount; }
        public List<StatefulSetCondition> getConditions() { return conditions; }
    }

    public static class StatefulSetCondition {
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
        return "StatefulSet{" +
                "metadata=" + metadata +
                ", replicas=" + (spec != null ? spec.replicas : "null") +
                ", readyReplicas=" + (status != null ? status.readyReplicas : "null") +
                '}';
    }
}
