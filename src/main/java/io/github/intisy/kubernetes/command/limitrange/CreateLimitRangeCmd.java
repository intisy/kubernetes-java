package io.github.intisy.kubernetes.command.limitrange;

import io.github.intisy.kubernetes.exception.ConflictException;
import io.github.intisy.kubernetes.exception.KubernetesException;
import io.github.intisy.kubernetes.model.LimitRange;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;

/**
 * Command to create a limit range.
 *
 * @author Finn Birich
 */
public class CreateLimitRangeCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private LimitRange limitRange;

    public CreateLimitRangeCmd(KubernetesHttpClient client, LimitRange limitRange) {
        this.client = client;
        this.limitRange = limitRange;
    }

    public CreateLimitRangeCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public LimitRange exec() {
        try {
            String ns = namespace;
            if (limitRange.getMetadata() != null && limitRange.getMetadata().getNamespace() != null) {
                ns = limitRange.getMetadata().getNamespace();
            }
            String path = "/api/v1/namespaces/" + ns + "/limitranges";
            KubernetesResponse response = client.post(path, limitRange);

            if (response.getStatusCode() == 409) {
                throw new ConflictException("LimitRange already exists");
            }
            if (!response.isSuccessful()) {
                throw new KubernetesException("Failed to create limit range: " + response.getBody(), response.getStatusCode());
            }

            return client.getGson().fromJson(response.getBody(), LimitRange.class);
        } catch (IOException e) {
            throw new KubernetesException("Failed to create limit range", e);
        }
    }
}
