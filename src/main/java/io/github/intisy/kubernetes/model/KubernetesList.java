package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Generic list response for Kubernetes resources.
 *
 * @param <T> The type of items in the list
 * @author Finn Birich
 */
public class KubernetesList<T> {
    @SerializedName("apiVersion")
    private String apiVersion;

    @SerializedName("kind")
    private String kind;

    @SerializedName("metadata")
    private ListMeta metadata;

    @SerializedName("items")
    private List<T> items;

    public String getApiVersion() { return apiVersion; }
    public String getKind() { return kind; }
    public ListMeta getMetadata() { return metadata; }
    public List<T> getItems() { return items; }

    public static class ListMeta {
        @SerializedName("selfLink")
        private String selfLink;

        @SerializedName("resourceVersion")
        private String resourceVersion;

        @SerializedName("continue")
        private String continueToken;

        public String getSelfLink() { return selfLink; }
        public String getResourceVersion() { return resourceVersion; }
        public String getContinueToken() { return continueToken; }
    }

    @Override
    public String toString() {
        return "KubernetesList{" +
                "kind='" + kind + '\'' +
                ", items=" + (items != null ? items.size() : 0) +
                '}';
    }
}
