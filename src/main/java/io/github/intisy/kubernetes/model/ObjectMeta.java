package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/**
 * Common metadata for all Kubernetes resources.
 *
 * @author Finn Birich
 */
public class ObjectMeta {
    @SerializedName("name")
    private String name;

    @SerializedName("generateName")
    private String generateName;

    @SerializedName("namespace")
    private String namespace;

    @SerializedName("selfLink")
    private String selfLink;

    @SerializedName("uid")
    private String uid;

    @SerializedName("resourceVersion")
    private String resourceVersion;

    @SerializedName("generation")
    private Long generation;

    @SerializedName("creationTimestamp")
    private String creationTimestamp;

    @SerializedName("deletionTimestamp")
    private String deletionTimestamp;

    @SerializedName("labels")
    private Map<String, String> labels;

    @SerializedName("annotations")
    private Map<String, String> annotations;

    @SerializedName("ownerReferences")
    private List<OwnerReference> ownerReferences;

    public ObjectMeta() {}

    public String getName() { return name; }
    public ObjectMeta setName(String name) { this.name = name; return this; }

    public String getGenerateName() { return generateName; }
    public ObjectMeta setGenerateName(String generateName) { this.generateName = generateName; return this; }

    public String getNamespace() { return namespace; }
    public ObjectMeta setNamespace(String namespace) { this.namespace = namespace; return this; }

    public String getSelfLink() { return selfLink; }
    public String getUid() { return uid; }
    public String getResourceVersion() { return resourceVersion; }
    public Long getGeneration() { return generation; }
    public String getCreationTimestamp() { return creationTimestamp; }
    public String getDeletionTimestamp() { return deletionTimestamp; }

    public Map<String, String> getLabels() { return labels; }
    public ObjectMeta setLabels(Map<String, String> labels) { this.labels = labels; return this; }

    public Map<String, String> getAnnotations() { return annotations; }
    public ObjectMeta setAnnotations(Map<String, String> annotations) { this.annotations = annotations; return this; }

    public List<OwnerReference> getOwnerReferences() { return ownerReferences; }

    /**
     * Owner reference for garbage collection.
     */
    public static class OwnerReference {
        @SerializedName("apiVersion")
        private String apiVersion;

        @SerializedName("kind")
        private String kind;

        @SerializedName("name")
        private String name;

        @SerializedName("uid")
        private String uid;

        @SerializedName("controller")
        private Boolean controller;

        @SerializedName("blockOwnerDeletion")
        private Boolean blockOwnerDeletion;

        public String getApiVersion() { return apiVersion; }
        public String getKind() { return kind; }
        public String getName() { return name; }
        public String getUid() { return uid; }
        public Boolean getController() { return controller; }
        public Boolean getBlockOwnerDeletion() { return blockOwnerDeletion; }
    }

    @Override
    public String toString() {
        return "ObjectMeta{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", uid='" + uid + '\'' +
                '}';
    }
}
