package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.transport.Sec1EcKey;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Sec1EcKeyTest {
    @Test
    void loadsASec1EcKeyThatPkcs8WouldReject() throws Exception {
        byte[] der = derFrom("ec-sec1-throwaway.pem");

        PrivateKey key = Sec1EcKey.toPrivateKey(der);

        assertNotNull(key);
        assertEquals("EC", key.getAlgorithm());
    }

    private byte[] derFrom(String resource) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            String pem = new String(out.toByteArray(), StandardCharsets.UTF_8);
            String base64 = pem.replaceAll("-----BEGIN [A-Z ]+-----", "")
                    .replaceAll("-----END [A-Z ]+-----", "")
                    .replaceAll("\\s", "");
            return Base64.getDecoder().decode(base64);
        }
    }
}
