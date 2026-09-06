package io.github.intisy.kubernetes.apply;

import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a kind to its plural resource name via the API server's discovery endpoints.
 *
 * @implNote Kind-to-plural comes from discovery rather than a hardcoded table so CRDs, which such
 * a table could never enumerate in advance, resolve the same way built-in kinds do. A discovery
 * response also lists subresources such as {@code pods/status} under the same kind as their
 * parent, so entries whose {@code name} contains a slash are skipped.
 */
public class ResourceResolver {
    private final KubernetesHttpClient httpClient;
    private final Map<String, Map<String, String>> pluralsByApiVersion = new HashMap<String, Map<String, String>>();

    public ResourceResolver(KubernetesHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public synchronized String resolvePlural(String apiVersion, String kind) throws IOException {
        Map<String, String> kindToPlural = pluralsByApiVersion.get(apiVersion);
        if (kindToPlural == null) {
            kindToPlural = discover(apiVersion);
            pluralsByApiVersion.put(apiVersion, kindToPlural);
        }
        String plural = kindToPlural.get(kind);
        if (plural == null) {
            throw new IllegalStateException("no resource for kind '" + kind
                    + "' found via discovery for apiVersion '" + apiVersion + "'");
        }
        return plural;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> discover(String apiVersion) throws IOException {
        String path = apiVersion.contains("/") ? "/apis/" + apiVersion : "/api/" + apiVersion;
        KubernetesResponse response = httpClient.get(path);
        if (!response.isSuccessful()) {
            throw new IllegalStateException("discovery for apiVersion '" + apiVersion
                    + "' failed with HTTP " + response.getStatusCode() + ": " + response.getBody());
        }

        Map<String, Object> body = httpClient.getGson().fromJson(response.getBody(), Map.class);
        Map<String, String> kindToPlural = new HashMap<String, String>();
        Object resources = body != null ? body.get("resources") : null;
        if (resources instanceof List) {
            for (Object entryObj : (List<Object>) resources) {
                Map<String, Object> entry = (Map<String, Object>) entryObj;
                Object name = entry.get("name");
                Object kind = entry.get("kind");
                if (name == null || kind == null || name.toString().contains("/")) {
                    continue;
                }
                kindToPlural.put(kind.toString(), name.toString());
            }
        }
        return kindToPlural;
    }
}
