package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a Kubernetes Event.
 *
 * @author Finn Birich
 */
public class Event {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "Event";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("involvedObject")
    private ObjectReference involvedObject;

    @SerializedName("reason")
    private String reason;

    @SerializedName("message")
    private String message;

    @SerializedName("source")
    private EventSource source;

    @SerializedName("firstTimestamp")
    private String firstTimestamp;

    @SerializedName("lastTimestamp")
    private String lastTimestamp;

    @SerializedName("count")
    private Integer count;

    @SerializedName("type")
    private String type;

    @SerializedName("action")
    private String action;

    @SerializedName("reportingComponent")
    private String reportingComponent;

    public Event() {}

    public String getApiVersion() { return apiVersion; }
    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public Event setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public ObjectReference getInvolvedObject() { return involvedObject; }
    public String getReason() { return reason; }
    public String getMessage() { return message; }
    public EventSource getSource() { return source; }
    public String getFirstTimestamp() { return firstTimestamp; }
    public String getLastTimestamp() { return lastTimestamp; }
    public Integer getCount() { return count; }
    public String getType() { return type; }
    public String getAction() { return action; }
    public String getReportingComponent() { return reportingComponent; }

    public static class ObjectReference {
        @SerializedName("kind")
        private String kind;

        @SerializedName("namespace")
        private String namespace;

        @SerializedName("name")
        private String name;

        @SerializedName("uid")
        private String uid;

        @SerializedName("apiVersion")
        private String apiVersion;

        @SerializedName("resourceVersion")
        private String resourceVersion;

        @SerializedName("fieldPath")
        private String fieldPath;

        public String getKind() { return kind; }
        public String getNamespace() { return namespace; }
        public String getName() { return name; }
        public String getUid() { return uid; }
        public String getApiVersion() { return apiVersion; }
        public String getResourceVersion() { return resourceVersion; }
        public String getFieldPath() { return fieldPath; }
    }

    public static class EventSource {
        @SerializedName("component")
        private String component;

        @SerializedName("host")
        private String host;

        public String getComponent() { return component; }
        public String getHost() { return host; }
    }

    @Override
    public String toString() {
        return "Event{" +
                "metadata=" + metadata +
                ", reason='" + reason + '\'' +
                ", message='" + message + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
