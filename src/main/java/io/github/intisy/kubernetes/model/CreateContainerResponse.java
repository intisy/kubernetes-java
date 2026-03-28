package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response from creating a container (used for compatibility with docker-java pattern).
 *
 * @author Finn Birich
 */
public class CreateContainerResponse {
    @SerializedName("metadata")
    private ObjectMeta metadata;

    public String getName() {
        return metadata != null ? metadata.getName() : null;
    }

    public String getUid() {
        return metadata != null ? metadata.getUid() : null;
    }

    public ObjectMeta getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "CreateContainerResponse{" +
                "name='" + getName() + '\'' +
                ", uid='" + getUid() + '\'' +
                '}';
    }
}
