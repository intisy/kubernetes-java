package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.KubernetesClient;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TlsFailureTest {
    private static final String GARBAGE_CERT =
            "-----BEGIN CERTIFICATE-----\nnot a certificate\n-----END CERTIFICATE-----";
    private static final String GARBAGE_EC_KEY =
            "-----BEGIN EC PRIVATE KEY-----\nnot a key\n-----END EC PRIVATE KEY-----";

    @Test
    void throwsWhenTheCaCertCannotBeParsed() throws Exception {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                KubernetesClient.builder()
                        .withApiServer("https://example.invalid:6443")
                        .withCaCertPem(GARBAGE_CERT)
                        .withClientCertPem(fixture("tls-cert.pem"))
                        .withClientKeyPem(fixture("tls-key-sec1.pem"))
                        .build());

        assertTrue(thrown.getMessage().contains("TLS"),
                "expected the failure to name TLS, got: " + thrown.getMessage());
    }

    @Test
    void throwsWhenTheClientCertCannotBeParsed() throws Exception {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                KubernetesClient.builder()
                        .withApiServer("https://example.invalid:6443")
                        .withCaCertPem(fixture("tls-cert.pem"))
                        .withClientCertPem(GARBAGE_CERT)
                        .withClientKeyPem(fixture("tls-key-sec1.pem"))
                        .build());

        assertTrue(thrown.getMessage().contains("TLS"),
                "expected the failure to name TLS, got: " + thrown.getMessage());
    }

    @Test
    void throwsWhenTheClientKeyCannotBeParsed() throws Exception {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                KubernetesClient.builder()
                        .withApiServer("https://example.invalid:6443")
                        .withCaCertPem(fixture("tls-cert.pem"))
                        .withClientCertPem(fixture("tls-cert.pem"))
                        .withClientKeyPem(GARBAGE_EC_KEY)
                        .build());

        assertTrue(thrown.getMessage().contains("TLS"),
                "expected the failure to name TLS, got: " + thrown.getMessage());
    }

    @Test
    void succeedsWithAValidPkcs8ClientKey() throws Exception {
        KubernetesClient client = assertDoesNotThrow(() ->
                KubernetesClient.builder()
                        .withApiServer("https://example.invalid:6443")
                        .withCaCertPem(fixture("tls-cert.pem"))
                        .withClientCertPem(fixture("tls-cert.pem"))
                        .withClientKeyPem(fixture("tls-key-pkcs8.pem"))
                        .build());

        assertTrue(client != null);
    }

    @Test
    void allowsTrustAllToBypassCaVerification() throws Exception {
        KubernetesClient client = assertDoesNotThrow(() ->
                KubernetesClient.builder()
                        .withApiServer("https://example.invalid:6443")
                        .withCaCertPem(GARBAGE_CERT)
                        .withInsecureTrustAll(true)
                        .build());

        assertTrue(client != null);
    }

    private static String fixture(String resource) throws Exception {
        try (InputStream in = TlsFailureTest.class.getClassLoader().getResourceAsStream(resource)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
