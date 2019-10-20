package fr.rischmann.apero

import java.time.Instant

data class ListItem(val time: Instant, val content: String)

data class Signature(private val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Signature

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}

object Types {
    data class CopyRequest(val signature: Signature, val content: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CopyRequest

            if (signature != other.signature) return false
            if (!content.contentEquals(other.content)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = signature.hashCode()
            result = 31 * result + content.contentHashCode()
            return result
        }
    }

    data class PasteRequest(val signature: Signature, val id: ULID)
    data class MoveRequest(val signature: Signature, val id: ULID)

    data class ListRequest(val signature: Signature)
}