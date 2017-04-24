package me.proxer.app.adapter.manga

import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import me.proxer.app.R
import me.proxer.app.adapter.base.PagingAdapter
import me.proxer.app.adapter.manga.LocalMangaEntryAdapter.LocalMangaEntryAdapterCallback
import me.proxer.app.entity.LocalMangaChapter
import me.proxer.app.util.PaddingDividerItemDecoration
import me.proxer.app.util.ParcelableStringBooleanMap
import me.proxer.app.util.extension.CompleteLocalMangaEntry
import me.proxer.app.util.extension.bindView
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.util.ProxerUrls


/**
 * @author Ruben Gees
 */
class LocalMangaAdapter(savedInstanceState: Bundle?, private val glide: RequestManager) :
        PagingAdapter<CompleteLocalMangaEntry>() {

    private companion object {
        private const val EXPANDED_STATE = "local_manga_expanded"
    }

    private val expanded: ParcelableStringBooleanMap

    var callback: LocalMangaAdapterCallback? = null

    init {
        expanded = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = internalList[position].first.id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_local_manga, parent, false))

    override fun onViewAttachedToWindow(holder: PagingViewHolder<CompleteLocalMangaEntry>) {
        super.onViewAttachedToWindow(holder)

        if (holder is ViewHolder) {
            holder.adapter.callback = object : LocalMangaEntryAdapterCallback {
                override fun onChapterClick(chapter: LocalMangaChapter) = holder.adapterPosition.let {
                    if (it != RecyclerView.NO_POSITION) {
                        this@LocalMangaAdapter.callback?.onChapterClick(internalList[it].first, chapter)
                    }
                }

                override fun onDeleteClick(chapter: LocalMangaChapter) = holder.adapterPosition.let {
                    if (it != RecyclerView.NO_POSITION) {
                        this@LocalMangaAdapter.callback?.onDeleteClick(internalList[it].first, chapter)
                    }
                }
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: PagingViewHolder<CompleteLocalMangaEntry>?) {
        if (holder is ViewHolder) {
            holder.adapter.destroy()
        }
    }

    override fun destroy() {
        super.destroy()

        callback = null
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(EXPANDED_STATE, expanded)
    }

    inner class ViewHolder(itemView: View) : PagingViewHolder<CompleteLocalMangaEntry>(itemView) {

        internal val adapter: LocalMangaEntryAdapter
            get() = chapters.adapter as LocalMangaEntryAdapter

        private val image: ImageView by bindView(R.id.image)
        private val title: TextView by bindView(R.id.title)
        private val chapters: RecyclerView by bindView(R.id.chapters)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition {
                    val id = internalList[it].first.id

                    if (expanded[id] ?: false) {
                        expanded.remove(id)
                    } else {
                        expanded.put(id, true)
                    }

                    notifyItemChanged(it)
                }
            }

            itemView.setOnLongClickListener { view ->
                withSafeAdapterPosition {
                    callback?.onChapterLongClick(view, internalList[it].first)
                }

                true
            }

            chapters.isNestedScrollingEnabled = false
            chapters.adapter = LocalMangaEntryAdapter()
            chapters.layoutManager = LinearLayoutManager(itemView.context)
            chapters.addItemDecoration(PaddingDividerItemDecoration(chapters.context, 4f))
        }

        override fun bind(item: CompleteLocalMangaEntry) {
            ViewCompat.setTransitionName(image, "local_manga_${item.first.id}")

            title.text = item.first.name

            if (expanded[item.first.id] ?: false) {
                chapters.visibility = View.VISIBLE
                adapter.replace(item.second)
            } else {
                chapters.visibility = View.GONE
                adapter.clear()
            }

            glide.load(ProxerUrls.entryImage(item.first.id).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    interface LocalMangaAdapterCallback {
        fun onChapterClick(entry: EntryCore, chapter: LocalMangaChapter) {}
        fun onChapterLongClick(view: View, entry: EntryCore) {}
        fun onDeleteClick(entry: EntryCore, chapter: LocalMangaChapter) {}
    }
}