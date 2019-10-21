package fr.rischmann.apero

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.rischmann.apero.Logging.TAG
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
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
        fun real(endpoint: String, credentials: Credentials): EntryRepository {
            return RealEntryRepository(endpoint, credentials)
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


private class RealEntryRepository(private val endpoint: String, val credentials: Credentials) : EntryRepository {
    val client = OkHttpClient()

    override fun getEntries(): LiveData<Entries> {
        Log.d(TAG, "loading entries from $endpoint")

        // Prepare a secret box
        val secretBox = SecretBox(credentials.psKey)

        // Sign the static payload
        val signature = Crypto.sign(credentials.signPrivateKey, listPayload)

        // Encode the list request
        val listRequest = APITypes.ListRequest(signature)
        val payload = JSONHelpers.objectMapper.writeValueAsBytes(listRequest)

        // Encrypt the payload with the pre-shared key
        val ciphertext = secretBox.seal(payload, SecretBox.newNonce())

        val req = Request.Builder()
            .url("$endpoint/api/v1/list")
            .post(ciphertext.toRequestBody())
            .build()

        val data = MutableLiveData<Entries>()

        client.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // TODO(vincent): error handling

                if (response.code != 200) {
                    Log.e(TAG, "invalid response code ${response.code}")
                    return
                }

                if (response.body == null) {
                    Log.e(TAG, "no response body in list request")
                    return
                }
                val respData = response.body!!.bytes()

                val plaintext = Crypto.openSecretBox(secretBox, respData)
                if (plaintext == null) {
                    Log.e(TAG, "unable to open box")
                    return
                }

                val resp = JSONHelpers.objectMapper.readValue(plaintext, APITypes.ListResponse::class.java)

                data.postValue(resp.entries.map(::Entry))
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "call to list entries failed", e)
            }
        })

        return data
    }

    override fun moveEntry(entry: Entry): CompletableFuture<ByteArray> {
        Log.d(TAG, "move entry ${entry.id}")
        return moveOrPasteEntry(entry, Operation.MOVE)
    }

    override fun pasteEntry(entry: Entry): CompletableFuture<ByteArray> {
        Log.d(TAG, "paste entry ${entry.id}")
        return moveOrPasteEntry(entry, Operation.PASTE)
    }

    private enum class Operation {
        MOVE, PASTE
    }

    private fun moveOrPasteEntry(entry: Entry, operation: Operation): CompletableFuture<ByteArray> {
        // Prepare a secret box
        val secretBox = SecretBox(credentials.psKey)

        // Sign the id
        val signature = Crypto.sign(credentials.signPrivateKey, entry.id.data)

        val moveRequest = APITypes.MoveRequest(
            signature = signature,
            id = entry.id
        )

        // Encode the list request
        val payload = JSONHelpers.objectMapper.writeValueAsBytes(moveRequest)

        // Encrypt the payload with the pre-shared key
        val ciphertext = secretBox.seal(payload, SecretBox.newNonce())

        val builder = when (operation) {
            Operation.MOVE -> Request.Builder()
                .url("$endpoint/api/v1/move")
                .delete(ciphertext.toRequestBody())

            Operation.PASTE -> Request.Builder()
                .url("$endpoint/api/v1/paste")
                .post(ciphertext.toRequestBody())
        }

        val req = builder.build()

        val future = CompletableFuture<ByteArray>()

        client.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // TODO(vincent): error handling

                if (response.code != 200) {
                    future.completeExceptionally(IllegalStateException("invalid response code ${response.code}"))
                    return
                }

                if (response.body == null) {
                    future.completeExceptionally(IllegalStateException("no response body in move request"))
                    return
                }
                val respData = response.body!!.bytes()

                val plaintext = Crypto.openSecretBox(secretBox, respData)
                if (plaintext == null) {
                    future.completeExceptionally(IllegalStateException("unable to open secret box"))
                    return
                }

                future.complete(plaintext)
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "call to move entry failed", e)
                future.completeExceptionally(e)
            }
        })

        return future
    }


    companion object {
        private val listPayload = byteArrayOf('L'.toByte())
    }
}