package fr.rischmann.apero

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import fr.rischmann.apero.Logging.TAG

class CopyFragment : Fragment() {
    private var copyListener: OnCopyListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_copy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editText = view.findViewById<EditText>(R.id.copyEditText)
        savedInstanceState?.let {
            val s = it.getString("content")
            Log.d(TAG, "saved content: $s")
        }

        val copyButton = view.findViewById<Button>(R.id.copyButton)
        copyButton.setOnClickListener {
            val editable = editText.text

            copyListener?.onCopy(editable.toString())

            editable.clear()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnCopyListener) {
            copyListener = context
        } else {
            throw RuntimeException("$context must implement OnCopyListener")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        view?.findViewById<EditText>(R.id.copyEditText)?.let {
            val s = it.text.toString()
            Log.d(TAG, "saving content: $s")
            outState.putString("content", s)
        }
    }

    interface OnCopyListener {
        fun onCopy(text: String)
    }
}
