package com.zhihu.matisse.internal.ui.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec

class MediaGrid : SquareFrameLayout, View.OnClickListener {
    private var thumbnail: ImageView? = null
    private var checkView: CheckView? = null
    private var gifTag: ImageView? = null
    private var videoDuration: TextView? = null
    private var preBindInfo: PreBindInfo? = null
    private var listener: OnMediaGridClickListener? = null

    var media: Item? = null
        private set

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    override fun onClick(v: View) {
        if (listener != null) {
            if (v === thumbnail) {
                listener!!.onThumbnailClicked(thumbnail!!, media!!, preBindInfo!!.viewHolder)
            } else if (v === checkView) {
                listener!!.onCheckViewClicked(checkView!!, media!!, preBindInfo!!.viewHolder)
            }
        }
    }

    fun preBindMedia(info: PreBindInfo?) {
        preBindInfo = info
    }

    fun bindMedia(item: Item?) {
        media = item
        setGifTag()
        initCheckView()
        setImage()
        setVideoDuration()
    }

    private fun setGifTag() {
        gifTag!!.visibility = if (media!!.isGif) VISIBLE else GONE
    }

    private fun initCheckView() {
        checkView!!.setCountable(preBindInfo!!.checkViewCountable)
    }

    fun setCheckEnabled(enabled: Boolean) {
        checkView!!.isEnabled = enabled
    }

    fun setCheckedNum(checkedNum: Int) {
        checkView!!.setCheckedNum(checkedNum)
    }

    fun setChecked(checked: Boolean) {
        checkView!!.setChecked(checked)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.media_grid_content, this, true)
        thumbnail = findViewById<View>(R.id.media_thumbnail) as ImageView
        checkView = findViewById<View>(R.id.check_view) as CheckView
        gifTag = findViewById<View>(R.id.gif) as ImageView
        videoDuration = findViewById<View>(R.id.video_duration) as TextView
        thumbnail!!.setOnClickListener(this)
        checkView!!.setOnClickListener(this)
    }

    private fun setImage() {
        if (media!!.isGif) {
            SelectionSpec.imageEngine?.loadGifThumbnail(
                context, preBindInfo!!.resize,
                preBindInfo!!.placeholder, thumbnail!!, media!!.contentUri
            )
        } else {
            SelectionSpec.imageEngine?.loadThumbnail(
                context, preBindInfo!!.resize,
                preBindInfo!!.placeholder, thumbnail!!, media!!.contentUri
            )
        }
    }

    private fun setVideoDuration() {
        if (media!!.isVideo) {
            videoDuration!!.visibility = VISIBLE
            videoDuration!!.text = DateUtils.formatElapsedTime(media!!.duration / 1000)
        } else {
            videoDuration!!.visibility = GONE
        }
    }

    fun setOnMediaGridClickListener(listener: OnMediaGridClickListener?) {
        this.listener = listener
    }

    fun removeOnMediaGridClickListener() {
        listener = null
    }

    interface OnMediaGridClickListener {
        fun onThumbnailClicked(thumbnail: ImageView, item: Item, holder: ViewHolder)
        fun onCheckViewClicked(checkView: CheckView, item: Item, holder: ViewHolder)
    }

    data class PreBindInfo(
        val resize: Int,
        val placeholder: Drawable?,
        val checkViewCountable: Boolean,
        val viewHolder: ViewHolder
    )
}