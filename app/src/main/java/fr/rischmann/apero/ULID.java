package fr.rischmann.apero;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;

public class ULID {
    private final byte[] data = new byte[16];

    private void setRandom(byte[] data) {
        System.arraycopy(data, 0, this.data, 6, data.length);
    }

    public long timestamp() {
        long l = data[5] & 0xFF;
        long l1 = ((long) data[4] & 0xFF) << 8;
        long l2 = ((long) data[3] & 0xFF) << 16;
        long l3 = ((long) data[2] & 0xFF) << 24;
        long l4 = ((long) data[1] & 0xFF) << 32;
        long l5 = ((long) data[0] & 0xFF) << 40;
        return l | l1 | l2 | l3 | l4 | l5;
    }

    private void setTimestamp(long ms) {
        data[0] = (byte) ((ms >> 40) & 0xFF);
        data[1] = (byte) ((ms >> 32) & 0xFF);
        data[2] = (byte) ((ms >> 24) & 0xFF);
        data[3] = (byte) ((ms >> 16) & 0xFF);
        data[4] = (byte) ((ms >> 8) & 0xFF);
        data[5] = (byte) (ms & 0xFF);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ULID ulid = (ULID) o;
        return Arrays.equals(data, ulid.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @NotNull
    @Override
    public String toString() {
        byte[] dst = new byte[ENCODED_SIZE];

        dst[0] = (byte) ALPHABET[(data[0] & 224) >> 5];
        dst[1] = (byte) ALPHABET[data[0] & 31];
        dst[2] = (byte) ALPHABET[(data[1] & 248) >> 3];
        dst[3] = (byte) ALPHABET[((data[1] & 7) << 2) | ((data[2] & 192) >> 6)];
        dst[4] = (byte) ALPHABET[(data[2] & 62) >> 1];
        dst[5] = (byte) ALPHABET[((data[2] & 1) << 4) | ((data[3] & 240) >> 4)];
        dst[6] = (byte) ALPHABET[((data[3] & 15) << 1) | ((data[4] & 128) >> 7)];
        dst[7] = (byte) ALPHABET[(data[4] & 124) >> 2];
        dst[8] = (byte) ALPHABET[((data[4] & 3) << 3) | ((data[5] & 224) >> 5)];
        dst[9] = (byte) ALPHABET[data[5] & 31];

        // 16 bytes of entropy
        dst[10] = (byte) ALPHABET[(data[6] & 248) >> 3];
        dst[11] = (byte) ALPHABET[((data[6] & 7) << 2) | ((data[7] & 192) >> 6)];
        dst[12] = (byte) ALPHABET[(data[7] & 62) >> 1];
        dst[13] = (byte) ALPHABET[((data[7] & 1) << 4) | ((data[8] & 240) >> 4)];
        dst[14] = (byte) ALPHABET[((data[8] & 15) << 1) | ((data[9] & 128) >> 7)];
        dst[15] = (byte) ALPHABET[(data[9] & 124) >> 2];
        dst[16] = (byte) ALPHABET[((data[9] & 3) << 3) | ((data[10] & 224) >> 5)];
        dst[17] = (byte) ALPHABET[data[10] & 31];
        dst[18] = (byte) ALPHABET[(data[11] & 248) >> 3];
        dst[19] = (byte) ALPHABET[((data[11] & 7) << 2) | ((data[12] & 192) >> 6)];
        dst[20] = (byte) ALPHABET[(data[12] & 62) >> 1];
        dst[21] = (byte) ALPHABET[((data[12] & 1) << 4) | ((data[13] & 240) >> 4)];
        dst[22] = (byte) ALPHABET[((data[13] & 15) << 1) | ((data[14] & 128) >> 7)];
        dst[23] = (byte) ALPHABET[(data[14] & 124) >> 2];
        dst[24] = (byte) ALPHABET[((data[14] & 3) << 3) | ((data[15] & 224) >> 5)];
        dst[25] = (byte) ALPHABET[data[15] & 31];

        return new String(dst, Charset.forName("UTF-8"));
    }

    public static ULID fromString(String s) {
        if (s.length() != ENCODED_SIZE) {
            throw new IllegalArgumentException("invalid ULID string size");
        }

        if (s.charAt(0) > '7') {
            throw new IllegalArgumentException("ULID will overflow");
        }

        for (int i = 0; i < ENCODED_SIZE; i++) {
            if (dec[s.charAt(i)] == 0xFF) {
                throw new IllegalArgumentException("invalid characters in ULID string");
            }
        }

        ULID id = new ULID();

        // 6 bytes timestamp (48 bits)
        id.data[0] = (byte) ((dec[s.charAt(0)] << 5) | dec[s.charAt(1)]);
        id.data[1] = (byte) ((dec[s.charAt(2)] << 3) | (dec[s.charAt(3)] >> 2));
        id.data[2] = (byte) ((dec[s.charAt(3)] << 6) | (dec[s.charAt(4)] << 1) | (dec[s.charAt(5)] >> 4));
        id.data[3] = (byte) ((dec[s.charAt(5)] << 4) | (dec[s.charAt(6)] >> 1));
        id.data[4] = (byte) ((dec[s.charAt(6)] << 7) | (dec[s.charAt(7)] << 2) | (dec[s.charAt(8)] >> 3));
        id.data[5] = (byte) ((dec[s.charAt(8)] << 5) | dec[s.charAt(9)]);

        // 10 bytes of entropy (80 bits)
        id.data[6] = (byte) ((dec[s.charAt(10)] << 3) | (dec[s.charAt(11)] >> 2));
        id.data[7] = (byte) ((dec[s.charAt(11)] << 6) | (dec[s.charAt(12)] << 1) | (dec[s.charAt(13)] >> 4));
        id.data[8] = (byte) ((dec[s.charAt(13)] << 4) | (dec[s.charAt(14)] >> 1));
        id.data[9] = (byte) ((dec[s.charAt(14)] << 7) | (dec[s.charAt(15)] << 2) | (dec[s.charAt(16)] >> 3));
        id.data[10] = (byte) ((dec[s.charAt(16)] << 5) | dec[s.charAt(17)]);
        id.data[11] = (byte) ((dec[s.charAt(18)] << 3) | dec[s.charAt(19)] >> 2);
        id.data[12] = (byte) ((dec[s.charAt(19)] << 6) | (dec[s.charAt(20)] << 1) | (dec[s.charAt(21)] >> 4));
        id.data[13] = (byte) ((dec[s.charAt(21)] << 4) | (dec[s.charAt(22)] >> 1));
        id.data[14] = (byte) ((dec[s.charAt(22)] << 7) | (dec[s.charAt(23)] << 2) | (dec[s.charAt(24)] >> 3));
        id.data[15] = (byte) ((dec[s.charAt(24)] << 5) | dec[s.charAt(25)]);

        return id;
    }

    public static ULID random(long timestamp, SecureRandom random) {
        byte[] randomBytes = new byte[10];
        random.nextBytes(randomBytes);

        ULID id = new ULID();
        id.setTimestamp(timestamp);
        id.setRandom(randomBytes);

        return id;
    }

    private static final char[] ALPHABET = new char[]{
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
            'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X', 'Y', 'Z'
    };

    static final int ENCODED_SIZE = 26;

    // Byte to index table for O(1) lookups when parsing.
    // 0xFF is a sentinel value for invalid indexes.
    private static final int[] dec = new int[]{
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x00, 0x01,
            0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E,
            0x0F, 0x10, 0x11, 0xFF, 0x12, 0x13, 0xFF, 0x14, 0x15, 0xFF,
            0x16, 0x17, 0x18, 0x19, 0x1A, 0xFF, 0x1B, 0x1C, 0x1D, 0x1E,
            0x1F, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0x0A, 0x0B, 0x0C,
            0x0D, 0x0E, 0x0F, 0x10, 0x11, 0xFF, 0x12, 0x13, 0xFF, 0x14,
            0x15, 0xFF, 0x16, 0x17, 0x18, 0x19, 0x1A, 0xFF, 0x1B, 0x1C,
            0x1D, 0x1E, 0x1F, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
    };
}
