package com.proxerme.app.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.bindView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.proxerme.app.R
import com.proxerme.app.util.TimeUtil
import com.proxerme.library.connection.ucp.entitiy.HistoryEntry
import com.proxerme.library.info.ProxerUrlHolder

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class HistoryAdapter(savedInstanceState: Bundle? = null) : PagingAdapter<HistoryEntry>() {

    private companion object {
        private const val ITEMS_STATE = "adapter_history_state_items"
    }

    var callback: OnHistoryEntryInteractionListener? = null

    init {
        savedInstanceState?.let {
            list.addAll(it.getParcelableArrayList(ITEMS_STATE))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            PagingViewHolder<HistoryEntry> {
        return HistoryViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_entry, parent, false))
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(ITEMS_STATE, list)
    }

    class HistoryViewHolder(itemView: View) : PagingViewHolder<HistoryEntry>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val status: TextView by bindView(R.id.status)

        override fun bind(item: HistoryEntry) {
            title.text = item.name
            medium.text = item.medium
            status.text = status.context.getString(R.string.history_entry_status, item.episode,
                    TimeUtil.convertToRelativeReadableTime(status.context, item.time))

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getCoverImageUrl(item.id).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    abstract class OnHistoryEntryInteractionListener {
        open fun onClick(v: View, entry: HistoryEntry) {

        }
    }
}