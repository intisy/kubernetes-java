package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.transport.Sec1EcKey;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /**
     * @implNote truncation is checked at three depths because each one used to fail on a different
     * raw array access, and an ArrayIndexOutOfBoundsException from any of them forced a caller
     * wanting a clean error to catch RuntimeException broadly, which would swallow real bugs too.
     */
    @Test
    void refusesTruncatedDerWithAMessageNamingTheOffset() throws Exception {
        byte[] whole = derFrom("ec-sec1-throwaway.pem");

        for (int keep : new int[]{1, 5, whole.length - 1}) {
            byte[] truncated = Arrays.copyOf(whole, keep);

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    () -> Sec1EcKey.toPrivateKey(truncated),
                    "expected truncation after " + keep + " byte(s) to be reported as malformed input,"
                            + " not to escape as an array index failure");
            assertTrue(thrown.getMessage().startsWith("malformed DER: "),
                    "expected a DER-specific message, got: " + thrown.getMessage());
        }
    }

    /**
     * @implNote the curve is an OPTIONAL element of SEC1, so a key without one is well-formed DER
     * and reaches the check rather than failing as garbage. Built here rather than carved out of a
     * fixture, because a hand-built key is the shortest thing that is unambiguously valid apart
     * from the one missing element.
     */
    @Test
    void refusesAWellFormedKeyThatNamesNoCurve() {
        byte[] scalar = new byte[32];
        Arrays.fill(scalar, (byte) 0x2a);
        byte[] sec1WithoutCurve = new byte[2 + 3 + 2 + scalar.length];
        int at = 0;
        sec1WithoutCurve[at++] = 0x30;
        sec1WithoutCurve[at++] = (byte) (3 + 2 + scalar.length);
        sec1WithoutCurve[at++] = 0x02;
        sec1WithoutCurve[at++] = 0x01;
        sec1WithoutCurve[at++] = 0x01;
        sec1WithoutCurve[at++] = 0x04;
        sec1WithoutCurve[at++] = (byte) scalar.length;
        System.arraycopy(scalar, 0, sec1WithoutCurve, at, scalar.length);

        GeneralSecurityException thrown = assertThrows(GeneralSecurityException.class,
                () -> Sec1EcKey.toPrivateKey(sec1WithoutCurve));

        assertEquals("SEC1 EC key carries no named curve", thrown.getMessage());
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
