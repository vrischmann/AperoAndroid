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
    }
}

private class RealEntryRepository(private val client: AperoClient) : EntryRepository {
    override fun getEntries(): LiveData<Entries> {
        val data = MutableLiveData<Entries>()

        val future = client.getEntries()
        future.whenComplete { value, exception ->
            if (exception != null) {
                Log.e(TAG, "unable to get entries", exception)
                return@whenComplete
            }
            data.postValue(value)
        }

        return data
    }

    override fun moveEntry(entry: Entry): CompletableFuture<ByteArray> = client.moveEntry(entry)

    override fun pasteEntry(entry: Entry): CompletableFuture<ByteArray> = client.pasteEntry(entry)
}