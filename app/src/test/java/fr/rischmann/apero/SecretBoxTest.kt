package fr.rischmann.apero

import junit.framework.Assert.assertEquals
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretBoxTest {
    @Test
    fun sealOpenGoGenerated() {
        val key = Hex.decode("61caae80b04a17c12a824c6e65ff7e65da611b23e2aa0a4432995e8be79dc2f2")
        val input = Hex.decode("b829ada6d2669feb76bd4113b3f84a45f29c171e458b71599a78342d7699d5a7997782baa6cfb8d3527d82018e9f68")

        val nonce = input.sliceArray(0 until SecretBox.NONCE_SIZE)
        assertTrue(nonce.size == SecretBox.NONCE_SIZE)
        val ciphertext = input.sliceArray(SecretBox.NONCE_SIZE until input.size)
        assertTrue(ciphertext.isNotEmpty())

        val box = SecretBox(key)
        val plaintext = box.open(ciphertext, nonce)

        val message = plaintext?.toString(Charsets.UTF_8)
        assertEquals("plaintext is invalid", "yellow\n", message)
    }

    @Test
    fun sealOpen() {
        val key = SecretBox.newKey()
        val box = SecretBox(key)
        val nonce = SecretBox.newNonce()

        val message = "foobar".toByteArray()

        val ciphertext = box.seal(message, nonce)
        assertTrue(ciphertext.size > SecretBox.NONCE_SIZE)

        val newNonce = ciphertext.sliceArray(0 until SecretBox.NONCE_SIZE)
        val newCiphertext = ciphertext.sliceArray(SecretBox.NONCE_SIZE until ciphertext.size)

        val plaintext = box.open(newCiphertext, newNonce)

        assertArrayEquals("decrypted plaintext is invalid", message, plaintext)
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }
}