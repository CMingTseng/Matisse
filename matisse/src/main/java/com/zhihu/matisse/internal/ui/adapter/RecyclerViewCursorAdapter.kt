/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.ui.adapter

import android.database.Cursor
import android.provider.MediaStore
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

abstract class RecyclerViewCursorAdapter<VH : ViewHolder?> internal constructor(c: Cursor?) : RecyclerView.Adapter<VH>() {
    var cursor: Cursor? = null
        private set
    private var mRowIDColumn = 0
    protected abstract fun onBindViewHolder(holder: VH, cursor: Cursor?)
    override fun onBindViewHolder(holder: VH, position: Int) {
        check(isDataValid(cursor)) { "Cannot bind view holder when cursor is in invalid state." }
        check(cursor!!.moveToPosition(position)) {
            ("Could not move cursor to position " + position
                    + " when trying to bind view holder")
        }
        onBindViewHolder(holder, cursor)
    }

    override fun getItemViewType(position: Int): Int {
        check(cursor!!.moveToPosition(position)) {
            ("Could not move cursor to position " + position
                    + " when trying to get item view type.")
        }
        return getItemViewType(position, cursor)
    }

    protected abstract fun getItemViewType(position: Int, cursor: Cursor?): Int
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
            ("Could not move cursor to position " + position
                    + " when trying to get an item id")
        }
        return cursor!!.getLong(mRowIDColumn)
    }

    fun swapCursor(newCursor: Cursor?) {
        if (newCursor === cursor) {
            return
        }
        if (newCursor != null) {
            cursor = newCursor
            mRowIDColumn = cursor!!.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            // notify the observers about the new cursor
            notifyDataSetChanged()
        } else {
            notifyItemRangeRemoved(0, itemCount)
            cursor = null
            mRowIDColumn = -1
        }
    }

    private fun isDataValid(cursor: Cursor?): Boolean {
        return cursor != null && !cursor.isClosed
    }

    init {
        setHasStableIds(true)
        swapCursor(c)
    }
}