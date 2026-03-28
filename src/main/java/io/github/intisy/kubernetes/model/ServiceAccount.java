package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a Kubernetes ServiceAccount.
 *
 * @author Finn Birich
 */
public class ServiceAccount {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "ServiceAccount";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("secrets")
    private List<ObjectReference> secrets;

    @SerializedName("automountServiceAccountToken")
    private Boolean automountServiceAccountToken;

    public ServiceAccount() {}

    public String getApiVersion() { return apiVersion; }
    public ServiceAccount setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public ServiceAccount setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public List<ObjectReference> getSecrets() { return secrets; }
    public ServiceAccount setSecrets(List<ObjectReference> secrets) { this.secrets = secrets; return this; }

    public Boolean getAutomountServiceAccountToken() { return automountServiceAccountToken; }
    public ServiceAccount setAutomountServiceAccountToken(Boolean automount) { this.automountServiceAccountToken = automount; return this; }

    public static class ObjectReference {
        @SerializedName("kind")
        private String kind;

        @SerializedName("namespace")
        private String namespace;

        @SerializedName("name")
        private String name;

        public String getKind() { return kind; }
        public String getNamespace() { return namespace; }
        public String getName() { return name; }
    }

    @Override
    public String toString() {
        return "ServiceAccount{" +
                "metadata=" + metadata +
                '}';
    }
}
