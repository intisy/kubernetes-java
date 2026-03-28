package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Kubernetes ClusterRole (RBAC).
 *
 * @author Finn Birich
 */
public class ClusterRole {
    @SerializedName("apiVersion")
    private String apiVersion = "rbac.authorization.k8s.io/v1";

    @SerializedName("kind")
    private String kind = "ClusterRole";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("rules")
    private List<Role.PolicyRule> rules;

    @SerializedName("aggregationRule")
    private AggregationRule aggregationRule;

    public ClusterRole() {}

    public String getApiVersion() { return apiVersion; }
    public ClusterRole setApiVersion(String apiVersion) { this.apiVersion = apiVersion; return this; }

    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public ClusterRole setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public List<Role.PolicyRule> getRules() { return rules; }
    public ClusterRole setRules(List<Role.PolicyRule> rules) { this.rules = rules; return this; }

    public ClusterRole addRule(Role.PolicyRule rule) {
        if (this.rules == null) this.rules = new ArrayList<Role.PolicyRule>();
        this.rules.add(rule);
        return this;
    }

    public AggregationRule getAggregationRule() { return aggregationRule; }
    public ClusterRole setAggregationRule(AggregationRule aggregationRule) { this.aggregationRule = aggregationRule; return this; }

    public static class AggregationRule {
        @SerializedName("clusterRoleSelectors")
        private List<Deployment.LabelSelector> clusterRoleSelectors;

        public List<Deployment.LabelSelector> getClusterRoleSelectors() { return clusterRoleSelectors; }
        public AggregationRule setClusterRoleSelectors(List<Deployment.LabelSelector> selectors) { this.clusterRoleSelectors = selectors; return this; }
    }

    @Override
    public String toString() {
        return "ClusterRole{" +
                "metadata=" + metadata +
                ", rulesCount=" + (rules != null ? rules.size() : 0) +
                '}';
    }
}
