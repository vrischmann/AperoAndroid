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
    fun list(): LiveData<AperoResponse<Entries>>
    fun move(entry: Entry): CompletableFuture<AperoResponse<ByteArray>>
    fun paste(entry: Entry): CompletableFuture<AperoResponse<ByteArray>>

    companion object {
        fun real(client: AperoClient): EntryRepository {
            return RealEntryRepository(client)
        }
    }
}

private class RealEntryRepository(private val client: AperoClient) : EntryRepository {
    override fun list(): LiveData<AperoResponse<Entries>> {
        val data = MutableLiveData<AperoResponse<Entries>>()

        val future = client.list()
        future.whenComplete { value, exception ->
            if (exception != null) {
                Log.e(TAG, "unable to get entries", exception)
                return@whenComplete
            }
            data.postValue(value)
        }

        return data
    }

    override fun move(entry: Entry): CompletableFuture<AperoResponse<ByteArray>> = client.move(entry)

    override fun paste(entry: Entry): CompletableFuture<AperoResponse<ByteArray>> = client.paste(entry)
}