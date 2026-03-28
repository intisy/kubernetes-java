package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a Kubernetes Job.
 *
 * @author Finn Birich
 */
public class Job {
    @SerializedName("apiVersion")
    private String apiVersion = "batch/v1";

    @SerializedName("kind")
    private String kind = "Job";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private JobSpec spec;

    @SerializedName("status")
    private JobStatus status;

    public Job() {}

    public String getApiVersion() { return apiVersion; }
    public Job setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public Job setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public JobSpec getSpec() { return spec; }
    public Job setSpec(JobSpec spec) { this.spec = spec; return this; }

    public JobStatus getStatus() { return status; }

    public static class JobSpec {
        @SerializedName("parallelism")
        private Integer parallelism;

        @SerializedName("completions")
        private Integer completions;

        @SerializedName("activeDeadlineSeconds")
        private Long activeDeadlineSeconds;

        @SerializedName("backoffLimit")
        private Integer backoffLimit;

        @SerializedName("selector")
        private Deployment.LabelSelector selector;

        @SerializedName("template")
        private Deployment.PodTemplateSpec template;

        @SerializedName("ttlSecondsAfterFinished")
        private Integer ttlSecondsAfterFinished;

        public JobSpec() {}

        public Integer getParallelism() { return parallelism; }
        public JobSpec setParallelism(Integer parallelism) { this.parallelism = parallelism; return this; }

        public Integer getCompletions() { return completions; }
        public JobSpec setCompletions(Integer completions) { this.completions = completions; return this; }

        public Long getActiveDeadlineSeconds() { return activeDeadlineSeconds; }
        public JobSpec setActiveDeadlineSeconds(Long activeDeadlineSeconds) { this.activeDeadlineSeconds = activeDeadlineSeconds; return this; }

        public Integer getBackoffLimit() { return backoffLimit; }
        public JobSpec setBackoffLimit(Integer backoffLimit) { this.backoffLimit = backoffLimit; return this; }

        public Deployment.LabelSelector getSelector() { return selector; }
        public JobSpec setSelector(Deployment.LabelSelector selector) { this.selector = selector; return this; }

        public Deployment.PodTemplateSpec getTemplate() { return template; }
        public JobSpec setTemplate(Deployment.PodTemplateSpec template) { this.template = template; return this; }

        public Integer getTtlSecondsAfterFinished() { return ttlSecondsAfterFinished; }
        public JobSpec setTtlSecondsAfterFinished(Integer ttlSecondsAfterFinished) { this.ttlSecondsAfterFinished = ttlSecondsAfterFinished; return this; }
    }

    public static class JobStatus {
        @SerializedName("conditions")
        private List<JobCondition> conditions;

        @SerializedName("startTime")
        private String startTime;

        @SerializedName("completionTime")
        private String completionTime;

        @SerializedName("active")
        private Integer active;

        @SerializedName("succeeded")
        private Integer succeeded;

        @SerializedName("failed")
        private Integer failed;

        public List<JobCondition> getConditions() { return conditions; }
        public String getStartTime() { return startTime; }
        public String getCompletionTime() { return completionTime; }
        public Integer getActive() { return active; }
        public Integer getSucceeded() { return succeeded; }
        public Integer getFailed() { return failed; }
    }

    public static class JobCondition {
        @SerializedName("type")
        private String type;

        @SerializedName("status")
        private String status;

        @SerializedName("lastProbeTime")
        private String lastProbeTime;

        @SerializedName("lastTransitionTime")
        private String lastTransitionTime;

        @SerializedName("reason")
        private String reason;

        @SerializedName("message")
        private String message;

        public String getType() { return type; }
        public String getStatus() { return status; }
        public String getLastProbeTime() { return lastProbeTime; }
        public String getLastTransitionTime() { return lastTransitionTime; }
        public String getReason() { return reason; }
        public String getMessage() { return message; }
    }

    @Override
    public String toString() {
        return "Job{" +
                "metadata=" + metadata +
                ", active=" + (status != null ? status.active : "null") +
                ", succeeded=" + (status != null ? status.succeeded : "null") +
                '}';
    }
}
