package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Represents a Kubernetes PersistentVolume.
 *
 * @author Finn Birich
 */
public class PersistentVolume {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "PersistentVolume";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private PVSpec spec;

    @SerializedName("status")
    private PVStatus status;

    public PersistentVolume() {}

    public String getApiVersion() { return apiVersion; }
    public PersistentVolume setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public PersistentVolume setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public PVSpec getSpec() { return spec; }
    public PersistentVolume setSpec(PVSpec spec) { this.spec = spec; return this; }

    public PVStatus getStatus() { return status; }

    public static class PVSpec {
        @SerializedName("capacity")
        private Map<String, String> capacity;

        @SerializedName("accessModes")
        private List<String> accessModes;

        @SerializedName("persistentVolumeReclaimPolicy")
        private String persistentVolumeReclaimPolicy;

        @SerializedName("storageClassName")
        private String storageClassName;

        @SerializedName("volumeMode")
        private String volumeMode;

        @SerializedName("hostPath")
        private Pod.HostPathVolumeSource hostPath;

        @SerializedName("nfs")
        private NFSVolumeSource nfs;

        @SerializedName("claimRef")
        private ObjectReference claimRef;

        public PVSpec() {}

        public Map<String, String> getCapacity() { return capacity; }
        public PVSpec setCapacity(Map<String, String> capacity) { this.capacity = capacity; return this; }

        public List<String> getAccessModes() { return accessModes; }
        public PVSpec setAccessModes(List<String> accessModes) { this.accessModes = accessModes; return this; }

        public String getPersistentVolumeReclaimPolicy() { return persistentVolumeReclaimPolicy; }
        public PVSpec setPersistentVolumeReclaimPolicy(String policy) { this.persistentVolumeReclaimPolicy = policy; return this; }

        public String getStorageClassName() { return storageClassName; }
        public PVSpec setStorageClassName(String storageClassName) { this.storageClassName = storageClassName; return this; }

        public String getVolumeMode() { return volumeMode; }
        public PVSpec setVolumeMode(String volumeMode) { this.volumeMode = volumeMode; return this; }

        public Pod.HostPathVolumeSource getHostPath() { return hostPath; }
        public PVSpec setHostPath(Pod.HostPathVolumeSource hostPath) { this.hostPath = hostPath; return this; }

        public NFSVolumeSource getNfs() { return nfs; }
        public PVSpec setNfs(NFSVolumeSource nfs) { this.nfs = nfs; return this; }

        public ObjectReference getClaimRef() { return claimRef; }
        public PVSpec setClaimRef(ObjectReference claimRef) { this.claimRef = claimRef; return this; }
    }

    public static class NFSVolumeSource {
        @SerializedName("server")
        private String server;

        @SerializedName("path")
        private String path;

        @SerializedName("readOnly")
        private Boolean readOnly;

        public String getServer() { return server; }
        public NFSVolumeSource setServer(String server) { this.server = server; return this; }

        public String getPath() { return path; }
        public NFSVolumeSource setPath(String path) { this.path = path; return this; }

        public Boolean getReadOnly() { return readOnly; }
        public NFSVolumeSource setReadOnly(Boolean readOnly) { this.readOnly = readOnly; return this; }
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

        @SerializedName("apiVersion")
        private String apiVersion;

        public String getKind() { return kind; }
        public String getNamespace() { return namespace; }
        public String getName() { return name; }
        public String getUid() { return uid; }
        public String getApiVersion() { return apiVersion; }
    }

    public static class PVStatus {
        @SerializedName("phase")
        private String phase;

        @SerializedName("message")
        private String message;

        @SerializedName("reason")
        private String reason;

        public String getPhase() { return phase; }
        public String getMessage() { return message; }
        public String getReason() { return reason; }
    }

    @Override
    public String toString() {
        return "PersistentVolume{" +
                "metadata=" + metadata +
                ", phase=" + (status != null ? status.phase : "null") +
                '}';
    }
}
