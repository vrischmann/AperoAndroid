package fr.rischmann.apero

import android.util.Log
import fr.rischmann.ulid.ULID
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.CompletableFuture

sealed class AperoStatus {
    object OK : AperoStatus()
    object NotFound : AperoStatus()
    data class Error(val msg: String, val throwable: Throwable?) : AperoStatus() {
        constructor(msg: String) : this(msg, null)
    }
}

data class AperoResponse<T>(val item: T, val status: AperoStatus)

typealias AperoCompletableFuture<T> = CompletableFuture<AperoResponse<T>>

interface AperoClient {
    fun getEntries(): AperoCompletableFuture<Entries>
    fun moveEntry(entry: Entry): AperoCompletableFuture<ByteArray>
    fun pasteEntry(entry: Entry): AperoCompletableFuture<ByteArray>
    fun copyEntry(content: ByteArray): AperoCompletableFuture<ULID>

    companion object {
        fun real(endpoint: String, credentials: Credentials): AperoClient {
            return RealAperoClient(endpoint, credentials)
        }

        fun dummy(): AperoClient {
            return DummyClient()
        }
    }
}

private class DummyClient : AperoClient {
    override fun getEntries(): AperoCompletableFuture<Entries> {
        return CompletableFuture.completedFuture(
            AperoResponse(
                item = emptyList(),
                status = AperoStatus.OK
            )
        )
    }

    override fun moveEntry(entry: Entry): AperoCompletableFuture<ByteArray> {
        return CompletableFuture.completedFuture(
            AperoResponse(
                item = byteArrayOf(),
                status = AperoStatus.OK
            )
        )
    }

    override fun pasteEntry(entry: Entry): AperoCompletableFuture<ByteArray> {
        return CompletableFuture.completedFuture(
            AperoResponse(
                item = byteArrayOf(),
                status = AperoStatus.OK
            )
        )
    }

    override fun copyEntry(content: ByteArray): AperoCompletableFuture<ULID> {
        val ts = Instant.now().toEpochMilli()
        return CompletableFuture.completedFuture(
            AperoResponse(
                item = ULID.random(ts, SecureRandom()),
                status = AperoStatus.OK
            )
        )
    }
}

private class RealAperoClient(private val endpoint: String, private val credentials: Credentials) : AperoClient {
    private val httpClient: OkHttpClient = OkHttpClient()

    override fun getEntries(): AperoCompletableFuture<Entries> {
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

        val data = AperoCompletableFuture<Entries>()

        httpClient.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    data.complete(AperoResponse(emptyList(), AperoStatus.Error("invalid response code ${response.code}")))
                    return
                }

                if (response.body == null) {
                    data.complete(AperoResponse(emptyList(), AperoStatus.Error("no body in list response")))
                    return
                }

                val respData = response.body!!.bytes()

                val plaintext = Crypto.openSecretBox(secretBox, respData)
                if (plaintext == null) {
                    data.complete(AperoResponse(emptyList(), AperoStatus.Error("unable to open PSKey secret box")))
                    return
                }

                val resp = JSONHelpers.objectMapper.readValue(plaintext, APITypes.ListResponse::class.java)
                val entries = resp.entries.map(::Entry)

                data.complete(AperoResponse(entries, AperoStatus.OK))
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.e(Logging.TAG, "call to list entries failed", e)
            }
        })

        return data
    }

    override fun copyEntry(content: ByteArray): AperoCompletableFuture<ULID> {
        // Prepare a secret box
        val secretBox = SecretBox(credentials.psKey)

        // Sign the id
        val signature = Crypto.sign(credentials.signPrivateKey, content)

        val copyRequest = APITypes.CopyRequest(
            signature = signature,
            content = content
        )

        // Encode the list request
        val payload = JSONHelpers.objectMapper.writeValueAsBytes(copyRequest)

        // Encrypt the payload with the pre-shared key
        val ciphertext = secretBox.seal(payload, SecretBox.newNonce())

        val req = Request.Builder()
            .url("$endpoint/api/v1/copy")
            .post(ciphertext.toRequestBody())
            .build()

        val future = AperoCompletableFuture<ULID>()

        httpClient.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // Handle errors

                if (response.code != 202) {
                    future.complete(AperoResponse(emptyULID(), AperoStatus.Error("invalid response code ${response.code}")))
                    return
                }
                if (response.body == null) {
                    future.complete(AperoResponse(emptyULID(), AperoStatus.Error("no body in move response")))
                    return
                }

                // Handle normal case

                val respData = response.body!!.bytes()

                val plaintext = Crypto.openSecretBox(secretBox, respData)
                if (plaintext == null) {
                    future.complete(AperoResponse(emptyULID(), AperoStatus.Error("unable to open PSKey secret box")))
                    return
                }

                if (plaintext.size != 16) {
                    future.complete(AperoResponse(emptyULID(), AperoStatus.Error("invalid ULID size ${plaintext.size}")))
                    return
                }


                future.complete(AperoResponse(ULID(plaintext), AperoStatus.OK))
            }

            override fun onFailure(call: Call, e: IOException) {
                future.complete(AperoResponse(emptyULID(), AperoStatus.Error("call to move entry failed", e)))
            }
        })

        return future
    }

    override fun moveEntry(entry: Entry): AperoCompletableFuture<ByteArray> {
        Log.d(Logging.TAG, "move entry ${entry.id}")
        return moveOrPasteEntry(entry, Operation.MOVE)
    }

    override fun pasteEntry(entry: Entry): AperoCompletableFuture<ByteArray> {
        Log.d(Logging.TAG, "paste entry ${entry.id}")
        return moveOrPasteEntry(entry, Operation.PASTE)
    }

    private enum class Operation {
        MOVE, PASTE
    }

    private fun moveOrPasteEntry(entry: Entry, operation: Operation): AperoCompletableFuture<ByteArray> {
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

        val future = AperoCompletableFuture<ByteArray>()

        httpClient.newCall(req).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                // Handle errors

                if (response.code == 404) {
                    future.complete(AperoResponse(byteArrayOf(), AperoStatus.NotFound))
                    return
                }
                if (response.code != 200) {
                    future.complete(AperoResponse(byteArrayOf(), AperoStatus.Error("invalid response code ${response.code}")))
                    return
                }
                if (response.body == null) {
                    future.complete(AperoResponse(byteArrayOf(), AperoStatus.Error("no body in move response")))
                    return
                }

                // Handle normal case

                val respData = response.body!!.bytes()

                val plaintext = Crypto.openSecretBox(secretBox, respData)
                if (plaintext == null) {
                    future.complete(AperoResponse(byteArrayOf(), AperoStatus.Error("unable to open PSKey secret box")))
                    return
                }

                future.complete(AperoResponse(plaintext, AperoStatus.OK))
            }

            override fun onFailure(call: Call, e: IOException) {
                future.complete(AperoResponse(byteArrayOf(), AperoStatus.Error("call to move entry failed", e)))
            }
        })

        return future
    }

    companion object {
        private val listPayload = byteArrayOf('L'.toByte())

        private fun emptyULID(): ULID {
            return ULID(byteArrayOf())
        }
    }
}