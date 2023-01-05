package com.zhihu.matisse.internal.ui

import android.os.Bundle
import com.zhihu.matisse.Matisse.Companion.EXTRA_DEFAULT_BUNDLE
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.model.SelectedItemCollection

class SelectedPreviewActivity : BasePreviewActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SelectionSpec.hasInit) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        val bundle = intent.getBundleExtra(EXTRA_DEFAULT_BUNDLE)
        val selected: List<Item> =
            bundle?.getParcelableArrayList(SelectedItemCollection.STATE_SELECTION)!!
        adapter!!.addAll(selected)
        adapter!!.notifyDataSetChanged()
        if (spec.countable) {
            checkView!!.setCheckedNum(1)
        } else {
            checkView!!.setChecked(true)
        }
        previousPos = 0
        updateSize(selected[0])
    }
}