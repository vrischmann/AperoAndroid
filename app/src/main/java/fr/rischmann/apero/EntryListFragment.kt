package fr.rischmann.apero

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import fr.rischmann.apero.Logging.TAG

class EntryListFragment : Fragment() {
    private var moveListener: OnListItemMove? = null
    private var pasteListener: OnListItemPaste? = null

    private lateinit var _vm: EntryViewModel
    private lateinit var _adapter: EntryListRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve view model
        _vm = activity?.run {
            ViewModelProviders.of(this)[EntryViewModel::class.java]
        } ?: throw Exception("invalid activity")

        _vm.entries.observeForever {
            when (it.status) {
                is AperoStatus.OK -> _adapter.setData(it.item)
                is AperoStatus.Error -> {
                    Toast.makeText(context, "Unable to get entries from the server", Toast.LENGTH_LONG).show()
                    Log.e(TAG, it.status.msg, it.status.throwable)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_entry_list, container, false)

        if (view is SwipeRefreshLayout) {
            with(view) {
                val rv = this.findViewById<RecyclerView>(R.id.entryList)
                with(rv) {
                    layoutManager = LinearLayoutManager(context)
                    adapter = _adapter
                }

                // Force reload the entries
                view.isRefreshing = true
                _vm.reloadEntries()
                view.isRefreshing = false


                // Trigger a reload when pulled to refresh
                this.setOnRefreshListener {
                    _vm.reloadEntries()
                    view.isRefreshing = false
                }
            }
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListItemMove) {
            moveListener = context
        } else {
            throw RuntimeException("$context must implement OnListItemMove")
        }
        if (context is OnListItemPaste) {
            pasteListener = context
        } else {
            throw RuntimeException("$context must implement OnListItemPaste")
        }

        _adapter = EntryListRecyclerViewAdapter(moveListener, pasteListener)
    }

    override fun onDetach() {
        super.onDetach()
        moveListener = null
        pasteListener = null
    }

    interface OnListItemMove {
        fun onListItemMove(item: Entry?)
    }

    interface OnListItemPaste {
        fun onListItemPaste(item: Entry?)
    }
}
