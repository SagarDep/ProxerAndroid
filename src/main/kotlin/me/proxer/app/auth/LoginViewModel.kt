package me.proxer.app.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.buildSingle
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.api.ProxerApi
import me.proxer.library.api.ProxerException
import me.proxer.library.api.ProxerException.ServerErrorType
import me.proxer.library.entity.user.User
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * @author Ruben Gees
 */
class LoginViewModel : ViewModel(), KoinComponent {

    val data = MutableLiveData<User?>()
    val error = MutableLiveData<ErrorUtils.ErrorAction?>()
    val isLoading = MutableLiveData<Boolean?>()
    val isTwoFactorAuthenticationEnabled = MutableLiveData<Boolean?>()

    private val api by inject<ProxerApi>()
    private val storageHelper by inject<StorageHelper>()

    private var dataDisposable: Disposable? = null

    init {
        isTwoFactorAuthenticationEnabled.value = storageHelper.isTwoFactorAuthenticationEnabled
    }

    override fun onCleared() {
        dataDisposable?.dispose()
        dataDisposable = null

        super.onCleared()
    }

    fun login(username: String, password: String, secretKey: String?) {
        if (isLoading.value != true) {
            dataDisposable?.dispose()
            dataDisposable = api.user().login(username, password)
                .secretKey(secretKey)
                .buildSingle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    error.value = null
                    isLoading.value = true
                }
                .doAfterTerminate { isLoading.value = false }
                .subscribeAndLogErrors({
                    data.value = it
                }, {
                    if (it is ProxerException && it.serverErrorType == ServerErrorType.USER_2FA_SECRET_REQUIRED) {
                        storageHelper.isTwoFactorAuthenticationEnabled = true
                        isTwoFactorAuthenticationEnabled.value = true
                    }

                    error.value = ErrorUtils.handle(it)
                })
        }
    }
}
