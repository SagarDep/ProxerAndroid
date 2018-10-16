package me.proxer.app.chat.pub.message

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxbinding2.widget.editorActionEvents
import com.jakewharton.rxbinding2.widget.textChanges
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.functions.Predicate
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseDialog
import me.proxer.app.util.extension.getSafeString
import me.proxer.app.util.extension.safeText
import me.proxer.app.util.extension.toast
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Ruben Gees
 */
class ChatReportDialog : BaseDialog() {

    companion object {
        private const val MESSAGE_ID_ARGUMENT = "message_id"

        fun show(activity: AppCompatActivity, messageId: String) = ChatReportDialog().apply {
            arguments = bundleOf(MESSAGE_ID_ARGUMENT to messageId)
        }.show(activity.supportFragmentManager, "chat_report_dialog")
    }

    private val viewModel by viewModel<ChatReportViewModel>()

    private val messageInput: EditText by bindView(R.id.message)
    private val messageContainer: TextInputLayout by bindView(R.id.messageContainer)
    private val inputContainer: ViewGroup by bindView(R.id.inputContainer)
    private val progress: ProgressBar by bindView(R.id.progress)

    private val messageId: String
        get() = requireArguments().getSafeString(MESSAGE_ID_ARGUMENT)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = MaterialDialog(requireContext())
        .noAutoDismiss()
        .title(R.string.dialog_chat_report_title)
        .positiveButton(R.string.dialog_chat_report_positive) { validateAndSendReport() }
        .negativeButton(R.string.cancel) { dismiss() }
        .customView(R.layout.dialog_chat_report, scrollable = true)

    override fun onDialogCreated(savedInstanceState: Bundle?) {
        super.onDialogCreated(savedInstanceState)

        if (savedInstanceState == null) {
            messageInput.requestFocus()

            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }

        messageInput.editorActionEvents(Predicate { event -> event.actionId() == EditorInfo.IME_ACTION_GO })
            .filter { event -> event.actionId() == EditorInfo.IME_ACTION_GO }
            .autoDisposable(dialogLifecycleOwner.scope())
            .subscribe { validateAndSendReport() }

        messageInput.textChanges()
            .skipInitialValue()
            .autoDisposable(dialogLifecycleOwner.scope())
            .subscribe { setError(messageContainer, null) }

        viewModel.data.observe(dialogLifecycleOwner, Observer {
            it?.let { dismiss() }
        })

        viewModel.error.observe(dialogLifecycleOwner, Observer {
            it?.let {
                viewModel.error.value = null

                requireContext().toast(it.message)
            }
        })

        viewModel.isLoading.observe(dialogLifecycleOwner, Observer {
            inputContainer.isGone = it == true
            progress.isVisible = it == true
        })
    }

    private fun validateAndSendReport() {
        val message = messageInput.safeText.trim().toString()

        if (validateInput(message)) {
            viewModel.sendReport(messageId, message)
        }
    }

    private fun validateInput(message: String) = when {
        message.isBlank() -> {
            setError(messageContainer, getString(R.string.dialog_chat_error_message))

            false
        }
        else -> true
    }

    private fun setError(container: TextInputLayout, errorText: String?) {
        container.isErrorEnabled = errorText != null
        container.error = errorText
    }
}
