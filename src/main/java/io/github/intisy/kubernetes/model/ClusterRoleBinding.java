package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Kubernetes ClusterRoleBinding (RBAC).
 *
 * @author Finn Birich
 */
public class ClusterRoleBinding {
    @SerializedName("apiVersion")
    private String apiVersion = "rbac.authorization.k8s.io/v1";

    @SerializedName("kind")
    private String kind = "ClusterRoleBinding";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("subjects")
    private List<RoleBinding.Subject> subjects;

    @SerializedName("roleRef")
    private RoleBinding.RoleRef roleRef;

    public ClusterRoleBinding() {}

    public String getApiVersion() { return apiVersion; }
    public ClusterRoleBinding setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public ClusterRoleBinding setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public List<RoleBinding.Subject> getSubjects() { return subjects; }
    public ClusterRoleBinding setSubjects(List<RoleBinding.Subject> subjects) { this.subjects = subjects; return this; }

    public ClusterRoleBinding addSubject(RoleBinding.Subject subject) {
        if (this.subjects == null) this.subjects = new ArrayList<RoleBinding.Subject>();
        this.subjects.add(subject);
        return this;
    }

    public RoleBinding.RoleRef getRoleRef() { return roleRef; }
    public ClusterRoleBinding setRoleRef(RoleBinding.RoleRef roleRef) { this.roleRef = roleRef; return this; }

    @Override
    public String toString() {
        return "ClusterRoleBinding{" +
                "metadata=" + metadata +
                ", roleRef=" + (roleRef != null ? roleRef.getName() : "null") +
                '}';
    }
}
