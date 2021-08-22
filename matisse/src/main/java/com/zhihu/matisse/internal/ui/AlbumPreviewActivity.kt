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
package com.zhihu.matisse.internal.ui

import android.database.Cursor
import android.os.Bundle
import com.zhihu.matisse.EXTRA_ALBUM
import com.zhihu.matisse.EXTRA_ITEM
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.model.AlbumMediaCollection
import com.zhihu.matisse.internal.ui.adapter.PreviewPagerAdapter
import java.util.*

class AlbumPreviewActivity : BasePreviewActivity(), AlbumMediaCollection.AlbumMediaCallbacks {
    private val mCollection = AlbumMediaCollection()
    private var mIsAlreadySetPosition = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SelectionSpec.getInstance().hasInited) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        mCollection.onCreate(this, this)
        intent?.let {
            it.getParcelableExtra<Album>(EXTRA_ALBUM)?.let { album ->
                mCollection.load(album)
            }
            intent.getParcelableExtra<Item>(EXTRA_ITEM)?.let { item ->
                if (mSpec.countable) {
                    mBinding!!.checkView.setCheckedNum(mSelectedCollection.checkedNumOf(item))
                } else {
                    mBinding!!.checkView.setChecked(mSelectedCollection.isSelected(item))
                }
                updateSize(item)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mCollection.onDestroy()
    }

    override fun onAlbumMediaLoad(cursor: Cursor) {
        val items: MutableList<Item> = ArrayList()
        while (cursor.moveToNext()) {
            items.add(Item.valueOf(cursor))
        }
        if (items.isEmpty()) {
            return
        }
        mBinding!!.pager.adapter?.let { adapter ->
            adapter as PreviewPagerAdapter
            adapter.addAll(items)
            adapter.notifyDataSetChanged()
            if (!mIsAlreadySetPosition) {
                //onAlbumMediaLoad is called many times..
                mIsAlreadySetPosition = true
                intent?.let {
                    it.getParcelableExtra<Item>(EXTRA_ITEM)?.let{selected->
                        val selectedIndex = items.indexOf(selected)
                        mBinding!!.pager.setCurrentItem(selectedIndex, false)
                        mPreviousPos = selectedIndex
                    }
                }
            }
        }
    }

    override fun onAlbumMediaReset() {}
}