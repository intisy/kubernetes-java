package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Represents a Kubernetes StorageClass.
 *
 * @author Finn Birich
 */
public class StorageClass {
    @SerializedName("apiVersion")
    private String apiVersion = "storage.k8s.io/v1";

    @SerializedName("kind")
    private String kind = "StorageClass";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("provisioner")
    private String provisioner;

    @SerializedName("parameters")
    private Map<String, String> parameters;

    @SerializedName("reclaimPolicy")
    private String reclaimPolicy;

    @SerializedName("mountOptions")
    private List<String> mountOptions;

    @SerializedName("allowVolumeExpansion")
    private Boolean allowVolumeExpansion;

    @SerializedName("volumeBindingMode")
    private String volumeBindingMode;

    public StorageClass() {}

    public String getApiVersion() { return apiVersion; }
    public StorageClass setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public StorageClass setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public String getProvisioner() { return provisioner; }
    public StorageClass setProvisioner(String provisioner) { this.provisioner = provisioner; return this; }

    public Map<String, String> getParameters() { return parameters; }
    public StorageClass setParameters(Map<String, String> parameters) { this.parameters = parameters; return this; }

    public String getReclaimPolicy() { return reclaimPolicy; }
    public StorageClass setReclaimPolicy(String reclaimPolicy) { this.reclaimPolicy = reclaimPolicy; return this; }

    public List<String> getMountOptions() { return mountOptions; }
    public StorageClass setMountOptions(List<String> mountOptions) { this.mountOptions = mountOptions; return this; }

    public Boolean getAllowVolumeExpansion() { return allowVolumeExpansion; }
    public StorageClass setAllowVolumeExpansion(Boolean allow) { this.allowVolumeExpansion = allow; return this; }

    public String getVolumeBindingMode() { return volumeBindingMode; }
    public StorageClass setVolumeBindingMode(String mode) { this.volumeBindingMode = mode; return this; }

    @Override
    public String toString() {
        return "StorageClass{" +
                "metadata=" + metadata +
                ", provisioner='" + provisioner + '\'' +
                '}';
    }
}
