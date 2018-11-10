package me.proxer.app.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.view.clicks
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.uber.autodispose.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.newbase.paged.NewBasePagedListAdapter
import me.proxer.app.news.NewsAdapter.ViewHolder
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.convertToRelativeReadableTime
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.fastText
import me.proxer.app.util.extension.getSafeParcelable
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.setIconicsImage
import me.proxer.library.entity.notifications.NewsArticle
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class NewsAdapter(savedInstanceState: Bundle?) : NewBasePagedListAdapter<NewsArticle, ViewHolder>() {

    private companion object {
        private const val EXPANDED_STATE = "news_expansion_map"
    }

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<NewsArticle> = PublishSubject.create()
    val imageClickSubject: PublishSubject<Pair<ImageView, NewsArticle>> = PublishSubject.create()

    private val expansionMap: ParcelableStringBooleanMap

    init {
        expansionMap = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getSafeParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getSafeItem(position))

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        glide = null
    }

    override fun saveInstanceState(outState: Bundle) {
        outState.putParcelable(EXPANDED_STATE, expansionMap)
    }

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val expand: ImageButton by bindView(R.id.expand)
        internal val description: AppCompatTextView by bindView(R.id.description)
        internal val image: ImageView by bindView(R.id.image)
        internal val title: TextView by bindView(R.id.title)
        internal val category: TextView by bindView(R.id.category)
        internal val time: TextView by bindView(R.id.time)

        init {
            expand.setIconicsImage(CommunityMaterial.Icon.cmd_chevron_down, 32)
        }

        fun bind(item: NewsArticle) {
            initListeners()

            ViewCompat.setTransitionName(image, "news_${item.id}")

            title.text = item.subject
            description.fastText = item.description.trim()
            category.text = item.category
            time.text = item.date.convertToRelativeReadableTime(time.context)

            handleExpansion(item.id)

            glide?.defaultLoad(image, ProxerUrls.newsImage(item.id, item.image))
        }

        private fun initListeners() {
            itemView.clicks()
                .mapAdapterPosition({ positionResolver(adapterPosition) }) { getSafeItem(it) }
                .autoDisposable(this)
                .subscribe(clickSubject)

            image.clicks()
                .mapAdapterPosition({ positionResolver(adapterPosition) }) { image to getSafeItem(it) }
                .autoDisposable(this)
                .subscribe(imageClickSubject)

            expand.clicks()
                .mapAdapterPosition({ positionResolver(adapterPosition) }) { getSafeItem(it).id }
                .autoDisposable(this)
                .subscribe {
                    expansionMap.putOrRemove(it)

                    handleExpansion(it, true)
                }
        }

        private fun handleExpansion(itemId: String, animate: Boolean = false) {
            ViewCompat.animate(expand).cancel()

            if (expansionMap.containsKey(itemId)) {
                description.maxLines = Int.MAX_VALUE

                when (animate) {
                    true -> ViewCompat.animate(expand).rotation(180f)
                    false -> expand.rotation = 180f
                }
            } else {
                description.maxLines = 3

                when (animate) {
                    true -> ViewCompat.animate(expand).rotation(0f)
                    false -> expand.rotation = 0f
                }
            }

            expand.post {
                if (description.lineCount <= 3) {
                    expand.isGone = true
                } else {
                    expand.isVisible = true
                }
            }

            if (animate) {
                description.requestLayout()
                layoutManager?.requestSimpleAnimationsInNextLayout()
            }
        }
    }
}
