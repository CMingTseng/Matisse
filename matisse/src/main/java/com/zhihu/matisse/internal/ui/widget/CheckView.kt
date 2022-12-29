package com.zhihu.matisse.internal.ui.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.zhihu.matisse.R

class CheckView : View {
    private var countable = false
    private var checked = false
    private var checkedNum = 0
    private var strokePaint: Paint? = null
    private var backgroundPaint: Paint? = null
    private var textPaint: TextPaint? = null
    private var shadowPaint: Paint? = null
    private var checkDrawable: Drawable? = null
    private var density = 0f
    private var checkRectRef: Rect? = null
    private var enabled = true

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
        // fixed size 48dp x 48dp
        val sizeSpec = MeasureSpec.makeMeasureSpec((SIZE * density).toInt(), MeasureSpec.EXACTLY)
        super.onMeasure(sizeSpec, sizeSpec)
    }

    private fun init(context: Context) {
        density = context.resources.displayMetrics.density
        strokePaint = Paint()
        strokePaint!!.isAntiAlias = true
        strokePaint!!.style = Paint.Style.STROKE
        strokePaint!!.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        strokePaint!!.strokeWidth = STROKE_WIDTH * density
        val ta =
            getContext().theme.obtainStyledAttributes(intArrayOf(R.attr.item_checkCircle_borderColor))
        val defaultColor = ResourcesCompat.getColor(
            resources, R.color.zhihu_item_checkCircle_borderColor,
            getContext().theme
        )
        val color = ta.getColor(0, defaultColor)
        ta.recycle()
        strokePaint!!.color = color
        checkDrawable = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.ic_check_white_18dp, context.theme
        )
    }

    fun setChecked(checked: Boolean) {
        check(!countable) { "CheckView is countable, call setCheckedNum() instead." }
        this.checked = checked
        invalidate()
    }

    fun setCountable(countable: Boolean) {
        this.countable = countable
    }

    fun setCheckedNum(checkedNum: Int) {
        check(countable) { "CheckView is not countable, call setChecked() instead." }
        require(!(checkedNum != UNCHECKED && checkedNum <= 0)) { "checked num can't be negative." }
        this.checkedNum = checkedNum
        invalidate()
    }

    override fun setEnabled(enabled: Boolean) {
        if (this.enabled != enabled) {
            this.enabled = enabled
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // draw outer and inner shadow
        initShadowPaint()
        canvas.drawCircle(
            SIZE.toFloat() * density / 2, SIZE.toFloat() * density / 2,
            (STROKE_RADIUS + STROKE_WIDTH / 2 + SHADOW_WIDTH) * density, shadowPaint!!
        )

        // draw white stroke
        canvas.drawCircle(
            SIZE.toFloat() * density / 2, SIZE.toFloat() * density / 2,
            STROKE_RADIUS * density, strokePaint!!
        )

        // draw content
        if (countable) {
            if (checkedNum != UNCHECKED) {
                initBackgroundPaint()
                canvas.drawCircle(
                    SIZE.toFloat() * density / 2, SIZE.toFloat() * density / 2,
                    BG_RADIUS * density, backgroundPaint!!
                )
                initTextPaint()
                val text = checkedNum.toString()
                val baseX = (canvas.width - textPaint!!.measureText(text)).toInt() / 2
                val baseY =
                    (canvas.height - textPaint!!.descent() - textPaint!!.ascent()).toInt() / 2
                canvas.drawText(text, baseX.toFloat(), baseY.toFloat(), textPaint!!)
            }
        } else {
            if (checked) {
                initBackgroundPaint()
                canvas.drawCircle(
                    SIZE.toFloat() * density / 2, SIZE.toFloat() * density / 2,
                    BG_RADIUS * density, backgroundPaint!!
                )
                checkDrawable!!.bounds = checkRect
                checkDrawable!!.draw(canvas)
            }
        }

        // enable hint
        alpha = if (enabled) 1.0f else 0.5f
    }

    private fun initShadowPaint() {
        if (shadowPaint == null) {
            shadowPaint = Paint()
            shadowPaint!!.isAntiAlias = true
            // all in dp
            val outerRadius = STROKE_RADIUS + STROKE_WIDTH / 2
            val innerRadius = outerRadius - STROKE_WIDTH
            val gradientRadius = outerRadius + SHADOW_WIDTH
            val stop0 = (innerRadius - SHADOW_WIDTH) / gradientRadius
            val stop1 = innerRadius / gradientRadius
            val stop2 = outerRadius / gradientRadius
            val stop3 = 1.0f
            shadowPaint!!.shader = RadialGradient(
                SIZE.toFloat() * density / 2,
                SIZE.toFloat() * density / 2,
                gradientRadius * density, intArrayOf(
                    Color.parseColor("#00000000"), Color.parseColor("#0D000000"),
                    Color.parseColor("#0D000000"), Color.parseColor("#00000000")
                ), floatArrayOf(stop0, stop1, stop2, stop3),
                Shader.TileMode.CLAMP
            )
        }
    }

    private fun initBackgroundPaint() {
        if (backgroundPaint == null) {
            backgroundPaint = Paint()
            backgroundPaint!!.isAntiAlias = true
            backgroundPaint!!.style = Paint.Style.FILL
            val ta = context.theme
                .obtainStyledAttributes(intArrayOf(R.attr.item_checkCircle_backgroundColor))
            val defaultColor = ResourcesCompat.getColor(
                resources, R.color.zhihu_item_checkCircle_backgroundColor,
                context.theme
            )
            val color = ta.getColor(0, defaultColor)
            ta.recycle()
            backgroundPaint!!.color = color
        }
    }

    private fun initTextPaint() {
        if (textPaint == null) {
            textPaint = TextPaint()
            textPaint!!.isAntiAlias = true
            textPaint!!.color = Color.WHITE
            textPaint!!.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint!!.textSize = 12.0f * density
        }
    }

    // rect for drawing checked number or mark
    private val checkRect: Rect
        get() {
            if (checkRectRef == null) {
                val rectPadding = (SIZE * density / 2 - CONTENT_SIZE * density / 2).toInt()
                checkRectRef = Rect(
                    rectPadding, rectPadding,
                    (SIZE * density - rectPadding).toInt(), (SIZE * density - rectPadding).toInt()
                )
            }
            return checkRectRef!!
        }

    companion object {
        const val UNCHECKED = Int.MIN_VALUE

        private const val STROKE_WIDTH = 3.0f // dp
        private const val SHADOW_WIDTH = 6.0f // dp
        private const val SIZE = 48 // dp
        private const val STROKE_RADIUS = 11.5f // dp
        private const val BG_RADIUS = 11.0f // dp
        private const val CONTENT_SIZE = 16 // dp
    }
}