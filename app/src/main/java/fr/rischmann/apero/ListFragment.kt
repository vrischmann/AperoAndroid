package fr.rischmann.apero

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.security.SecureRandom
import java.time.Instant

class ListFragment : Fragment() {
    private var moveListener: OnListItemMove? = null
    private var pasteListener: OnListItemPaste? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                adapter = ListItemRecyclerViewAdapter(
                    listOf(
                        ListItem(ULID.random(Instant.now().toEpochMilli(), SecureRandom())),
                        ListItem(ULID.random(Instant.now().toEpochMilli(), SecureRandom()))
                    ), moveListener, pasteListener
                )
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
    }

    override fun onDetach() {
        super.onDetach()
        moveListener = null
        pasteListener = null
    }

    interface OnListItemMove {
        fun onListItemMove(item: ListItem?)
    }

    interface OnListItemPaste {
        fun onListItemPaste(item: ListItem?)
    }
}
