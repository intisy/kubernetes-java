package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Kubernetes ConfigMap.
 *
 * @author Finn Birich
 */
public class ConfigMap {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "ConfigMap";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("data")
    private Map<String, String> data;

    @SerializedName("binaryData")
    private Map<String, String> binaryData;

    public ConfigMap() {}

    public String getApiVersion() { return apiVersion; }
    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public ConfigMap setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public Map<String, String> getData() { return data; }
    public ConfigMap setData(Map<String, String> data) { this.data = data; return this; }

    public ConfigMap addData(String key, String value) {
        if (this.data == null) this.data = new HashMap<>();
        this.data.put(key, value);
        return this;
    }

    public Map<String, String> getBinaryData() { return binaryData; }
    public ConfigMap setBinaryData(Map<String, String> binaryData) { this.binaryData = binaryData; return this; }

    @Override
    public String toString() {
        return "ConfigMap{" +
                "metadata=" + metadata +
                ", dataKeys=" + (data != null ? data.keySet() : "null") +
                '}';
    }
}
