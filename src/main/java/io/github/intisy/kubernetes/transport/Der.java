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
        require(1);
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
        require(1);
        int first = bytes[position++] & 0xFF;
        if ((first & 0x80) == 0) {
            return first;
        }
        int byteCount = first & 0x7F;
        require(byteCount);
        int length = 0;
        for (int i = 0; i < byteCount; i++) {
            length = (length << 8) | (bytes[position++] & 0xFF);
        }
        if (length < 0) {
            throw new IllegalArgumentException("malformed DER: length field at offset "
                    + (position - byteCount - 1) + " does not fit in an int");
        }
        return length;
    }

    byte[] readBytes(int length) {
        require(length);
        byte[] value = new byte[length];
        System.arraycopy(bytes, position, value, 0, length);
        position += length;
        return value;
    }

    void skip(int length) {
        require(length);
        position += length;
    }

    /**
     * @implNote every read goes through here so truncated input fails with a message naming the
     * offset, instead of the ArrayIndexOutOfBoundsException the raw array access used to throw.
     * That exception forced callers wanting a clean error to catch RuntimeException broadly, which
     * would have swallowed unrelated bugs along with it.
     */
    private void require(int count) {
        if (count < 0 || count > bytes.length - position) {
            throw new IllegalArgumentException("malformed DER: " + count + " byte(s) needed at offset "
                    + position + " but only " + (bytes.length - position) + " remain");
        }
    }
}
