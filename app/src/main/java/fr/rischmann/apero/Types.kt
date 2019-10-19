package fr.rischmann.apero

object Types {
    data class CopyRequest(val signature: ByteArray, val content: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CopyRequest

            if (!signature.contentEquals(other.signature)) return false
            if (!content.contentEquals(other.content)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = signature.contentHashCode()
            result = 31 * result + content.contentHashCode()
            return result
        }
    }
}