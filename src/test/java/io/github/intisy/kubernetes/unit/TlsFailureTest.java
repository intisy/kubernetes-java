package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.KubernetesClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TlsFailureTest {
    @Test
    void throwsInsteadOfSilentlyRunningInsecureWhenTheClientKeyIsUnusable() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                KubernetesClient.builder()
                        .withApiServer("https://example.invalid:6443")
                        .withCaCertPem("-----BEGIN CERTIFICATE-----\nnot a certificate\n-----END CERTIFICATE-----")
                        .withClientCertPem("-----BEGIN CERTIFICATE-----\nnot a certificate\n-----END CERTIFICATE-----")
                        .withClientKeyPem("-----BEGIN EC PRIVATE KEY-----\nnot a key\n-----END EC PRIVATE KEY-----")
                        .build());

        assertTrue(thrown.getMessage().contains("TLS"),
                "expected the failure to name TLS, got: " + thrown.getMessage());
    }

    @Test
    void allowsTrustAllOnlyWhenItIsAskedForExplicitly() {
        KubernetesClient client = KubernetesClient.builder()
                .withApiServer("https://example.invalid:6443")
                .withInsecureTrustAll(true)
                .build();

        assertTrue(client != null);
    }
}
