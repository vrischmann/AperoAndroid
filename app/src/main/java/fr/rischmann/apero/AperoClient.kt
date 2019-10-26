package fr.rischmann.apero

import android.util.Log
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.CompletableFuture

interface AperoClient {
    fun getEntries(): CompletableFuture<Entries>
    fun moveEntry(entry: Entry): CompletableFuture<ByteArray>
    fun pasteEntry(entry: Entry): CompletableFuture<ByteArray>

    companion object {
        fun real(endpoint: String, credentials: Credentials): AperoClient {
            return RealAperoClient(endpoint, credentials)
        }

        fun dummy(): AperoClient {
            return DummyClient()
        }
    }
}

private class DummyClient() : AperoClient {
    override fun getEntries(): CompletableFuture<Entries> {
        return CompletableFuture.completedFuture(emptyList())
    }

    override fun moveEntry(entry: Entry): CompletableFuture<ByteArray> {
        return CompletableFuture.completedFuture(byteArrayOf())
    }

    override fun pasteEntry(entry: Entry): CompletableFuture<ByteArray> {
        return CompletableFuture.completedFuture(byteArrayOf())
    }
}

private class RealAperoClient(private val endpoint: String, private val credentials: Credentials) : AperoClient {
    private val httpClient: OkHttpClient = OkHttpClient()

    override fun getEntries(): CompletableFuture<Entries> {
        Log.d(Logging.TAG, "loading entries from $endpoint")

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

        val data = CompletableFuture<Entries>()

        httpClient.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // TODO(vincent): error handling

                if (response.code != 200) {
                    Log.e(Logging.TAG, "invalid response code ${response.code}")
                    return
                }

                if (response.body == null) {
                    Log.e(Logging.TAG, "no response body in list request")
                    return
                }
                val respData = response.body!!.bytes()

                val plaintext = Crypto.openSecretBox(secretBox, respData)
                if (plaintext == null) {
                    Log.e(Logging.TAG, "unable to open box")
                    return
                }

                val resp = JSONHelpers.objectMapper.readValue(plaintext, APITypes.ListResponse::class.java)

                data.complete(resp.entries.map(::Entry))
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(Logging.TAG, "call to list entries failed", e)
            }
        })

        return data
    }

    override fun moveEntry(entry: Entry): CompletableFuture<ByteArray> {
        Log.d(Logging.TAG, "move entry ${entry.id}")
        return moveOrPasteEntry(entry, Operation.MOVE)
    }

    override fun pasteEntry(entry: Entry): CompletableFuture<ByteArray> {
        Log.d(Logging.TAG, "paste entry ${entry.id}")
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

        httpClient.newCall(req).enqueue(object : Callback {
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
                Log.e(Logging.TAG, "call to move entry failed", e)
                future.completeExceptionally(e)
            }
        })

        return future
    }

    companion object {
        private val listPayload = byteArrayOf('L'.toByte())
    }
}