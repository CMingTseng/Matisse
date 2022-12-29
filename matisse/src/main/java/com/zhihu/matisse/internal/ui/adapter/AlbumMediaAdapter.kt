package com.zhihu.matisse.internal.ui.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.entity.IncapableCause
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.internal.ui.widget.CheckView
import com.zhihu.matisse.internal.ui.widget.MediaGrid
import com.zhihu.matisse.internal.ui.widget.MediaGrid.OnMediaGridClickListener
import com.zhihu.matisse.internal.ui.widget.MediaGrid.PreBindInfo

class AlbumMediaAdapter(
    context: Context,
    private val selectedCollection: SelectedItemCollection,
    private val recyclerView: RecyclerView
) : RecyclerViewCursorAdapter<ViewHolder>(null), OnMediaGridClickListener {

    private val selectionSpec: SelectionSpec = SelectionSpec
    private val placeholder: Drawable?

    private var imageResize = 0
    private var checkStateListener: CheckStateListener? = null
    private var onMediaClickListener: OnMediaClickListener? = null

    init {
        val ta = context.theme.obtainStyledAttributes(intArrayOf(R.attr.item_placeholder))
        placeholder = ta.getDrawable(0)
        ta.recycle()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        when (viewType) {
            VIEW_TYPE_CAPTURE -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.photo_capture_item, parent, false)
                val holder = CaptureViewHolder(v)
                holder.itemView.setOnClickListener { view ->
                    if (view.context is OnPhotoCapture) {
                        (view.context as OnPhotoCapture).capture()
                    }
                }
                return holder
            }
            VIEW_TYPE_MEDIA -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.media_grid_item, parent, false)
                return MediaViewHolder(v)
            }
        }

        throw IllegalArgumentException("Doesn't support view type.")
    }

    override fun onBindViewHolder(holder: ViewHolder, cursor: Cursor) {
        if (holder is CaptureViewHolder) {
            val drawables = holder.hint.compoundDrawables
            val ta =
                holder.itemView.context.theme.obtainStyledAttributes(intArrayOf(R.attr.capture_textColor))
            val color = ta.getColor(0, 0)
            ta.recycle()
            for (i in drawables.indices) {
                val drawable = drawables[i]
                if (drawable != null) {
                    val state = drawable.constantState ?: continue
                    val newDrawable = state.newDrawable().mutate()
                    newDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                    newDrawable.bounds = drawable.bounds
                    drawables[i] = newDrawable
                }
            }
            holder.hint.setCompoundDrawables(
                drawables[0],
                drawables[1],
                drawables[2],
                drawables[3]
            )
        } else if (holder is MediaViewHolder) {
            val item = Item.valueOf(cursor)
            holder.mediaGrid.preBindMedia(
                PreBindInfo(
                    getImageResize(holder.mediaGrid.context),
                    placeholder,
                    selectionSpec.countable,
                    holder
                )
            )
            holder.mediaGrid.bindMedia(item)
            holder.mediaGrid.setOnMediaGridClickListener(this)
            setCheckStatus(item, holder.mediaGrid)
        }
    }

    private fun setCheckStatus(item: Item, mediaGrid: MediaGrid) {
        if (selectionSpec.countable) {
            val checkedNum = selectedCollection.checkedNumOf(item)
            if (checkedNum > 0) {
                mediaGrid.setCheckEnabled(true)
                mediaGrid.setCheckedNum(checkedNum)
            } else {
                if (selectedCollection.maxSelectableReached()) {
                    mediaGrid.setCheckEnabled(false)
                    mediaGrid.setCheckedNum(CheckView.UNCHECKED)
                } else {
                    mediaGrid.setCheckEnabled(true)
                    mediaGrid.setCheckedNum(checkedNum)
                }
            }
        } else {
            val selected = selectedCollection.isSelected(item)
            if (selected) {
                mediaGrid.setCheckEnabled(true)
                mediaGrid.setChecked(true)
            } else {
                if (selectedCollection.maxSelectableReached()) {
                    mediaGrid.setCheckEnabled(false)
                    mediaGrid.setChecked(false)
                } else {
                    mediaGrid.setCheckEnabled(true)
                    mediaGrid.setChecked(false)
                }
            }
        }
    }

    override fun onThumbnailClicked(thumbnail: ImageView, item: Item, holder: ViewHolder) {
        if (selectionSpec.showPreview) {
            onMediaClickListener?.onMediaClick(null, item, holder.adapterPosition)
        } else {
            updateSelectedItem(item, holder)
        }
    }

    override fun onCheckViewClicked(checkView: CheckView, item: Item, holder: ViewHolder) {
        updateSelectedItem(item, holder)
    }

    private fun updateSelectedItem(item: Item, holder: ViewHolder) {
        if (selectionSpec.countable) {
            val checkedNum = selectedCollection.checkedNumOf(item)
            if (checkedNum == CheckView.UNCHECKED) {
                if (assertAddSelection(holder.itemView.context, item)) {
                    selectedCollection.add(item)
                    notifyCheckStateChanged()
                }
            } else {
                selectedCollection.remove(item)
                notifyCheckStateChanged()
            }
        } else {
            if (selectedCollection.isSelected(item)) {
                selectedCollection.remove(item)
                notifyCheckStateChanged()
            } else {
                if (assertAddSelection(holder.itemView.context, item)) {
                    selectedCollection.add(item)
                    notifyCheckStateChanged()
                }
            }
        }
    }

    private fun notifyCheckStateChanged() {
        notifyDataSetChanged()
        if (checkStateListener != null) {
            checkStateListener!!.onUpdate()
        }
    }

    public override fun getItemViewType(position: Int, cursor: Cursor): Int {
        return if (Item.valueOf(cursor).isCapture) VIEW_TYPE_CAPTURE else VIEW_TYPE_MEDIA
    }

    private fun assertAddSelection(context: Context, item: Item): Boolean {
        val cause = selectedCollection.isAcceptable(item)
        IncapableCause.handleCause(context, cause)
        return cause == null
    }

    fun registerCheckStateListener(listener: CheckStateListener?) {
        checkStateListener = listener
    }

    fun unregisterCheckStateListener() {
        checkStateListener = null
    }

    fun registerOnMediaClickListener(listener: OnMediaClickListener?) {
        onMediaClickListener = listener
    }

    fun unregisterOnMediaClickListener() {
        onMediaClickListener = null
    }

    fun refreshSelection() {
        val layoutManager = recyclerView.layoutManager as GridLayoutManager?
        val first = layoutManager!!.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()
        if (first == -1 || last == -1) {
            return
        }
        val cursor = cursor
        for (i in first..last) {
            val holder = recyclerView.findViewHolderForAdapterPosition(first)
            if (holder is MediaViewHolder) {
                if (cursor?.moveToPosition(i) == true) {
                    setCheckStatus(Item.valueOf(cursor), holder.mediaGrid)
                }
            }
        }
    }

    private fun getImageResize(context: Context): Int {
        if (imageResize == 0) {
            val lm = recyclerView.layoutManager
            val spanCount = (lm as GridLayoutManager?)!!.spanCount
            val screenWidth = context.resources.displayMetrics.widthPixels
            val availableWidth = screenWidth - context.resources.getDimensionPixelSize(
                R.dimen.media_grid_spacing
            ) * (spanCount - 1)
            imageResize = availableWidth / spanCount
            imageResize = (imageResize * selectionSpec.thumbnailScale).toInt()
        }
        return imageResize
    }

    interface CheckStateListener {
        fun onUpdate()
    }

    interface OnMediaClickListener {
        fun onMediaClick(album: Album?, item: Item, adapterPosition: Int)
    }

    interface OnPhotoCapture {
        fun capture()
    }

    private class MediaViewHolder(itemView: View) : ViewHolder(itemView) {
        val mediaGrid: MediaGrid = itemView as MediaGrid
    }

    private class CaptureViewHolder(itemView: View) : ViewHolder(itemView) {
        val hint: TextView = itemView.findViewById<View>(R.id.hint) as TextView
    }

    companion object {
        private const val VIEW_TYPE_CAPTURE = 0x01
        private const val VIEW_TYPE_MEDIA = 0x02
    }
}