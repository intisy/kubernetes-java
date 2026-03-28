package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Represents a Kubernetes PersistentVolumeClaim.
 *
 * @author Finn Birich
 */
public class PersistentVolumeClaim {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "PersistentVolumeClaim";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private PVCSpec spec;

    @SerializedName("status")
    private PVCStatus status;

    public PersistentVolumeClaim() {}

    public String getApiVersion() { return apiVersion; }
    public PersistentVolumeClaim setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public PersistentVolumeClaim setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public PVCSpec getSpec() { return spec; }
    public PersistentVolumeClaim setSpec(PVCSpec spec) { this.spec = spec; return this; }

    public PVCStatus getStatus() { return status; }

    public static class PVCSpec {
        @SerializedName("accessModes")
        private List<String> accessModes;

        @SerializedName("resources")
        private ResourceRequirements resources;

        @SerializedName("storageClassName")
        private String storageClassName;

        @SerializedName("volumeName")
        private String volumeName;

        @SerializedName("selector")
        private Deployment.LabelSelector selector;

        @SerializedName("volumeMode")
        private String volumeMode;

        public PVCSpec() {}

        public List<String> getAccessModes() { return accessModes; }
        public PVCSpec setAccessModes(List<String> accessModes) { this.accessModes = accessModes; return this; }

        public ResourceRequirements getResources() { return resources; }
        public PVCSpec setResources(ResourceRequirements resources) { this.resources = resources; return this; }

        public String getStorageClassName() { return storageClassName; }
        public PVCSpec setStorageClassName(String storageClassName) { this.storageClassName = storageClassName; return this; }

        public String getVolumeName() { return volumeName; }
        public PVCSpec setVolumeName(String volumeName) { this.volumeName = volumeName; return this; }

        public Deployment.LabelSelector getSelector() { return selector; }
        public PVCSpec setSelector(Deployment.LabelSelector selector) { this.selector = selector; return this; }

        public String getVolumeMode() { return volumeMode; }
        public PVCSpec setVolumeMode(String volumeMode) { this.volumeMode = volumeMode; return this; }
    }

    public static class ResourceRequirements {
        @SerializedName("limits")
        private Map<String, String> limits;

        @SerializedName("requests")
        private Map<String, String> requests;

        public Map<String, String> getLimits() { return limits; }
        public ResourceRequirements setLimits(Map<String, String> limits) { this.limits = limits; return this; }

        public Map<String, String> getRequests() { return requests; }
        public ResourceRequirements setRequests(Map<String, String> requests) { this.requests = requests; return this; }
    }

    public static class PVCStatus {
        @SerializedName("phase")
        private String phase;

        @SerializedName("accessModes")
        private List<String> accessModes;

        @SerializedName("capacity")
        private Map<String, String> capacity;

        public String getPhase() { return phase; }
        public List<String> getAccessModes() { return accessModes; }
        public Map<String, String> getCapacity() { return capacity; }
    }

    @Override
    public String toString() {
        return "PersistentVolumeClaim{" +
                "metadata=" + metadata +
                ", phase=" + (status != null ? status.phase : "null") +
                '}';
    }
}
