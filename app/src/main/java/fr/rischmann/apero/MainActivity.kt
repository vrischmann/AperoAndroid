package fr.rischmann.apero

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import fr.rischmann.apero.Logging.TAG
import fr.rischmann.ulid.ULID
import okhttp3.OkHttpClient
import java.nio.charset.Charset
import java.util.*

class MainActivity : AppCompatActivity(),
    EntryListFragment.OnListItemMove,
    EntryListFragment.OnListItemPaste,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val _httpClient = OkHttpClient()
    private lateinit var _vm: EntryViewModel

    private lateinit var _credentials: Credentials
    private var _client = EntryRepository.dummy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(this)

        //

        recreateClient(prefs)
        recreateVM()

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

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        recreateClient(prefs)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }

    override fun onListItemMove(item: Entry?) {
        if (item == null) {
            return
        }

        Log.i(TAG, "move item $item")

        _vm.moveEntry(item)?.let {
            Log.d(TAG, "moved item ${item.id}")

            copyItemToClipboard(item.id, it)
        }
    }

    override fun onListItemPaste(item: Entry?) {
        if (item == null) {
            return
        }

        Log.i(TAG, "paste item $item")

        _vm.pasteEntry(item)?.let {
            Log.d(TAG, "pasted item ${item.id}")

            copyItemToClipboard(item.id, it)
        }
    }

    private fun recreateClient(preferences: SharedPreferences) {
        val endpoint = preferences.string("endpoint")
        if (endpoint.isEmpty()) {
            return
        }

        // Try to create the client based on old preferences
        _client = clientFromPrefs(preferences, endpoint)
    }

    private fun recreateVM() {
        val vmFactory = EntryViewModelFactory(_client)
        _vm = ViewModelProviders.of(this, vmFactory)[EntryViewModel::class.java]
        _vm.entries.observe(this, Observer {
            Log.d(TAG, "loaded entries: $it")
        })
    }

    private fun copyItemToClipboard(id: ULID, data: ByteArray) {
        // The data in each entry is itself encrypted with our E2E key
        val box = SecretBox(_credentials.encryptKey)

        val plaintext = Crypto.openSecretBox(box, data) ?: return
        val s = String(plaintext, Charset.defaultCharset())

        Log.d(TAG, s)

        val clipData = ClipData.newPlainText(id.toString(), s)

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clipData)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        val endpoint = preferences.string("endpoint")
        if (endpoint.isEmpty()) {
            _client = EntryRepository.dummy()
            return
        }

        // The user wants to fetch the credentials from the server
        if (key == "provision") {
            // TODO
        }

        Log.d(TAG, "preferences changed")
        _client = clientFromPrefs(preferences, endpoint)
    }

    private fun clientFromPrefs(p: SharedPreferences, endpoint: String): EntryRepository {
        val psKey = p.string("psKey").let(::fromB64) ?: byteArrayOf()
        val encryptKey = p.string("encryptKey").let(::fromB64) ?: byteArrayOf()
        val signPublicKey = p.string("signPublicKey").let(::fromB64) ?: byteArrayOf()
        // only use the first 32 bytes because Bouncycastle's and Go's implementation of ed25519 are not strictly compatible.
        // Bouncycastle's private keys are what the Go package calls seeds: https://godoc.org/golang.org/x/crypto/ed25519
        val signPrivateKey = p.string("signPrivateKey").let(::fromB64)?.sliceArray(0..31) ?: byteArrayOf()

        _credentials = Credentials(psKey, encryptKey, signPublicKey, signPrivateKey)
        if (!_credentials.isValid()) {
            return EntryRepository.dummy()
        }

        Log.i(TAG, "creating client to $endpoint")

        return EntryRepository.real(_httpClient, endpoint, _credentials)
    }
}

private fun SharedPreferences.string(key: String): String {
    return getString(key, "").orEmpty()
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
