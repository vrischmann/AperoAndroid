package fr.rischmann.apero

import java.util.*

object TestHelpers {
    fun b64(s: String): ByteArray {
        return Base64.getDecoder().decode(s)
    }
    fun b64(data: ByteArray): String {
        return Base64.getEncoder().encodeToString(data)
    }
}