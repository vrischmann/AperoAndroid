package fr.rischmann.apero

import org.bouncycastle.math.ec.rfc8032.Ed25519
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import java.util.*

class CryptoTest {
    @Test
    fun signAndVerify() {
        val privateKey = ByteArray(Ed25519.SECRET_KEY_SIZE)
        Ed25519.generatePrivateKey(SecureRandom(), privateKey)

        val publicKey = ByteArray(Ed25519.PUBLIC_KEY_SIZE)
        Ed25519.generatePublicKey(privateKey, 0, publicKey, 0)

        val message = "hello".toByteArray(charset("UTF-8"))

        val signature = Crypto.sign(privateKey, message)
        assertTrue(Crypto.verify(publicKey, message, signature))
    }

    @Test
    fun signAndVerifyFromGoData() {
        val privateKey =
            decodeB64("pkk5Z6hxvQzz8G8Zy+AnoGcHVvZ/ltHU19n2TDEa1ZLzP7buaHE34/RCZ9lV8zduQnbnU9MJz3qYc375k20mtA==")
                .sliceArray(0..31)
        val publicKey = decodeB64("8z+27mhxN+P0QmfZVfM3bkJ251PTCc96mHN++ZNtJrQ=")

        val message = "hello".toByteArray(charset("UTF-8"))

        // Computed with Go's ed25519 generated private key
        val computedSignature =
            decodeB64("D3AquVmc/lYcUSXHjXA2BhvXqDBGu+SdWj5uxvtsyg5CKLNaQtJY1axdyLEc25pkloq+e7vZykVkMnnaCXwKAg==")

        assertTrue(Crypto.verify(publicKey, message, computedSignature))

        // Sign with Go's ed25519 generated private key
        val signature = Crypto.sign(privateKey, message)
        assertArrayEquals(computedSignature, signature)
    }

    private fun decodeB64(s: String): ByteArray {
        return Base64.getDecoder().decode(s)
    }
}