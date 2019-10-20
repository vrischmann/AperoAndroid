package fr.rischmann.apero

import java.nio.ByteBuffer
import java.time.Instant
import java.util.*

data class ID(val bytes: ByteArray) {
    fun timestamp(): Long {
        val a = bytes[5].toLong()
        val b = bytes[4].toLong()
        val c = bytes[3].toLong()
        val d = bytes[2].toLong()
        val e = bytes[1].toLong()
        val f = bytes[0].toLong()

        return a or (b shl 8) or (c shl 16) or (d shl 24) or (e shl 32) or (f shl 40)
    }

    companion object {
        fun random(): ID {
            val uuid = UUID.randomUUID()

            val bb = ByteBuffer.allocate(16)
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)

            return ID(bb.array())
        }

        fun fromString(s: String): ID {
            val uuid = UUID.fromString(s)

            val bb = ByteBuffer.allocate(16)
            bb.putLong(uuid.mostSignificantBits)
            bb.putLong(uuid.leastSignificantBits)

            return ID(bb.array())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ID

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}

data class ListItem(val id: ID)

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

    data class PasteRequest(val signature: Signature, val id: String)
    data class MoveRequest(val signature: Signature, val id: String)

    data class ListRequest(val signature: Signature)
}