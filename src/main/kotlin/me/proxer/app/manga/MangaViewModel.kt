package me.proxer.app.manga

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import me.proxer.app.auth.LoginEvent
import me.proxer.app.auth.LogoutEvent
import me.proxer.app.base.BaseViewModel
import me.proxer.app.exception.AgeConfirmationRequiredException
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.exception.PartialException
import me.proxer.app.settings.AgeConfirmationEvent
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.Utils
import me.proxer.app.util.data.ResettingMutableLiveData
import me.proxer.app.util.extension.buildOptionalSingle
import me.proxer.app.util.extension.buildPartialErrorSingle
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.isAgeRestricted
import me.proxer.app.util.extension.isOfficial
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.toMediaLanguage
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MangaViewModel(
    private val entryId: String,
    private val language: Language,
    episode: Int
) : BaseViewModel<MangaChapterInfo>() {

    override val dataSingle: Single<MangaChapterInfo>
        get() = Single.fromCallable { validate() }
            .flatMap<EntryCore> { entrySingle() }
            .doOnSuccess {
                if (it.isAgeRestricted) {
                    if (!storageHelper.isLoggedIn) {
                        throw NotLoggedInException()
                    } else if (!preferenceHelper.isAgeRestrictedMediaAllowed) {
                        throw AgeConfirmationRequiredException()
                    }
                }
            }
            .flatMap { entry ->
                chapterSingle(entry)
                    .doOnSuccess {
                        if (!it.chapter.isOfficial && !storageHelper.isLoggedIn) {
                            throw NotLoggedInException()
                        }
                    }
                    .map { data ->
                        if (data.chapter.isOfficial) {
                            throw PartialException(
                                MangaLinkException(data.chapter.title, Utils.getAndFixUrl(data.chapter.server)),
                                entry
                            )
                        } else if (data.chapter.pages == null) {
                            throw PartialException(MangaNotAvailableException(), entry)
                        }

                        data
                    }
            }

    val userStateData = ResettingMutableLiveData<Unit?>()
    val userStateError = ResettingMutableLiveData<ErrorUtils.ErrorAction?>()

    var episode by Delegates.observable(episode) { _, old, new ->
        if (old != new) reload()
    }

    private var cachedEntryCore: EntryCore? = null

    private var userStateDisposable: Disposable? = null

    init {
        disposables += bus.register(AgeConfirmationEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                // TODO: Simplify once proguard does not crash on this.
                val safeValue = error.value

                if (safeValue != null && safeValue.buttonAction == ButtonAction.AGE_CONFIRMATION) {
                    reload()
                }
            }

        disposables += Observable.merge(bus.register(LoginEvent::class.java), bus.register(LogoutEvent::class.java))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { reload() }
    }

    override fun onCleared() {
        userStateDisposable?.dispose()
        userStateDisposable = null

        super.onCleared()
    }

    fun setEpisode(value: Int, trigger: Boolean = true) {
        if (episode != value) {
            episode = value

            if (trigger) reload()
        }
    }

    fun markAsFinished() = updateUserState(api.info().markAsFinished(entryId))

    fun bookmark(episode: Int) = updateUserState(
        api.ucp().setBookmark(entryId, episode, language.toMediaLanguage(), Category.MANGA)
    )

    private fun entrySingle() = when (cachedEntryCore != null) {
        true -> Single.just(cachedEntryCore)
        false -> api.info().entryCore(entryId).buildSingle()
    }

    private fun chapterSingle(entry: EntryCore) = api.manga().chapter(entryId, episode, language)
        .buildPartialErrorSingle(entry)
        .map { MangaChapterInfo(it, entry.name, entry.episodeAmount) }

    @Suppress("ForbiddenVoid")
    private fun updateUserState(endpoint: Endpoint<Void>) {
        userStateDisposable?.dispose()
        userStateDisposable = Single.fromCallable { validators.validateLogin() }
            .flatMap { endpoint.buildOptionalSingle() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeAndLogErrors({
                userStateError.value = null
                userStateData.value = Unit
            }, {
                userStateData.value = null
                userStateError.value = ErrorUtils.handle(it)
            })
    }
}
