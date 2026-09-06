package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.transport.Sec1EcKey;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Sec1EcKeyTest {
    private static final BigInteger P256_ORDER =
            new BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16);

    private static final BigInteger THROWAWAY_SCALAR =
            new BigInteger("7D90E8507589A81A65DEAB67D351ED1A8CD5F565402D8B3DA663F613DC21B9A6", 16);

    private static final BigInteger HIGHBIT_SCALAR =
            new BigInteger("FE9585217E7C72D2259ECAC1C04D3DF7E5458310282B1F1FA432653645074C53", 16);

    @Test
    void loadsASec1EcKeyThatPkcs8WouldReject() throws Exception {
        assertRecoversFixture("ec-sec1-throwaway.pem", THROWAWAY_SCALAR);
    }

    @Test
    void loadsASec1EcKeyWhoseScalarHasTheHighBitSet() throws Exception {
        assertRecoversFixture("ec-sec1-highbit.pem", HIGHBIT_SCALAR);
    }

    private void assertRecoversFixture(String resource, BigInteger expectedScalar) throws Exception {
        byte[] der = derFrom(resource);

        PrivateKey key = Sec1EcKey.toPrivateKey(der);

        assertNotNull(key);
        assertEquals("EC", key.getAlgorithm());
        ECPrivateKey ecKey = (ECPrivateKey) key;
        assertEquals(expectedScalar, ecKey.getS());
        assertEquals(256, ecKey.getParams().getCurve().getField().getFieldSize());
        assertEquals(P256_ORDER, ecKey.getParams().getOrder());
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
