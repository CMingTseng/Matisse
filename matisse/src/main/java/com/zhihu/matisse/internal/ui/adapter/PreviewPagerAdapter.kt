package com.zhihu.matisse.internal.ui.adapter

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.ui.PreviewItemFragment

class PreviewPagerAdapter(
    manager: FragmentManager,
    private val listener: OnPrimaryItemSetListener?
) : FragmentPagerAdapter(manager) {

    private val items = mutableListOf<Item>()

    override fun getItem(position: Int): Fragment {
        return PreviewItemFragment.newInstance(items[position])
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)
        listener?.onPrimaryItemSet(position)
    }

    fun getMediaItem(position: Int): Item {
        return items[position]
    }

    fun addAll(items: List<Item>) {
        this.items.addAll(items)
    }

    interface OnPrimaryItemSetListener {
        fun onPrimaryItemSet(position: Int)
    }
}