package io.github.intisy.kubernetes.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Reads a kubeconfig and resolves the current context into the material a client needs.
 *
 * @implNote talos embeds ca, client certificate and client key as base64 PEM rather than as file
 * paths, so this exposes PEM content and never a path; both spellings are accepted because a
 * hand-written kubeconfig may still point at files.
 */
public final class KubeConfig {
    private final String server;
    private final String caCertPem;
    private final String clientCertPem;
    private final String clientKeyPem;

    private KubeConfig(String server, String caCertPem, String clientCertPem, String clientKeyPem) {
        this.server = server;
        this.caCertPem = caCertPem;
        this.clientCertPem = clientCertPem;
        this.clientKeyPem = clientKeyPem;
    }

    public static KubeConfig parse(Path file) throws IOException {
        return parseString(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    public static KubeConfig parseString(String yaml) {
        Map<String, Object> root = (Map<String, Object>) new Yaml().load(yaml);
        String currentContext = requireString(root, "current-context");

        Map<String, Object> context = named(root, "contexts", currentContext, "context");
        if (context == null) {
            throw new IllegalStateException("kubeconfig has no context named '" + currentContext + "'");
        }
        String clusterName = requireString(context, "cluster");
        String userName = requireString(context, "user");

        Map<String, Object> cluster = named(root, "clusters", clusterName, "cluster");
        if (cluster == null) {
            throw new IllegalStateException("kubeconfig has no cluster named '" + clusterName + "'");
        }
        Map<String, Object> user = named(root, "users", userName, "user");
        if (user == null) {
            throw new IllegalStateException("kubeconfig has no user named '" + userName + "'");
        }

        return new KubeConfig(
                requireString(cluster, "server"),
                material(cluster, "certificate-authority"),
                material(user, "client-certificate"),
                material(user, "client-key"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> named(Map<String, Object> root, String listKey, String name, String innerKey) {
        Object list = root.get(listKey);
        if (!(list instanceof List)) {
            return null;
        }
        for (Object element : (List<Object>) list) {
            Map<String, Object> entry = (Map<String, Object>) element;
            if (name.equals(entry.get("name"))) {
                return (Map<String, Object>) entry.get(innerKey);
            }
        }
        return null;
    }

    private static String material(Map<String, Object> holder, String key) {
        Object inline = holder.get(key + "-data");
        if (inline != null) {
            return new String(Base64.getDecoder().decode(inline.toString()), StandardCharsets.UTF_8);
        }
        Object path = holder.get(key);
        if (path == null) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(Paths.get(path.toString())), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("kubeconfig references '" + path + "' which cannot be read", e);
        }
    }

    private static String requireString(Map<String, Object> holder, String key) {
        Object value = holder.get(key);
        if (value == null) {
            throw new IllegalStateException("kubeconfig is missing '" + key + "'");
        }
        return value.toString();
    }

    public String server() {
        return server;
    }

    public String caCertPem() {
        return caCertPem;
    }

    public String clientCertPem() {
        return clientCertPem;
    }

    public String clientKeyPem() {
        return clientKeyPem;
    }
}
