package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Kubernetes RoleBinding (RBAC).
 *
 * @author Finn Birich
 */
public class RoleBinding {
    @SerializedName("apiVersion")
    private String apiVersion = "rbac.authorization.k8s.io/v1";

    @SerializedName("kind")
    private String kind = "RoleBinding";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("subjects")
    private List<Subject> subjects;

    @SerializedName("roleRef")
    private RoleRef roleRef;

    public RoleBinding() {}

    public String getApiVersion() { return apiVersion; }
    public RoleBinding setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public RoleBinding setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public List<Subject> getSubjects() { return subjects; }
    public RoleBinding setSubjects(List<Subject> subjects) { this.subjects = subjects; return this; }

    public RoleBinding addSubject(Subject subject) {
        if (this.subjects == null) this.subjects = new ArrayList<Subject>();
        this.subjects.add(subject);
        return this;
    }

    public RoleRef getRoleRef() { return roleRef; }
    public RoleBinding setRoleRef(RoleRef roleRef) { this.roleRef = roleRef; return this; }

    /**
     * Subject contains a reference to the object or user identities a role binding applies to.
     * Shared by RoleBinding and ClusterRoleBinding.
     */
    public static class Subject {
        @SerializedName("kind")
        private String kind;

        @SerializedName("apiGroup")
        private String apiGroup;

        @SerializedName("name")
        private String name;

        @SerializedName("namespace")
        private String namespace;

        public Subject() {}

        public String getKind() { return kind; }
        public Subject setKind(String kind) { this.kind = kind; return this; }

        public String getApiGroup() { return apiGroup; }
        public Subject setApiGroup(String apiGroup) { this.apiGroup = apiGroup; return this; }

        public String getName() { return name; }
        public Subject setName(String name) { this.name = name; return this; }

        public String getNamespace() { return namespace; }
        public Subject setNamespace(String namespace) { this.namespace = namespace; return this; }
    }

    /**
     * RoleRef contains information that points to the role being used.
     * Shared by RoleBinding and ClusterRoleBinding.
     */
    public static class RoleRef {
        @SerializedName("apiGroup")
        private String apiGroup;

        @SerializedName("kind")
        private String kind;

        @SerializedName("name")
        private String name;

        public RoleRef() {}

        public String getApiGroup() { return apiGroup; }
        public RoleRef setApiGroup(String apiGroup) { this.apiGroup = apiGroup; return this; }

        public String getKind() { return kind; }
        public RoleRef setKind(String kind) { this.kind = kind; return this; }

        public String getName() { return name; }
        public RoleRef setName(String name) { this.name = name; return this; }
    }

    @Override
    public String toString() {
        return "RoleBinding{" +
                "metadata=" + metadata +
                ", roleRef=" + (roleRef != null ? roleRef.name : "null") +
                '}';
    }
}
