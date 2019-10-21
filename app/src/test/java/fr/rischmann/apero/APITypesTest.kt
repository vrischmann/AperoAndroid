package fr.rischmann.apero

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fr.rischmann.apero.TestHelpers.b64
import org.junit.Assert.assertEquals
import org.junit.Test

class APITypesTest {
    @Test
    fun copyRequest() {
        val mapper = jacksonObjectMapper()

        val signature = b64("FKb/5UmHzLNe3HNaYDkrFhKc2myn+612hSQNeeERSCLZZ6bEIhoLe0XXejScAuNhzS6bxe3HguP0F6ikpRxIBw==")
        val content = b64("Y4UrYkVGJwJev1hIzgtfFKohhcp1aUyUMUk5cRLpcNNJT9gv8hK3a1AhdHSD")

        val req = APITypes.CopyRequest(signature, content)
        val result = mapper.writeValueAsString(req)

        val exp =
            "{\"signature\":\"FKb/5UmHzLNe3HNaYDkrFhKc2myn+612hSQNeeERSCLZZ6bEIhoLe0XXejScAuNhzS6bxe3HguP0F6ikpRxIBw==\",\"content\":\"Y4UrYkVGJwJev1hIzgtfFKohhcp1aUyUMUk5cRLpcNNJT9gv8hK3a1AhdHSD\"}"
        assertEquals(exp, result)
    }
}
