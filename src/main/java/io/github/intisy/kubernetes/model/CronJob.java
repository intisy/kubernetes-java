package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a Kubernetes CronJob.
 *
 * @author Finn Birich
 */
public class CronJob {
    @SerializedName("apiVersion")
    private String apiVersion = "batch/v1";

    @SerializedName("kind")
    private String kind = "CronJob";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private CronJobSpec spec;

    @SerializedName("status")
    private CronJobStatus status;

    public CronJob() {}

    public String getApiVersion() { return apiVersion; }
    public CronJob setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public CronJob setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public CronJobSpec getSpec() { return spec; }
    public CronJob setSpec(CronJobSpec spec) { this.spec = spec; return this; }

    public CronJobStatus getStatus() { return status; }

    public static class CronJobSpec {
        @SerializedName("schedule")
        private String schedule;

        @SerializedName("concurrencyPolicy")
        private String concurrencyPolicy;

        @SerializedName("suspend")
        private Boolean suspend;

        @SerializedName("jobTemplate")
        private JobTemplateSpec jobTemplate;

        @SerializedName("successfulJobsHistoryLimit")
        private Integer successfulJobsHistoryLimit;

        @SerializedName("failedJobsHistoryLimit")
        private Integer failedJobsHistoryLimit;

        @SerializedName("startingDeadlineSeconds")
        private Long startingDeadlineSeconds;

        public CronJobSpec() {}

        public String getSchedule() { return schedule; }
        public CronJobSpec setSchedule(String schedule) { this.schedule = schedule; return this; }

        public String getConcurrencyPolicy() { return concurrencyPolicy; }
        public CronJobSpec setConcurrencyPolicy(String concurrencyPolicy) { this.concurrencyPolicy = concurrencyPolicy; return this; }

        public Boolean getSuspend() { return suspend; }
        public CronJobSpec setSuspend(Boolean suspend) { this.suspend = suspend; return this; }

        public JobTemplateSpec getJobTemplate() { return jobTemplate; }
        public CronJobSpec setJobTemplate(JobTemplateSpec jobTemplate) { this.jobTemplate = jobTemplate; return this; }

        public Integer getSuccessfulJobsHistoryLimit() { return successfulJobsHistoryLimit; }
        public CronJobSpec setSuccessfulJobsHistoryLimit(Integer limit) { this.successfulJobsHistoryLimit = limit; return this; }

        public Integer getFailedJobsHistoryLimit() { return failedJobsHistoryLimit; }
        public CronJobSpec setFailedJobsHistoryLimit(Integer limit) { this.failedJobsHistoryLimit = limit; return this; }

        public Long getStartingDeadlineSeconds() { return startingDeadlineSeconds; }
        public CronJobSpec setStartingDeadlineSeconds(Long seconds) { this.startingDeadlineSeconds = seconds; return this; }
    }

    public static class JobTemplateSpec {
        @SerializedName("metadata")
        private ObjectMeta metadata;

        @SerializedName("spec")
        private Job.JobSpec spec;

        public JobTemplateSpec() {}

        public ObjectMeta getMetadata() { return metadata; }
        public JobTemplateSpec setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

        public Job.JobSpec getSpec() { return spec; }
        public JobTemplateSpec setSpec(Job.JobSpec spec) { this.spec = spec; return this; }
    }

    public static class CronJobStatus {
        @SerializedName("active")
        private List<ObjectReference> active;

        @SerializedName("lastScheduleTime")
        private String lastScheduleTime;

        @SerializedName("lastSuccessfulTime")
        private String lastSuccessfulTime;

        public List<ObjectReference> getActive() { return active; }
        public String getLastScheduleTime() { return lastScheduleTime; }
        public String getLastSuccessfulTime() { return lastSuccessfulTime; }
    }

    public static class ObjectReference {
        @SerializedName("kind")
        private String kind;

        @SerializedName("namespace")
        private String namespace;

        @SerializedName("name")
        private String name;

        @SerializedName("uid")
        private String uid;

        public String getKind() { return kind; }
        public String getNamespace() { return namespace; }
        public String getName() { return name; }
        public String getUid() { return uid; }
    }

    @Override
    public String toString() {
        return "CronJob{" +
                "metadata=" + metadata +
                ", schedule=" + (spec != null ? spec.schedule : "null") +
                '}';
    }
}
