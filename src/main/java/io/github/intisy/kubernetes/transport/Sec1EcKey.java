package io.github.intisy.kubernetes.transport;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;

/**
 * Loads a SEC1 {@code EC PRIVATE KEY}, which {@code PKCS8EncodedKeySpec} rejects.
 *
 * @implNote talosctl writes SEC1, so without this the client silently loses its client
 * certificate; see the phase 1b-2 design section 4.2.
 */
public final class Sec1EcKey {
    private static final int TAG_SEQUENCE = 0x30;
    private static final int TAG_INTEGER = 0x02;
    private static final int TAG_OCTET_STRING = 0x04;
    private static final int TAG_OID = 0x06;
    private static final int TAG_PARAMETERS = 0xA0;

    public static PrivateKey toPrivateKey(byte[] sec1Der) throws GeneralSecurityException {
        Der der = new Der(sec1Der, 0);
        der.expectTag(TAG_SEQUENCE);
        int sequenceLength = der.readLength();
        int end = der.position() + sequenceLength;

        der.expectTag(TAG_INTEGER);
        der.skip(der.readLength());

        der.expectTag(TAG_OCTET_STRING);
        byte[] scalar = der.readBytes(der.readLength());

        String curveOid = null;
        while (der.position() < end) {
            int tag = der.readTag();
            int length = der.readLength();
            if (tag == TAG_PARAMETERS) {
                Der parameters = new Der(der.readBytes(length), 0);
                parameters.expectTag(TAG_OID);
                curveOid = decodeOid(parameters.readBytes(parameters.readLength()));
            } else {
                der.skip(length);
            }
        }
        if (curveOid == null) {
            throw new GeneralSecurityException("SEC1 EC key carries no named curve");
        }

        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec(curveOid));
        ECParameterSpec curve = parameters.getParameterSpec(ECParameterSpec.class);

        return KeyFactory.getInstance("EC")
                .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, scalar), curve));
    }

    static String decodeOid(byte[] encoded) {
        StringBuilder oid = new StringBuilder();
        int first = encoded[0] & 0xFF;
        oid.append(first / 40).append('.').append(first % 40);
        long value = 0;
        for (int i = 1; i < encoded.length; i++) {
            int b = encoded[i] & 0xFF;
            value = (value << 7) | (b & 0x7F);
            if ((b & 0x80) == 0) {
                oid.append('.').append(value);
                value = 0;
            }
        }
        return oid.toString();
    }

    private Sec1EcKey() {}
}
