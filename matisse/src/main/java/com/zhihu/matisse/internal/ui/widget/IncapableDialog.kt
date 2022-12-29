package com.zhihu.matisse.internal.ui.widget

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.zhihu.matisse.R

class IncapableDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = requireArguments().getString(EXTRA_TITLE)
        val message = requireArguments().getString(EXTRA_MESSAGE)
        val builder = AlertDialog.Builder(requireActivity())
            .setPositiveButton(R.string.button_ok) { dialog, _ -> dialog.dismiss() }
        if (!title.isNullOrEmpty()) {
            builder.setTitle(title)
        }
        if (!message.isNullOrEmpty()) {
            builder.setMessage(message)
        }
        return builder.create()
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"

        fun newInstance(title: String?, message: String?): IncapableDialog {
            val args = Bundle().apply {
                putString(EXTRA_TITLE, title)
                putString(EXTRA_MESSAGE, message)
            }
            val dialog = IncapableDialog().apply {
                arguments = args
            }
            return dialog
        }
    }
}