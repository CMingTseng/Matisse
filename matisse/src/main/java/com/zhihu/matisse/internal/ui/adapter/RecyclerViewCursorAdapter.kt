package com.zhihu.matisse.internal.ui.adapter

import android.database.Cursor
import android.provider.MediaStore
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

abstract class RecyclerViewCursorAdapter<VH : ViewHolder?> internal constructor(c: Cursor?) :
    RecyclerView.Adapter<VH>() {

    private var rowIDColumn = 0

    var cursor: Cursor? = null
        private set

    init {
        setHasStableIds(true)
        swapCursor(c)
    }

    protected abstract fun onBindViewHolder(holder: VH, cursor: Cursor)
    protected abstract fun getItemViewType(position: Int, cursor: Cursor): Int

    override fun onBindViewHolder(holder: VH, position: Int) {
        check(isDataValid(cursor)) { "Cannot bind view holder when cursor is in invalid state." }
        check(cursor!!.moveToPosition(position)) {
            "Could not move cursor to position $position when trying to bind view holder"
        }
        onBindViewHolder(holder, cursor!!)
    }

    override fun getItemViewType(position: Int): Int {
        check(isDataValid(cursor)) { "Cannot get item view type when cursor is in invalid state." }
        check(cursor!!.moveToPosition(position)) {
            "Could not move cursor to position $position when trying to get item view type."
        }
        return getItemViewType(position, cursor!!)
    }

    override fun getItemCount(): Int {
        return if (isDataValid(cursor)) {
            cursor!!.count
        } else {
            0
        }
    }

    override fun getItemId(position: Int): Long {
        check(isDataValid(cursor)) { "Cannot lookup item id when cursor is in invalid state." }
        check(cursor!!.moveToPosition(position)) {
            ("Could not move cursor to position $position when trying to get an item id")
        }
        return cursor!!.getLong(rowIDColumn)
    }

    fun swapCursor(newCursor: Cursor?) {
        if (newCursor === cursor) {
            return
        }
        if (newCursor != null) {
            cursor = newCursor
            rowIDColumn = cursor!!.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            // notify the observers about the new cursor
            notifyDataSetChanged()
        } else {
            notifyItemRangeRemoved(0, itemCount)
            cursor = null
            rowIDColumn = -1
        }
    }

    private fun isDataValid(cursor: Cursor?): Boolean {
        return cursor != null && !cursor.isClosed
    }
}