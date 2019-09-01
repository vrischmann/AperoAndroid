package fr.rischmann.apero

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.engines.ChaChaEngine
import org.bouncycastle.crypto.engines.XSalsa20Engine
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.jcajce.provider.symmetric.ChaCha
import org.bouncycastle.math.ec.rfc8032.Ed25519

class MainActivity : AppCompatActivity() {
    init {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun copy(view: View) {
        val text = text.text
        val s = text.toString()

        val engine = ChaChaEngine()

        engine.processBytes()
    }

    fun paste(view: View) {

    }
}
