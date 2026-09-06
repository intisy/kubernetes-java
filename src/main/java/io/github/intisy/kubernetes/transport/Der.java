package io.github.intisy.kubernetes.transport;

/**
 * The smallest DER reader that can walk a SEC1 EC private key.
 *
 * @implNote deliberately not a general ASN.1 parser; it reads tag-length-value in order and
 * understands nothing about the structures it walks, which is all {@link Sec1EcKey} needs.
 */
final class Der {
    private final byte[] bytes;
    private int position;

    Der(byte[] bytes, int position) {
        this.bytes = bytes;
        this.position = position;
    }

    int position() {
        return position;
    }

    int readTag() {
        return bytes[position++] & 0xFF;
    }

    void expectTag(int expected) {
        int tag = readTag();
        if (tag != expected) {
            throw new IllegalArgumentException(
                    "expected DER tag 0x" + Integer.toHexString(expected)
                            + " but found 0x" + Integer.toHexString(tag));
        }
    }

    int readLength() {
        int first = bytes[position++] & 0xFF;
        if ((first & 0x80) == 0) {
            return first;
        }
        int byteCount = first & 0x7F;
        int length = 0;
        for (int i = 0; i < byteCount; i++) {
            length = (length << 8) | (bytes[position++] & 0xFF);
        }
        return length;
    }

    byte[] readBytes(int length) {
        byte[] value = new byte[length];
        System.arraycopy(bytes, position, value, 0, length);
        position += length;
        return value;
    }

    void skip(int length) {
        position += length;
    }
}
