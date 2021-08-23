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

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.zhihu.matisse.EXTRA_ALBUM
import com.zhihu.matisse.R
import com.zhihu.matisse.databinding.FragmentMediaSelectionBinding
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.model.AlbumMediaCollection
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter
import com.zhihu.matisse.internal.ui.widget.MediaGridInset
import com.zhihu.matisse.internal.utils.UIUtils.spanCount


class MediaSelectionFragment : Fragment(), AlbumMediaCollection.AlbumMediaCallbacks, AlbumMediaAdapter.CheckStateListener, AlbumMediaAdapter.OnMediaClickListener {
    interface SelectionProvider {
        fun provideSelectedItemCollection(): SelectedItemCollection?
    }

    companion object {
        fun newInstance(album: Album): MediaSelectionFragment {
            val fragment = MediaSelectionFragment()
            val args = Bundle()
            args.putParcelable(EXTRA_ALBUM, album)
            fragment.arguments = args
            return fragment
        }
    }

    private var mBinding: FragmentMediaSelectionBinding? = null
    private val mAlbumMediaCollection = AlbumMediaCollection()
    private lateinit var mAdapter: AlbumMediaAdapter
    private lateinit var mSelectionProvider: SelectionProvider
    private var mCheckStateListener: AlbumMediaAdapter.CheckStateListener? = null
    private var mOnMediaClickListener: AlbumMediaAdapter.OnMediaClickListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mSelectionProvider = if (context is SelectionProvider) {
            context
        } else {
            throw IllegalStateException("Context must implement SelectionProvider.")
        }
        if (context is AlbumMediaAdapter.CheckStateListener) {
            mCheckStateListener = context
        }
        if (context is AlbumMediaAdapter.OnMediaClickListener) {
            mOnMediaClickListener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentMediaSelectionBinding.inflate(inflater)
        return mBinding!!.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireArguments()?.let { arguments ->
            val context = requireContext()
            arguments.getParcelable<Album>(EXTRA_ALBUM)?.let { album ->
                mAdapter = AlbumMediaAdapter(context, mSelectionProvider.provideSelectedItemCollection(), mBinding!!.recyclerview)
                mAdapter.registerCheckStateListener(this)
                mAdapter.registerOnMediaClickListener(this)
                mBinding!!.recyclerview.setHasFixedSize(true)
                val selectionSpec = SelectionSpec.getInstance()
                if (selectionSpec.isUseHeaderHint) {
                    mBinding!!.selectAlbumTitle.visibility = View.VISIBLE
                    var type = ""
                    "".also {
                        selectionSpec.mimeTypeSet.forEach { key ->
                            type = type + key.toString().replace("image/", ".") + " "
                        }
                        mBinding!!.selectAlbumHint.text = String.format(getString(R.string.media_header_hint), selectionSpec.maxSelectable, selectionSpec.originalMaxSize, type)
                    }
                }

                val spancount = if (selectionSpec.gridExpectedSize > 0) {
                    spanCount(context, selectionSpec.gridExpectedSize)
                } else {
                    selectionSpec.spanCount
                }
                mBinding!!.recyclerview.layoutManager?.let { layoutManager ->
                    (layoutManager as GridLayoutManager)?.let { glm ->
                        glm.spanCount = spancount
//                        glm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
//                            override fun getSpanSize(position: Int): Int {
//                                Log.e("SpanSizeLookup","Show GridLayoutManager.SpanSizeLookup position : $position")
//                                val viewtype: Int = mAdapter.getItemViewType(position)
//                                Log.e("SpanSizeLookup","Show GridLayoutManager.SpanSizeLookup viewtype : $viewtype")
//                                var spansize = spancount
//                                Log.e("SpanSizeLookup","Show GridLayoutManager.SpanSizeLookup spansize : $spansize")
//                                return spansize
//                            }
//                        }
                        mBinding!!.recyclerview.layoutManager = glm
                        val spacing = resources.getDimensionPixelSize(R.dimen.media_grid_spacing)
                        mBinding!!.recyclerview.addItemDecoration(MediaGridInset(spancount, spacing, false))
                        mBinding!!.recyclerview.adapter = mAdapter
                        mAlbumMediaCollection.onCreate(requireActivity(), this)
                        mAlbumMediaCollection.load(album, selectionSpec.capture)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mAlbumMediaCollection.onDestroy()
    }

    fun refreshMediaGrid() {
        mAdapter!!.notifyDataSetChanged()
    }

    fun refreshSelection() {
        mAdapter!!.refreshSelection()
    }

    override fun onAlbumMediaLoad(cursor: Cursor) {
        mAdapter!!.swapCursor(cursor)
    }

    override fun onAlbumMediaReset() {
        mAdapter!!.swapCursor(null)
    }

    override fun onUpdate() {
        // notify outer Activity that check state changed
        if (mCheckStateListener != null) {
            mCheckStateListener!!.onUpdate()
        }
    }

    override fun onMediaClick(album: Album?, item: Item, adapterPosition: Int) {
        mOnMediaClickListener?.let { listener ->
            requireArguments()?.let { arguments ->
                arguments.getParcelable<Album>(EXTRA_ALBUM)?.let { album ->
                    listener.onMediaClick(album, item, adapterPosition)
                }
            }
        }
    }
}