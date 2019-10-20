package fr.rischmann.apero

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.rischmann.apero.Logging.TAG

class EntryListFragment : Fragment() {
    private var moveListener: OnListItemMove? = null
    private var pasteListener: OnListItemPaste? = null

    private lateinit var _vm: EntryViewModel
    private lateinit var _adapter: EntryListRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _vm = activity?.run {
            ViewModelProviders.of(this)[EntryViewModel::class.java]
        } ?: throw Exception("invalid activity")
        _vm.getEntries().observeForever {
            Log.d(TAG, "list: $it")
            _adapter.setData(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = _adapter
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
