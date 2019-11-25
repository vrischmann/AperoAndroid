package fr.rischmann.apero

import fr.rischmann.apero.TestHelpers.b64
import org.bouncycastle.math.ec.rfc8032.Ed25519
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

class CryptoTest {
    @Test
    fun open() {
        val key = Hex.decode("61caae80b04a17c12a824c6e65ff7e65da611b23e2aa0a4432995e8be79dc2f2")
        val input = Hex.decode("63b7379c350cd231ba74aaa13c0ceb3626240f30254a5fb0c4bba811ae8314f1864f484516054d89ada72e335ff6e2")

        val box = SecretBox(key)

        val plaintext = Crypto.openSecretBox(box, input)
        val message = plaintext?.toString(Charsets.UTF_8)
        assertEquals("yellow\n", message)
    }

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
        val privateKey = b64("fR2YhWSJFev9eLUXpQRYyr9CKL7F6Nsq6+JuF84Vz0w=")
        val publicKey = b64("bW7CIJBdDTHh5bL5UK7L+AXirOLHQMHSQUi206EoMCI=")
        val message = "hello".toByteArray(charset("UTF-8"))

        // Computed with Go's ed25519 generated private key
        val computedSignature = b64("vlqcAjxbN84QsGYOszauR3y9x1urfHVRjJ/6j67ELgbpUM/HE6rwCQlx1DvZnk1HNgReReLdrHDgfkJMTgSdAA==")

        assertTrue(Crypto.verify(publicKey, message, computedSignature))

        // Sign with Go's ed25519 generated private key
        val signature = Crypto.sign(privateKey, message)
        assertArrayEquals(computedSignature, signature)
    }
}