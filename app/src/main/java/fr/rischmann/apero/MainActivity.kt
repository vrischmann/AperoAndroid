package fr.rischmann.apero

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager
            .beginTransaction()
            .add(SettingsFragment(), null)
            .commit()

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    fun copy(view: View) {
        val text = text.text

    }

    fun paste(view: View) {

    }


}

class SettingsFragment() : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        val endpoint = EditTextPreference(context)
        endpoint.key = "endpoint"
        endpoint.title = "Endpoint"

        val psKey = EditTextPreference(context)
        psKey.key = "ps_key"
        psKey.title = "Pre-shared key"

        val encryptKey = EditTextPreference(context)
        encryptKey.key = "encrypt_key"
        encryptKey.title = "Encryption key"

        val signPrivateKey = EditTextPreference(context)
        signPrivateKey.key = "sign_private_key"
        signPrivateKey.title = "Signature private key"

        val signPublicKey = EditTextPreference(context)
        signPublicKey.key = "sign_public_key"
        signPublicKey.title = "Signature public key"

        screen.addPreference(endpoint)
        screen.addPreference(psKey)
        screen.addPreference(encryptKey)
        screen.addPreference(signPrivateKey)
        screen.addPreference(signPublicKey)

        preferenceScreen = screen
    }

}
