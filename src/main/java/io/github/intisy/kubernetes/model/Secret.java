package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a Kubernetes Secret.
 *
 * @author Finn Birich
 */
public class Secret {
    @SerializedName("apiVersion")
    private String apiVersion = "v1";

    @SerializedName("kind")
    private String kind = "Secret";

    @SerializedName("metadata")
    private ObjectMeta metadata;

    @SerializedName("data")
    private Map<String, String> data;

    @SerializedName("stringData")
    private Map<String, String> stringData;

    @SerializedName("type")
    private String type;

    public Secret() {}

    public String getApiVersion() { return apiVersion; }
    public String getKind() { return kind; }

    public ObjectMeta getMetadata() { return metadata; }
    public Secret setMetadata(ObjectMeta metadata) { this.metadata = metadata; return this; }

    public Map<String, String> getData() { return data; }
    public Secret setData(Map<String, String> data) { this.data = data; return this; }

    public Secret addData(String key, String value) {
        if (this.data == null) this.data = new HashMap<>();
        this.data.put(key, value);
        return this;
    }

    public Map<String, String> getStringData() { return stringData; }
    public Secret setStringData(Map<String, String> stringData) { this.stringData = stringData; return this; }

    public Secret addStringData(String key, String value) {
        if (this.stringData == null) this.stringData = new HashMap<>();
        this.stringData.put(key, value);
        return this;
    }

    public String getType() { return type; }
    public Secret setType(String type) { this.type = type; return this; }

    @Override
    public String toString() {
        return "Secret{" +
                "metadata=" + metadata +
                ", type='" + type + '\'' +
                '}';
    }
}
