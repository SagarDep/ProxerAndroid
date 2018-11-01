package me.proxer.app.ucp.settings

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.preference.Preference
import changes
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import me.proxer.app.R
import me.proxer.app.util.KotterKnifePreference
import me.proxer.app.util.bindPreference
import me.proxer.library.enums.UcpSettingConstraint
import me.proxer.library.util.ProxerUtils
import net.xpece.android.support.preference.ListPreference
import net.xpece.android.support.preference.XpPreferenceFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

/**
 * @author Ruben Gees
 */
class UcpSettingsFragment : XpPreferenceFragment() {

    companion object {
        fun newInstance() = UcpSettingsFragment().apply {
            arguments = bundleOf()
        }
    }

    private val viewModel by sharedViewModel<UcpSettingsViewModel>()

    private val profile by bindPreference<ListPreference>("profile")
    private val topten by bindPreference<ListPreference>("topten")
    private val anime by bindPreference<ListPreference>("anime")
    private val manga by bindPreference<ListPreference>("manga")
    private val comment by bindPreference<ListPreference>("comment")
    private val forum by bindPreference<ListPreference>("forum")
    private val friend by bindPreference<ListPreference>("friend")
    private val friendRequest by bindPreference<ListPreference>("friend_request")
    private val about by bindPreference<ListPreference>("about")
    private val history by bindPreference<ListPreference>("history")
    private val guestBook by bindPreference<ListPreference>("guest_book")
    private val guestBookEntry by bindPreference<ListPreference>("guest_book_entry")
    private val gallery by bindPreference<ListPreference>("gallery")
    private val article by bindPreference<ListPreference>("article")

    override fun onCreatePreferences2(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.profile_preferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.data.observe(viewLifecycleOwner, Observer {
            showData(it)
        })

        initPreference(profile) { settings, constraint -> settings.copy(profileVisibility = constraint) }
        initPreference(topten) { settings, constraint -> settings.copy(topTenVisibility = constraint) }
        initPreference(anime) { settings, constraint -> settings.copy(animeVisibility = constraint) }
        initPreference(manga) { settings, constraint -> settings.copy(mangaVisibility = constraint) }
        initPreference(comment) { settings, constraint -> settings.copy(commentVisibility = constraint) }
        initPreference(forum) { settings, constraint -> settings.copy(forumVisibility = constraint) }
        initPreference(friend) { settings, constraint -> settings.copy(friendVisibility = constraint) }
        initPreference(friendRequest) { settings, constraint -> settings.copy(friendRequestConstraint = constraint) }
        initPreference(about) { settings, constraint -> settings.copy(aboutVisibility = constraint) }
        initPreference(history) { settings, constraint -> settings.copy(historyVisibility = constraint) }
        initPreference(guestBook) { settings, constraint -> settings.copy(guestBookVisibility = constraint) }
        initPreference(guestBookEntry) { settings, constraint -> settings.copy(guestBookEntryConstraint = constraint) }
        initPreference(gallery) { settings, constraint -> settings.copy(galleryVisibility = constraint) }
        initPreference(article) { settings, constraint -> settings.copy(articleVisibility = constraint) }
    }

    override fun onDestroyView() {
        KotterKnifePreference.reset(this)

        super.onDestroyView()
    }

    private fun showData(ucpSettings: LocalUcpSettings) {
        profile.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.profileVisibility))
        topten.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.topTenVisibility))
        anime.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.animeVisibility))
        manga.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.mangaVisibility))
        comment.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.commentVisibility))
        forum.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.forumVisibility))
        friend.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.friendVisibility))
        friendRequest.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.friendRequestConstraint))
        about.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.aboutVisibility))
        history.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.historyVisibility))
        guestBook.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.guestBookVisibility))
        guestBookEntry.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.guestBookEntryConstraint))
        gallery.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.galleryVisibility))
        article.setValue(ProxerUtils.getSafeApiEnumName(ucpSettings.articleVisibility))
    }

    private fun initPreference(
        preference: Preference,
        copyCallback: (LocalUcpSettings, UcpSettingConstraint) -> LocalUcpSettings
    ) = preference.changes()
        .autoDisposable(this.scope())
        .subscribe {
            val currentSettings = viewModel.data.value

            if (currentSettings != null) {
                val newConstraint = ProxerUtils.toSafeApiEnum(UcpSettingConstraint::class.java, it)
                val newSettings = copyCallback(currentSettings, newConstraint)

                viewModel.update(newSettings)
            }
        }
}
