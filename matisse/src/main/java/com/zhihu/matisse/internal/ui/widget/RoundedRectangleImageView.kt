package com.zhihu.matisse.internal.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class RoundedRectangleImageView : AppCompatImageView {
    private var radius = 0f //dp
    private var roundedRectPath: Path? = null
    private var rectF: RectF? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        rectF!![0.0f, 0.0f, measuredWidth.toFloat()] = measuredHeight.toFloat()
        roundedRectPath!!.addRoundRect(rectF!!, radius, radius, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.clipPath(roundedRectPath!!)
        super.onDraw(canvas)
    }

    private fun init(context: Context) {
        val density = context.resources.displayMetrics.density
        radius = 2.0f * density
        roundedRectPath = Path()
        rectF = RectF()
    }
}