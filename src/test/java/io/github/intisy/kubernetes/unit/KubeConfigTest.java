package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.config.KubeConfig;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

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
}
