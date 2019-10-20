package fr.rischmann.apero


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_item.view.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ListItemRecyclerViewAdapter(
    private val mValues: List<ListItem>,
    private val moveListener: ListFragment.OnListItemMove?,
    private val pasteListener: ListFragment.OnListItemPaste?
) : RecyclerView.Adapter<ListItemRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]

        holder.mIDView.text = item.id.toString()
        holder.mTimeView.text = formattedULIDTime(item.id)

        with(holder.mMoveButton) {
            setOnClickListener {
                moveListener?.onListItemMove(item)
            }
        }
        with(holder.mPasteButton) {
            setOnClickListener {
                pasteListener?.onListItemPaste(item)
            }
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIDView: TextView = mView.item_id
        val mTimeView: TextView = mView.item_time
        val mMoveButton: ImageButton = mView.item_move
        val mPasteButton: ImageButton = mView.item_paste
    }

    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())

        private fun formattedULIDTime(id: ULID): String {
            val instant = Instant.ofEpochMilli(id.timestamp())
            return dateTimeFormatter.format(instant)
        }
    }
}
