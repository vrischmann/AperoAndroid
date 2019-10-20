package fr.rischmann.apero

import java.security.SecureRandom
import java.time.Instant

data class Entry(val id: ULID, val content: String) {
    companion object {
        public final val ALL = listOf(
            Entry(ULID.random(Instant.now().toEpochMilli(), SecureRandom()), "foobar"),
            Entry(ULID.random(Instant.now().toEpochMilli(), SecureRandom()), "barbaz")
        )
    }
}
