package fr.rischmann.apero

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
import androidx.preference.PreferenceFragmentCompat
import fr.rischmann.apero.Logging.TAG

class MainActivity : AppCompatActivity(), EntryListFragment.OnListItemMove,
    EntryListFragment.OnListItemPaste {

    private lateinit var _vm: EntryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _vm = ViewModelProviders.of(this)[EntryViewModel::class.java]
        _vm.getEntries().observe(this, Observer {
            Log.i(TAG, "loaded entries: $it")
        })

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }

    override fun onListItemMove(item: Entry?) {
        if (item == null) {
            return
        }

        Log.i(TAG, "move item $item")

        // TODO(vincent): do move on the server via API call

        _vm.removeEntry(item)
    }

    override fun onListItemPaste(item: Entry?) {
        if (item == null) {
            return
        }

        Log.i(TAG, "paste item $item")

        // TODO(vincent): do paste on the server via API call
    }

}

object Logging {
    const val TAG = "Apero"
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
