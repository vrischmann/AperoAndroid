package fr.rischmann.apero

import android.util.Log
import fr.rischmann.apero.Logging.TAG
import org.bouncycastle.math.ec.rfc8032.Ed25519

object Crypto {
    fun sign(privateKey: ByteArray, content: ByteArray): ByteArray {
        require(privateKey.size == Ed25519.SECRET_KEY_SIZE) { "private key size is invalid" }

        val signature = ByteArray(Ed25519.SIGNATURE_SIZE)
        Ed25519.sign(privateKey, 0, content, 0, content.size, signature, 0)

        return signature
    }

    fun verify(publicKey: ByteArray, content: ByteArray, signature: ByteArray): Boolean {
        require(publicKey.size == Ed25519.PUBLIC_KEY_SIZE) { "public key size is invalid" }

        return Ed25519.verify(signature, 0, publicKey, 0, content, 0, content.size)
    }

    fun openSecretBox(box: SecretBox, data: ByteArray): ByteArray? {
        if (data.size <= SecretBox.NONCE_SIZE) {
            Log.e(TAG, "invalid secret box size")
            return null
        }

        val nonce = data.sliceArray(0 until SecretBox.NONCE_SIZE)
        val ciphertext = data.sliceArray(SecretBox.NONCE_SIZE until data.size)

        return box.open(ciphertext, nonce)
    }
}