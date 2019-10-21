package fr.rischmann.apero

import fr.rischmann.ulid.ULID

data class Entry(val id: ULID)

typealias Entries = List<Entry>