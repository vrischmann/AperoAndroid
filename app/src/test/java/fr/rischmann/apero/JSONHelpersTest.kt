package fr.rischmann.apero

import fr.rischmann.ulid.ULID
import org.junit.Assert.assertEquals
import org.junit.Test

class JSONHelpersTest {
    data class WithULID(val foo: ULID)

    @Test
    fun ulid() {
        val jsonData = "{\"foo\": \"01DQKAB1JGXYHT5EDBBFVTM1Z9\"}"
        val temp = JSONHelpers.objectMapper.readValue(jsonData, WithULID::class.java)

        val s = "01DQKAB1JGXYHT5EDBBFVTM1Z9"
        val exp = ULID.fromString(s)

        assertEquals(exp, temp.foo)
    }
}