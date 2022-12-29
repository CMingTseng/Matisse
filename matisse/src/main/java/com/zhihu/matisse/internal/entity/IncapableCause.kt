package com.zhihu.matisse.internal.entity

import android.content.Context
import android.widget.Toast
import androidx.annotation.IntDef
import androidx.fragment.app.FragmentActivity
import com.zhihu.matisse.internal.ui.widget.IncapableDialog

class IncapableCause(
    private val message: String,
    private val title: String? = null,
    @Form private val form: Int = TOAST,
) {

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(TOAST, DIALOG, NONE)
    annotation class Form

    companion object {
        const val TOAST = 0x00
        const val DIALOG = 0x01
        const val NONE = 0x02

        fun handleCause(context: Context, cause: IncapableCause?) {
            if (cause == null) return
            when (cause.form) {
                NONE -> {
                }
                DIALOG -> {
                    val incapableDialog = IncapableDialog.newInstance(cause.title, cause.message)
                    incapableDialog.show(
                        (context as FragmentActivity).supportFragmentManager,
                        IncapableDialog::class.java.name
                    )
                }
                TOAST -> Toast.makeText(context, cause.message, Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(context, cause.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}