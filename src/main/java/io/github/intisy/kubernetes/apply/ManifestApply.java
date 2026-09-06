package io.github.intisy.kubernetes.apply;

import io.github.intisy.kubernetes.transport.KubernetesHttpClient;
import io.github.intisy.kubernetes.transport.KubernetesResponse;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies Kubernetes manifests server-side, the replacement for {@code kubectl apply -f}.
 *
 * @implNote Each document keeps its original YAML text as the request body; snakeyaml is used
 * only to read {@code apiVersion}, {@code kind} and {@code metadata} for path resolution, never
 * to re-serialise the manifest, since a round-tripped document can silently drop or reorder
 * fields the cluster cares about.
 */
public class ManifestApply {
    public static final String CONTENT_TYPE = "application/apply-patch+yaml";

    private final KubernetesHttpClient httpClient;
    private final ResourceResolver resourceResolver;

    public ManifestApply(KubernetesHttpClient httpClient, ResourceResolver resourceResolver) {
        this.httpClient = httpClient;
        this.resourceResolver = resourceResolver;
    }

    public static Map<String, String> applyQuery() {
        Map<String, String> query = new LinkedHashMap<String, String>();
        query.put("fieldManager", "kubernetes-java");
        query.put("force", "true");
        return query;
    }

    public static String resourcePath(String apiVersion, String plural, String namespace, String name) {
        StringBuilder path = new StringBuilder();
        int slash = apiVersion.indexOf('/');
        if (slash >= 0) {
            path.append("/apis/").append(apiVersion, 0, slash).append('/').append(apiVersion.substring(slash + 1));
        } else {
            path.append("/api/").append(apiVersion);
        }
        if (namespace != null) {
            path.append("/namespaces/").append(namespace);
        }
        path.append('/').append(plural).append('/').append(name);
        return path.toString();
    }

    public static List<Document> documents(String yaml) {
        List<Document> documents = new ArrayList<Document>();
        Yaml snakeYaml = new Yaml();
        for (String text : splitOnDocumentSeparator(yaml)) {
            if (text.trim().isEmpty()) {
                continue;
            }
            Object loaded = snakeYaml.load(text);
            if (!(loaded instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> root = (Map<String, Object>) loaded;
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) root.get("metadata");
            documents.add(new Document(
                    stringOf(root.get("apiVersion")),
                    stringOf(root.get("kind")),
                    metadata != null ? stringOf(metadata.get("namespace")) : null,
                    metadata != null ? stringOf(metadata.get("name")) : null,
                    text));
        }
        return documents;
    }

    public void applyYaml(String yaml) throws IOException {
        for (Document document : documents(yaml)) {
            apply(document);
        }
    }

    private void apply(Document document) throws IOException {
        String plural = resourceResolver.resolvePlural(document.apiVersion(), document.kind());
        String path = resourcePath(document.apiVersion(), plural, document.namespace(), document.name());
        KubernetesResponse response = httpClient.patch(path, applyQuery(), document.yaml(), CONTENT_TYPE);
        if (!response.isSuccessful()) {
            throw new IllegalStateException("failed to apply " + document.kind() + "/" + document.name()
                    + " at " + path + ": HTTP " + response.getStatusCode() + ": " + response.getBody());
        }
    }

    private static String stringOf(Object value) {
        return value != null ? value.toString() : null;
    }

    private static List<String> splitOnDocumentSeparator(String yaml) {
        List<String> parts = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        for (String line : yaml.split("\n", -1)) {
            if (line.trim().equals("---")) {
                parts.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(line).append('\n');
            }
        }
        parts.add(current.toString());
        return parts;
    }

    public static final class Document {
        private final String apiVersion;
        private final String kind;
        private final String namespace;
        private final String name;
        private final String yaml;

        public Document(String apiVersion, String kind, String namespace, String name, String yaml) {
            this.apiVersion = apiVersion;
            this.kind = kind;
            this.namespace = namespace;
            this.name = name;
            this.yaml = yaml;
        }

        public String apiVersion() {
            return apiVersion;
        }

        public String kind() {
            return kind;
        }

        public String namespace() {
            return namespace;
        }

        public String name() {
            return name;
        }

        public String yaml() {
            return yaml;
        }
    }
}
