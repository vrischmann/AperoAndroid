package fr.rischmann.apero

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.rischmann.apero.Logging.TAG
import java.util.concurrent.CompletableFuture

// TODO(vincent): error handling and shit


class Credentials(
    val psKey: ByteArray,
    val encryptKey: ByteArray,
    val signPublicKey: ByteArray,
    val signPrivateKey: ByteArray
) {
    fun isValid(): Boolean {
        return psKey.isNotEmpty() && encryptKey.isNotEmpty() && signPublicKey.isNotEmpty() && signPrivateKey.isNotEmpty()
    }
}

interface EntryRepository {
    fun getEntries(): LiveData<Entries>
    fun moveEntry(entry: Entry): CompletableFuture<ByteArray>
    fun pasteEntry(entry: Entry): CompletableFuture<ByteArray>

    companion object {
        fun real(client: AperoClient): EntryRepository {
            return RealEntryRepository(client)
        }

        fun dummy(): EntryRepository {
            return DummyEntryRepository()
        }
    }
}

private class DummyEntryRepository : EntryRepository {
    override fun getEntries(): LiveData<Entries> {
        Log.d(TAG, "dummy repository: get entries")
        return MutableLiveData(emptyList())
    }

    override fun moveEntry(entry: Entry): CompletableFuture<ByteArray> {
        Log.d(TAG, "dummy repository: move entry")
        return CompletableFuture.completedFuture(byteArrayOf())
    }

    override fun pasteEntry(entry: Entry): CompletableFuture<ByteArray> {
        Log.d(TAG, "dummy repository: paste entry")
        return CompletableFuture.completedFuture(byteArrayOf())
    }

}


private class RealEntryRepository(private val client: AperoClient) : EntryRepository {
    override fun getEntries(): LiveData<Entries> {
        val data = MutableLiveData<Entries>()

        val future = client.getEntries()
        future.whenComplete { value, exception ->
            exception ?: return@whenComplete
            data.postValue(value)
        }

        return data
    }

    override fun moveEntry(entry: Entry): CompletableFuture<ByteArray> = client.moveEntry(entry)

    override fun pasteEntry(entry: Entry): CompletableFuture<ByteArray> = client.pasteEntry(entry)
}