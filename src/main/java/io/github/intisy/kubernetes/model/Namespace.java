package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a Kubernetes Namespace.
 *
 * @author Finn Birich
 */
public class Namespace {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "Namespace";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("spec")
    private NamespaceSpec spec;

    @SerializedName("status")
    private NamespaceStatus status;

    public Namespace() {}

    public String getApiVersion() { return apiVersion; }
    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public Namespace setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public NamespaceSpec getSpec() { return spec; }
    public NamespaceStatus getStatus() { return status; }

    public static class NamespaceSpec {
        @SerializedName("finalizers")
        private List<String> finalizers;

        public List<String> getFinalizers() { return finalizers; }
    }

    public static class NamespaceStatus {
        @SerializedName("phase")
        private String phase;

        public String getPhase() { return phase; }
    }

    @Override
    public String toString() {
        return "Namespace{" +
                "metadata=" + metadata +
                ", phase=" + (status != null ? status.phase : "null") +
                '}';
    }
}
