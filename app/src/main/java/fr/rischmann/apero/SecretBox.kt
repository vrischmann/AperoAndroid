package fr.rischmann.apero

import org.bouncycastle.crypto.engines.XSalsa20Engine
import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.max
import kotlin.math.min

// SecretBox is a compatible implementation of https://godoc.org/golang.org/x/crypto/nacl/secretbox
// which is in turn a compatible implementation of https://nacl.cr.yp.to/secretbox.html
//
// I based this code on https://github.com/codahale/xsalsa20poly1305/blob/master/src/main/java/com/codahale/xsalsa20poly1305/SecretBox.java
class SecretBox(key: ByteArray) {
    private val key: ByteArray

    init {
        require(key.size == KEY_SIZE) { "key must be 32 bytes" }
        this.key = key.copyOf(key.size)

    }

    fun open(ciphertext: ByteArray, nonce: ByteArray): ByteArray? {
        val xSalsa20 = XSalsa20Engine()
        val poly1305 = Poly1305()

        // initialize XSalsa20
        xSalsa20.init(true, ParametersWithIV(KeyParameter(key), nonce))

        // generate Poly1305 sub key
        val subKey = ByteArray(32)
        xSalsa20.processBytes(subKey, 0, subKey.size, subKey, 0)

        // hash ciphertext
        poly1305.init(KeyParameter(subKey))
        val len = max(ciphertext.size - poly1305.macSize, 0)
        poly1305.update(ciphertext, poly1305.macSize, len)
        val calculatedMac = ByteArray(poly1305.macSize)
        poly1305.doFinal(calculatedMac, 0)

        val presentedMac = ByteArray(poly1305.macSize)
        System.arraycopy(ciphertext, 0, presentedMac, 0, min(ciphertext.size, poly1305.macSize))

        if (!MessageDigest.isEqual(calculatedMac, presentedMac)) {
            return null
        }

        val plaintext = ByteArray(len)
        xSalsa20.processBytes(ciphertext, poly1305.macSize, plaintext.size, plaintext, 0)

        return plaintext
    }

    fun seal(message: ByteArray, nonce: ByteArray): ByteArray {
        val xSalsa20 = XSalsa20Engine()
        val poly1305 = Poly1305()

        // initialize XSalsa20
        xSalsa20.init(true, ParametersWithIV(KeyParameter(key), nonce))

        // generate Poly1305 sub key
        val subKey = ByteArray(32)
        xSalsa20.processBytes(subKey, 0, subKey.size, subKey, 0)

        // encrypt plaintext
        val out = ByteArray(NONCE_SIZE + poly1305.macSize + message.size)
        xSalsa20.processBytes(message, 0, message.size, out, NONCE_SIZE + poly1305.macSize)

        // hash ciphertext and prepend MAC to ciphertext
        poly1305.init(KeyParameter(subKey))
        poly1305.update(out, NONCE_SIZE + poly1305.macSize, message.size)
        poly1305.doFinal(out, NONCE_SIZE)

        System.arraycopy(nonce, 0, out, 0, NONCE_SIZE)

        return out
    }

    companion object {
        const val NONCE_SIZE = 24
        const val KEY_SIZE = 32

        fun newNonce(): ByteArray {
            val key = ByteArray(NONCE_SIZE)

            val secureRandom = SecureRandom()
            secureRandom.nextBytes(key)

            return key
        }

        fun newKey(): ByteArray {
            val key = ByteArray(KEY_SIZE)

            val secureRandom = SecureRandom()
            secureRandom.nextBytes(key)

            return key
        }
    }
}
