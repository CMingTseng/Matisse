package com.zhihu.matisse.internal.ui.widget

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import com.zhihu.matisse.R

class CheckRadioView : AppCompatImageView {
    private var checkRadioDrawable: Drawable? = null
    private var selectedColor = 0
    private var unSelectUdColor = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    fun setChecked(enable: Boolean) {
        if (enable) {
            setImageResource(R.drawable.ic_preview_radio_on)
            checkRadioDrawable = drawable
            checkRadioDrawable?.setColorFilter(selectedColor, PorterDuff.Mode.SRC_IN)
        } else {
            setImageResource(R.drawable.ic_preview_radio_off)
            checkRadioDrawable = drawable
            checkRadioDrawable?.setColorFilter(unSelectUdColor, PorterDuff.Mode.SRC_IN)
        }
    }

    fun setColor(color: Int) {
        if (checkRadioDrawable == null) {
            checkRadioDrawable = drawable
        }
        checkRadioDrawable!!.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    private fun init() {
        selectedColor = ResourcesCompat.getColor(
            resources, R.color.zhihu_item_checkCircle_backgroundColor,
            context.theme
        )
        unSelectUdColor = ResourcesCompat.getColor(
            resources, R.color.zhihu_check_original_radio_disable,
            context.theme
        )
        setChecked(false)
    }
}