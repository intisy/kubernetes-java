package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.config.KubeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KubeConfigTest {
    private String fixture(String name) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void readsServerAndInlinePemFromTheCurrentContext() throws Exception {
        KubeConfig config = KubeConfig.parseString(fixture("kubeconfig-talos.yaml"));

        assertEquals("https://172.23.115.149:6443", config.server());
        assertEquals("CA-PEM", config.caCertPem());
        assertEquals("CERT-PEM", config.clientCertPem());
        assertEquals("KEY-PEM", config.clientKeyPem());
    }

    @Test
    void failsLoudlyWhenTheCurrentContextIsMissing() throws Exception {
        String withoutContext = fixture("kubeconfig-talos.yaml")
                .replace("current-context: admin@talos-default", "current-context: nope");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> KubeConfig.parseString(withoutContext));
        assertEquals("kubeconfig has no context named 'nope'", thrown.getMessage());
    }

    @Test
    void readsMaterialFromAFilePathWhenItIsNotInlined(@TempDir Path directory) throws Exception {
        Path caFile = directory.resolve("ca.pem");
        Files.write(caFile, "CA-FROM-FILE".getBytes(StandardCharsets.UTF_8));
        String withPath = fixture("kubeconfig-talos.yaml")
                .replace("certificate-authority-data: Q0EtUEVN",
                        "certificate-authority: " + caFile.toString().replace("\\", "/"));

        KubeConfig config = KubeConfig.parseString(withPath);

        assertEquals("CA-FROM-FILE", config.caCertPem());
        assertEquals("CERT-PEM", config.clientCertPem());
    }

    @Test
    void failsLoudlyWhenAReferencedFileCannotBeRead(@TempDir Path directory) throws Exception {
        String absent = directory.resolve("missing.pem").toString().replace("\\", "/");
        String withPath = fixture("kubeconfig-talos.yaml")
                .replace("certificate-authority-data: Q0EtUEVN", "certificate-authority: " + absent);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> KubeConfig.parseString(withPath));
        assertEquals("kubeconfig references '" + absent + "' which cannot be read", thrown.getMessage());
    }

    /**
     * @implNote a duplicate name makes the file ambiguous about which credential the context means,
     * and picking one silently can hand a client the wrong certificate for the wrong cluster.
     */
    @Test
    void failsLoudlyWhenTwoClustersShareAName() throws Exception {
        String duplicated = fixture("kubeconfig-talos.yaml")
                .replace("clusters:", "clusters:\n- cluster:\n    server: https://impostor:6443\n"
                        + "  name: talos-default");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> KubeConfig.parseString(duplicated));
        assertEquals("kubeconfig has more than one entry named 'talos-default' under 'clusters'",
                thrown.getMessage());
    }

    @Test
    void failsLoudlyWhenTheReferencedClusterIsMissing() throws Exception {
        String withoutCluster = fixture("kubeconfig-talos.yaml")
                .replace("cluster: talos-default", "cluster: nope");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> KubeConfig.parseString(withoutCluster));
        assertEquals("kubeconfig has no cluster named 'nope'", thrown.getMessage());
    }

    @Test
    void failsLoudlyWhenTheReferencedUserIsMissing() throws Exception {
        String withoutUser = fixture("kubeconfig-talos.yaml")
                .replace("user: admin@talos-default", "user: nope");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> KubeConfig.parseString(withoutUser));
        assertEquals("kubeconfig has no user named 'nope'", thrown.getMessage());
    }
}
