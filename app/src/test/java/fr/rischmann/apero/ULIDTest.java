package fr.rischmann.apero;

import org.junit.Test;

import java.security.SecureRandom;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ULIDTest {
    @Test
    public void ulidParse() {
        String s = "01DQKAB1JGXYHT5EDBBFVTM1Z9";

        ULID id = ULID.fromString(s);
        assertNotNull(id);
        assertEquals(s, id.toString());

        long timestamp = id.timestamp();
        long expected = 1571532670544L;
        assertEquals(expected, timestamp);

        Instant time = Instant.ofEpochMilli(timestamp);
        assertEquals("2019-10-20T00:51:10.544Z", time.toString());
    }

    @Test
    public void ulidRandom() {
        Instant time = Instant.now();
        long timestamp = time.toEpochMilli();

        ULID id = ULID.random(timestamp, new SecureRandom());
        assertNotNull(id);

        String s = id.toString();
        assertEquals(ULID.ENCODED_SIZE, s.length());

        ULID id2 = ULID.fromString(s);
        assertEquals(id2, id);

        Instant time2 = Instant.ofEpochMilli(id.timestamp());
        assertEquals(time2, time);
    }
}
