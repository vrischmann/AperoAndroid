package fr.rischmann.apero


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fr.rischmann.apero.ListFragment.OnListFragmentInteractionListener
import kotlinx.android.synthetic.main.fragment_item.view.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ItemRecyclerViewAdapter(
    private val mValues: List<ListItem>,
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<ItemRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as ListItem
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mTimeView.text = formattedIDTime(item.id)

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mTimeView: TextView = mView.item_time
        val mPasteButton: ImageButton = mView.item_paste
    }

    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())

        private fun formattedIDTime(id: ID): String {
            val instant = Instant.ofEpochMilli(id.timestamp())
            return dateTimeFormatter.format(instant)
        }
    }
}
