package fr.rischmann.apero

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import fr.rischmann.apero.Logging.TAG
import fr.rischmann.ulid.ULID
import java.nio.charset.Charset
import java.util.*

class MainActivity : AppCompatActivity(),
    EntryListFragment.OnListItemMove,
    EntryListFragment.OnListItemPaste,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var _vm: EntryViewModel

    private var _encryptKey = byteArrayOf()

    private var _client = AperoClient.dummy()
    private var _repository = EntryRepository.real(_client)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //

        createRepositoryFromPrefs()
        createViewModel()

        //

        setContentView(R.layout.activity_main)

        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onResume() {
        super.onResume()

        createRepositoryFromPrefs()
        createViewModel()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }

    override fun onListItemMove(item: Entry?) {
        if (item == null) {
            return
        }
        if (_encryptKey.isEmpty()) {
            Log.i(TAG, "encryption key is not defined")
            Toast.makeText(applicationContext, getString(R.string.move_encryption_key_undefined), Toast.LENGTH_LONG).show()
            return
        }

        Log.i(TAG, "move item $item")

        _vm.moveEntry(item)?.let {
            when (it.status) {
                is AperoStatus.OK -> {
                    Log.d(TAG, "moved item ${item.id}")
                    copyItemToClipboard(item.id, it.item)
                }
                is AperoStatus.NotFound -> {
                    Log.d(TAG, "item ${item.id} doesn't exist")
                    Toast.makeText(applicationContext, getString(R.string.client_item_does_not_exist), Toast.LENGTH_LONG).show()
                }
                is AperoStatus.Error -> {
                    Log.e(TAG, it.status.msg, it.status.throwable)
                    Toast.makeText(applicationContext, getString(R.string.client_unable_move_item), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onListItemPaste(item: Entry?) {
        if (item == null) {
            return
        }
        if (_encryptKey.isEmpty()) {
            Log.i(TAG, "encryption key is not defined")
            Toast.makeText(applicationContext, getString(R.string.paste_encryption_undefined), Toast.LENGTH_LONG).show()
            return
        }

        Log.i(TAG, "paste item $item")

        _vm.pasteEntry(item)?.let {
            when (it.status) {
                is AperoStatus.OK -> {
                    Log.d(TAG, "pasted item ${item.id}")
                    copyItemToClipboard(item.id, it.item)
                }
                is AperoStatus.NotFound -> {
                    Log.d(TAG, "item ${item.id} doesn't exist")
                    Toast.makeText(applicationContext, getString(R.string.client_item_does_not_exist), Toast.LENGTH_LONG).show()
                }
                is AperoStatus.Error -> {
                    Log.e(TAG, it.status.msg, it.status.throwable)
                    Toast.makeText(applicationContext, getString(R.string.client_unable_paste_item), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun createRepositoryFromPrefs() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)

        // Try to create the client based on old preferences
        createClientFromPrefs(prefs)
        createRepository()
    }

    private fun createViewModel() {
        Log.d(TAG, "recreating view model")

        viewModelStore.clear()

        val vmFactory = EntryViewModelFactory(_repository)
        _vm = ViewModelProviders.of(this, vmFactory)[EntryViewModel::class.java]
        _vm.entries.observe(this, Observer {
            Log.d(TAG, "loaded entries: $it")
        })
    }

    private fun copyItemToClipboard(id: ULID, data: ByteArray) {
        val plaintext = Crypto.openSecretBox(SecretBox(_encryptKey), data)
        if (plaintext == null) {
            Log.w(TAG, "unable to open E2E secret box")
            return
        }
        val s = String(plaintext, Charset.defaultCharset())

        Log.d(TAG, s)

        val clipData = ClipData.newPlainText(id.toString(), s)

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clipData)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences?.let {
            Log.d(TAG, "preferences changed")
            createClientFromPrefs(it)
            createRepository()
            createViewModel()
        }
    }

    private fun createClientFromPrefs(p: SharedPreferences) {
        val endpoint = p.getString("endpoint", "").orEmpty()
        if (endpoint.isEmpty()) {
            _client = AperoClient.dummy()
            return
        }

        Log.i(TAG, "creating client to $endpoint")

        val credentials = if (BuildConfig.DEBUG) {
            Log.d(TAG, "loading from debug configuration")

            val psKey = BuildConfig.PS_KEY.let(::fromB64) ?: byteArrayOf()
            val encryptKey = BuildConfig.ENCRYPT_KEY.let(::fromB64) ?: byteArrayOf()
            val signPublicKey = BuildConfig.SIGN_PUBLIC_KEY.let(::fromB64) ?: byteArrayOf()
            // only use the first 32 bytes because Bouncycastle's and Go's implementation of ed25519 are not strictly compatible.
            // Bouncycastle's private keys are what the Go package calls seeds: https://godoc.org/golang.org/x/crypto/ed25519
            val signPrivateKey = BuildConfig.SIGN_PRIVATE_KEY.let(::fromB64)?.sliceArray(0..31) ?: byteArrayOf()

            Credentials(psKey, encryptKey, signPublicKey, signPrivateKey)
        } else {
            Log.d(TAG, "loading from preferences")

            fun g(key: String): String? {
                return p.getString(key, "")
            }

            val psKey = g("psKey")?.let(::fromB64) ?: byteArrayOf()
            val encryptKey = g("encryptKey")?.let(::fromB64) ?: byteArrayOf()
            val signPublicKey = g("signPublicKey")?.let(::fromB64) ?: byteArrayOf()
            // only use the first 32 bytes because Bouncycastle's and Go's implementation of ed25519 are not strictly compatible.
            // Bouncycastle's private keys are what the Go package calls seeds: https://godoc.org/golang.org/x/crypto/ed25519
            val signPrivateKey = g("signPrivateKey")?.let(::fromB64)?.sliceArray(0..31) ?: byteArrayOf()

            Credentials(psKey, encryptKey, signPublicKey, signPrivateKey)
        }

        if (!credentials.isValid()) {
            _client = AperoClient.dummy()
            return
        }

        _encryptKey = credentials.encryptKey.copyOf()
        _client = AperoClient.real(endpoint, credentials)
    }

    private fun createRepository() {
        _repository = EntryRepository.real(_client)
    }
}

private fun fromB64(s: String): ByteArray? {
    if (s.isEmpty()) {
        return null
    }
    return Base64.getDecoder().decode(s)
}

object Logging {
    const val TAG = "Apero"
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
