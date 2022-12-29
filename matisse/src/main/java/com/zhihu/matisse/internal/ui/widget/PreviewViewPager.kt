package com.zhihu.matisse.internal.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.viewpager.widget.ViewPager
import it.sephiroth.android.library.imagezoom.ImageViewTouch

class PreviewViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {

    override fun canScroll(v: View, checkV: Boolean, dx: Int, x: Int, y: Int): Boolean {
        return if (v is ImageViewTouch) {
            v.canScroll(dx) || super.canScroll(v, checkV, dx, x, y)
        } else {
            super.canScroll(v, checkV, dx, x, y)
        }
    }
}