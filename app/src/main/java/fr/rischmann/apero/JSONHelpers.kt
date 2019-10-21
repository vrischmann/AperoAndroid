package fr.rischmann.apero

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fr.rischmann.ulid.ULID

object JSONHelpers {
    val objectMapper = jacksonObjectMapper()

    init {
        val module = SimpleModule("apero")

        module.addSerializer(ULID::class.java, ULIDSerializer())
        module.addDeserializer(ULID::class.java, ULIDDeserializer())

        objectMapper.registerModule(module)
    }

    class ULIDSerializer : StdSerializer<ULID>(ULID::class.java) {
        override fun serialize(value: ULID?, gen: JsonGenerator?, provider: SerializerProvider?) {
            if (value == null) {
                gen?.writeString("")
                return
            }
            gen?.writeString(value.toString())
        }
    }

    class ULIDDeserializer : StdDeserializer<ULID>(ULID::class.java) {
        override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): ULID {
            if (p == null) {
                return ULID(null)
            }
            return ULID.fromString(p.text)
        }
    }
}
