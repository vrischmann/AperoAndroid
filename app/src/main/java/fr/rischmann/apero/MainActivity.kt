package fr.rischmann.apero

import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import fr.rischmann.apero.Logging.TAG
import fr.rischmann.bip39.BIP39
import fr.rischmann.bip39.WordList
import fr.rischmann.ulid.ULID
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

class MainActivity : AppCompatActivity(),
    EntryListFragment.OnListItemMove,
    EntryListFragment.OnListItemPaste,
    CopyFragment.OnCopyListener,
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

        if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                val bundle = bundleOf(Pair("content", it))
                navController.navigate(R.id.action_miListFragment_to_copyFragment, bundle)
            }
        }
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
        viewModelStore.clear()

        val vmFactory = EntryViewModelFactory(_repository)
        _vm = ViewModelProviders.of(this, vmFactory)[EntryViewModel::class.java]
    }

    private fun copyItemToClipboard(id: ULID, data: ByteArray) {
        val plaintext = Crypto.openSecretBox(SecretBox(_encryptKey), data)
        if (plaintext == null) {
            Log.w(TAG, "unable to open E2E secret box")
            return
        }
        val s = String(plaintext, Charset.defaultCharset()).trim()

        Log.d(TAG, s)

        val clipData = ClipData.newPlainText(id.toString(), s)

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(clipData)
    }

    override fun onCopy(text: String) {
        val content = text.toByteArray(StandardCharsets.UTF_8)

        // Encrypt with the e2e encryption key
        val ciphertext = SecretBox(_encryptKey).seal(content, SecretBox.newNonce())

        _client.copy(ciphertext).handle { response, exception ->
            if (exception != null) {
                Log.e(TAG, "unable to copy content", exception)
                Toast.makeText(applicationContext, getString(R.string.unable_copy_to_server), Toast.LENGTH_LONG).show()
                return@handle
            }

            when (response.status) {
                is AperoStatus.OK -> {
                    Log.d(TAG, "copied content, id is $response")
                    Toast.makeText(applicationContext, "Copy successful", Toast.LENGTH_LONG).show()
                }
                is AperoStatus.Error -> {
                    Log.e(TAG, response.status.msg, response.status.throwable)
                    Toast.makeText(applicationContext, getString(R.string.unable_copy_to_server), Toast.LENGTH_LONG).show()
                }
            }
        }
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

            val psKey = BuildConfig.PS_KEY.let(::parsePreference) ?: byteArrayOf()
            val encryptKey = BuildConfig.ENCRYPT_KEY.let(::parsePreference) ?: byteArrayOf()
            val signPublicKey = BuildConfig.SIGN_PUBLIC_KEY.let(::parsePreference) ?: byteArrayOf()
            val signPrivateKey = BuildConfig.SIGN_PRIVATE_KEY.let(::parsePreference) ?: byteArrayOf()

            Credentials(psKey, encryptKey, signPublicKey, signPrivateKey)
        } else {
            Log.d(TAG, "loading from preferences")

            val psKey = p.string("psKey")?.let(::parsePreference) ?: byteArrayOf()
            val encryptKey = p.string("encryptKey")?.let(::parsePreference) ?: byteArrayOf()
            val signPublicKey = p.string("signPublicKey")?.let(::parsePreference) ?: byteArrayOf()
            val signPrivateKey = p.string("signPrivateKey")?.let(::parsePreference) ?: byteArrayOf()

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

private fun SharedPreferences.string(key: String): String? {
    return this.getString(key, "")
}

private fun parsePreference(pref: String): ByteArray? {
    val s = pref.trim()

    if (s.isEmpty()) {
        return null
    }

    try {
        return Base64.getDecoder().decode(s)
    } catch (e: IllegalArgumentException) {
    }

    try {
        return BIP39.bytes(WordList.Language.ENGLISH, s)
    } catch (e: java.lang.IllegalArgumentException) {
        Log.e(TAG, "not a base64 string or a mnemonic", e)
    }

    return null
}

object Logging {
    const val TAG = "Apero"
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
