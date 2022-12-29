package com.zhihu.matisse.internal.ui

import android.database.Cursor
import android.os.Bundle
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.model.AlbumMediaCollection
import com.zhihu.matisse.internal.model.AlbumMediaCollection.AlbumMediaCallbacks
import com.zhihu.matisse.internal.ui.adapter.PreviewPagerAdapter

class AlbumPreviewActivity : BasePreviewActivity(), AlbumMediaCallbacks {
    private val collection = AlbumMediaCollection()
    private var isAlreadySetPosition = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SelectionSpec.hasInit) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        collection.onCreate(this, this)
        val album: Album? = intent.getParcelableExtra(EXTRA_ALBUM)
        collection.load(album, hashCode())
        val item: Item = intent.getParcelableExtra(EXTRA_ITEM)!!
        if (spec.countable) {
            checkView?.setCheckedNum(selectedCollection.checkedNumOf(item))
        } else {
            checkView?.setChecked(selectedCollection.isSelected(item))
        }
        updateSize(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        collection.onDestroy()
    }

    override fun onAlbumMediaLoad(cursor: Cursor) {
        val items = mutableListOf<Item>()
        while (cursor.moveToNext()) {
            items.add(Item.valueOf(cursor))
        }
        //        cursor.close();
        if (items.isEmpty()) {
            return
        }
        val adapter = pager?.adapter as PreviewPagerAdapter
        adapter.addAll(items)
        adapter.notifyDataSetChanged()
        if (!isAlreadySetPosition) {
            //onAlbumMediaLoad is called many times..
            isAlreadySetPosition = true
            val selected: Item = intent.getParcelableExtra(EXTRA_ITEM)!!
            val selectedIndex = items.indexOf(selected)
            pager?.setCurrentItem(selectedIndex, false)
            previousPos = selectedIndex
        }
    }

    override fun onAlbumMediaReset() {}

    companion object {
        const val EXTRA_ALBUM = "extra_album"
        const val EXTRA_ITEM = "extra_item"
    }
}