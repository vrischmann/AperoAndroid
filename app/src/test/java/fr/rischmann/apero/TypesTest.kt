package fr.rischmann.apero

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class TypesTest {
    @Test
    fun copyRequest() {
        val mapper = jacksonObjectMapper()

        val signature = Base64.getDecoder()
            .decode("FKb/5UmHzLNe3HNaYDkrFhKc2myn+612hSQNeeERSCLZZ6bEIhoLe0XXejScAuNhzS6bxe3HguP0F6ikpRxIBw==")
        val content = Base64.getDecoder()
            .decode("Y4UrYkVGJwJev1hIzgtfFKohhcp1aUyUMUk5cRLpcNNJT9gv8hK3a1AhdHSD")

        val req = Types.CopyRequest(signature, content)

        val s = mapper.writeValueAsString(req)

        val exp =
            "{\"signature\":\"FKb/5UmHzLNe3HNaYDkrFhKc2myn+612hSQNeeERSCLZZ6bEIhoLe0XXejScAuNhzS6bxe3HguP0F6ikpRxIBw==\",\"content\":\"Y4UrYkVGJwJev1hIzgtfFKohhcp1aUyUMUk5cRLpcNNJT9gv8hK3a1AhdHSD\"}"
        assertEquals(exp, s)
    }
}
