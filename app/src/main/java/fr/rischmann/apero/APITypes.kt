package fr.rischmann.apero

import fr.rischmann.ulid.ULID

object APITypes {
    class CopyRequest(val signature: ByteArray, val content: ByteArray)
    class PasteRequest(val signature: ByteArray, val id: ULID)
    class MoveRequest(val signature: ByteArray, val id: ULID)
    class ListRequest(val signature: ByteArray)
    class ListResponse(val entries: List<ULID>)
}