package fr.rischmann.apero

import junit.framework.Assert.assertEquals
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretBoxTest {
    @Test
    fun sealOpenGoGenerated() {
        val key = Hex.decode("e39616fa92ab3779f1503a31ed71a9990eb3524be51257b7d645ec588dc0b7e1")

        val input =
            Hex.decode("43ef80e62ee631cf73cf526ccff1dc92c4cbdc1c4b14c19a46cc2b612ed698e8fbc9f34caa3fecb50673200ede82")

        val nonce = input.sliceArray(0 until SecretBox.NONCE_SIZE)
        val ciphertext = input.sliceArray(SecretBox.NONCE_SIZE until input.size)

        val box = SecretBox(key)
        val plaintext = box.open(ciphertext, nonce)

        val message = plaintext?.toString(Charsets.UTF_8)
        assertEquals("plaintext is invalid", "yellow", message)
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