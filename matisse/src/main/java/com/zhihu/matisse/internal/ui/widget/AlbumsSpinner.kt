package com.zhihu.matisse.internal.ui.widget

import android.content.Context
import android.graphics.PorterDuff
import android.view.View
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.CursorAdapter
import android.widget.TextView
import androidx.appcompat.widget.ListPopupWindow
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.Album

class AlbumsSpinner(context: Context) {

    private var adapter: CursorAdapter? = null
    private var selected: TextView? = null
    private val listPopupWindow: ListPopupWindow =
        ListPopupWindow(context, null, R.attr.listPopupWindowStyle)
    private var mOnItemSelectedListener: OnItemSelectedListener? = null

    init {
        val density = context.resources.displayMetrics.density
        listPopupWindow.isModal = true
        listPopupWindow.setContentWidth((216 * density).toInt())
        listPopupWindow.horizontalOffset = (16 * density).toInt()
        listPopupWindow.verticalOffset = (-48 * density).toInt()
        listPopupWindow.setOnItemClickListener { parent, view, position, id ->
            onItemSelected(parent.context, position)
            mOnItemSelectedListener?.onItemSelected(parent, view, position, id)
        }
    }

    fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        mOnItemSelectedListener = listener
    }

    fun setSelection(context: Context, position: Int) {
        listPopupWindow.setSelection(position)
        onItemSelected(context, position)
    }

    private fun onItemSelected(context: Context, position: Int) {
        listPopupWindow.dismiss()
        val cursor = adapter!!.cursor
        cursor.moveToPosition(position)
        val album = Album.valueOf(cursor)
        val displayName = album.getDisplayName(context)
        if (selected!!.visibility == View.VISIBLE) {
            selected!!.text = displayName
        } else {
            selected!!.alpha = 0.0f
            selected!!.visibility = View.VISIBLE
            selected!!.text = displayName
            val duration =
                context.resources.getInteger(android.R.integer.config_longAnimTime).toLong()
            selected!!.animate()
                .alpha(1.0f)
                .setDuration(duration)
                .start()
        }
    }

    fun setAdapter(adapter: CursorAdapter?) {
        listPopupWindow.setAdapter(adapter)
        this.adapter = adapter
    }

    fun setSelectedTextView(textView: TextView?) {
        selected = textView
        // tint dropdown arrow icon
        val drawables = selected!!.compoundDrawables
        val right = drawables[2]
        val ta =
            selected!!.context.theme.obtainStyledAttributes(intArrayOf(R.attr.album_element_color))
        val color = ta.getColor(0, 0)
        ta.recycle()
        right.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        selected!!.visibility = View.GONE
        selected!!.setOnClickListener { v ->
            val itemHeight = v.resources.getDimensionPixelSize(R.dimen.album_item_height)
            listPopupWindow.height =
                if (adapter!!.count > MAX_SHOWN_COUNT) itemHeight * MAX_SHOWN_COUNT
                else itemHeight * adapter!!.count
            listPopupWindow.show()
        }
        selected!!.setOnTouchListener(listPopupWindow.createDragToOpenListener(selected))
    }

    fun setPopupAnchorView(view: View?) {
        listPopupWindow.anchorView = view
    }

    companion object {
        private const val MAX_SHOWN_COUNT = 6
    }
}