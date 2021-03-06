package me.proxer.app.chat.prv.conference

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.transition.TransitionManager
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChangeEvents
import com.jakewharton.rxbinding2.view.actionViewEvents
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotterknife.bindView
import me.proxer.app.GlideApp
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.chat.prv.ConferenceWithMessage
import me.proxer.app.chat.prv.create.CreateConferenceActivity
import me.proxer.app.chat.prv.message.MessengerActivity
import me.proxer.app.chat.prv.sync.MessengerNotifications
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.extension.doAfterAnimations
import me.proxer.app.util.extension.isAtTop
import me.proxer.app.util.extension.scrollToTop
import me.proxer.app.util.extension.unsafeLazy
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ConferenceFragment : BaseContentFragment<List<ConferenceWithMessage>>() {

    companion object {
        private const val SEARCH_QUERY_ARGUMENT = "search_query"

        fun newInstance() = ConferenceFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by viewModel<ConferenceViewModel> { parametersOf(searchQuery ?: "") }

    private var adapter by Delegates.notNull<ConferenceAdapter>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val toolbar by unsafeLazy { requireActivity().findViewById<Toolbar>(R.id.toolbar) }
    private val recyclerView: RecyclerView by bindView(R.id.recyclerView)

    private var searchQuery: String?
        get() = requireArguments().getString(SEARCH_QUERY_ARGUMENT, null)
        set(value) {
            requireArguments().putString(SEARCH_QUERY_ARGUMENT, value)

            shouldScrollToTop = true

            viewModel.searchQuery = value ?: ""
        }

    private var shouldScrollToTop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ConferenceAdapter(storageHelper)

        adapter.clickSubject
            .autoDisposable(this.scope())
            .subscribe { (conference) -> MessengerActivity.navigateTo(requireActivity(), conference) }

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_conferences, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.glide = GlideApp.with(this)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = StaggeredGridLayoutManager(
            DeviceUtils.calculateSpanAmount(requireActivity()),
            StaggeredGridLayoutManager.VERTICAL
        )
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()

        MessengerNotifications.cancel(requireContext())

        bus.register(ConferenceFragmentPingEvent::class.java)
            .autoDisposable(this.scope())
            .subscribe()
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, context, R.menu.fragment_conferences, menu, true)

        menu.findItem(R.id.search).let { searchItem ->
            val searchView = searchItem.actionView as SearchView

            searchItem.actionViewEvents()
                .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                .subscribe {
                    if (it.menuItem().isActionViewExpanded) {
                        searchQuery = null
                    }

                    TransitionManager.beginDelayedTransition(toolbar)
                }

            searchView.queryTextChangeEvents()
                .skipInitialValue()
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(viewLifecycleOwner.scope(Lifecycle.Event.ON_DESTROY))
                .subscribe {
                    searchQuery = it.queryText().toString().trim()

                    if (it.isSubmitted) {
                        searchView.clearFocus()
                    }
                }

            if (searchQuery.isNullOrBlank().not()) {
                searchItem.expandActionView()
                searchView.setQuery(searchQuery, false)
                searchView.clearFocus()
            }
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.create_chat -> CreateConferenceActivity.navigateTo(requireActivity(), false)
            R.id.new_group -> CreateConferenceActivity.navigateTo(requireActivity(), true)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun showData(data: List<ConferenceWithMessage>) {
        super.showData(data)

        val wasAtFirstPosition = recyclerView.isAtTop()

        adapter.swapDataAndNotifyWithDiffing(data)

        when {
            adapter.isEmpty() -> when (searchQuery.isNullOrBlank()) {
                true -> showError(ErrorAction(R.string.error_no_data_conferences, ACTION_MESSAGE_HIDE))
                false -> showError(ErrorAction(R.string.error_no_data_search, ACTION_MESSAGE_HIDE))
            }
            shouldScrollToTop -> recyclerView.scrollToTop()
            wasAtFirstPosition -> recyclerView.doAfterAnimations { recyclerView.smoothScrollToPosition(0) }
        }

        shouldScrollToTop = false
    }

    override fun hideData() {
        adapter.swapDataAndNotifyWithDiffing(emptyList())

        recyclerView.scrollToTop()

        super.hideData()
    }

    override fun showError(action: ErrorAction) {
        super.showError(action)

        if (adapter.isEmpty()) {
            recyclerView.scrollToTop()
        }
    }
}
