package com.zhihu.matisse.internal.ui

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.model.AlbumMediaCollection
import com.zhihu.matisse.internal.model.AlbumMediaCollection.AlbumMediaCallbacks
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter.CheckStateListener
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter.OnMediaClickListener
import com.zhihu.matisse.internal.ui.widget.MediaGridInset
import com.zhihu.matisse.internal.utils.UIUtils

class MediaSelectionFragment : Fragment(), AlbumMediaCallbacks,
    CheckStateListener, OnMediaClickListener {
    private val albumMediaCollection = AlbumMediaCollection()
    private var recyclerView: RecyclerView? = null
    private var adapter: AlbumMediaAdapter? = null
    private var selectionProvider: SelectionProvider? = null
    private var onCheckStateListener: CheckStateListener? = null
    private var onMediaClickListener: OnMediaClickListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        selectionProvider = if (context is SelectionProvider) {
            context
        } else {
            throw IllegalStateException("Context must implement SelectionProvider.")
        }

        if (context is CheckStateListener) {
            onCheckStateListener = context
        }
        if (context is OnMediaClickListener) {
            onMediaClickListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_media_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById<View>(R.id.recyclerview) as RecyclerView
        requireActivity().lifecycle.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onCreated() {
                val album: Album? = arguments!!.getParcelable(EXTRA_ALBUM)
                val context = requireContext()
                adapter = AlbumMediaAdapter(
                    context,
                    selectionProvider!!.provideSelectedItemCollection()!!, recyclerView!!
                )
                adapter!!.registerCheckStateListener(this@MediaSelectionFragment)
                adapter!!.registerOnMediaClickListener(this@MediaSelectionFragment)
                recyclerView!!.setHasFixedSize(true)
                val spanCount: Int
                val selectionSpec = SelectionSpec
                spanCount = if (selectionSpec.gridExpectedSize > 0) {
                    UIUtils.spanCount(context, selectionSpec.gridExpectedSize)
                } else {
                    selectionSpec.spanCount
                }
                val spacing = resources.getDimensionPixelSize(R.dimen.media_grid_spacing)
                recyclerView!!.layoutManager = GridLayoutManager(context, spanCount)
                recyclerView!!.addItemDecoration(MediaGridInset(spanCount, spacing, false))
                recyclerView!!.adapter = adapter
                albumMediaCollection.onCreate(requireActivity(), this@MediaSelectionFragment)
                albumMediaCollection.load(album, selectionSpec.capture, hashCode())
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        albumMediaCollection.onDestroy()
    }

    override fun onAlbumMediaLoad(cursor: Cursor) {
        adapter!!.swapCursor(cursor)
    }

    override fun onAlbumMediaReset() {
        adapter!!.swapCursor(null)
    }

    override fun onUpdate() {
        // notify outer Activity that check state changed
        if (onCheckStateListener != null) {
            onCheckStateListener!!.onUpdate()
        }
    }

    override fun onMediaClick(album: Album?, item: Item, adapterPosition: Int) {
        onMediaClickListener?.onMediaClick(
            requireArguments().getParcelable(EXTRA_ALBUM),
            item, adapterPosition
        )
    }

    fun refreshMediaGrid() {
        adapter!!.notifyDataSetChanged()
    }

    fun refreshSelection() {
        adapter!!.refreshSelection()
    }

    interface SelectionProvider {
        fun provideSelectedItemCollection(): SelectedItemCollection?
    }

    companion object {
        const val EXTRA_ALBUM = "extra_album"

        fun newInstance(album: Album?): MediaSelectionFragment {
            val args = Bundle().apply {
                putParcelable(EXTRA_ALBUM, album)
            }
            val fragment = MediaSelectionFragment().apply {
                arguments = args
            }
            return fragment
        }
    }
}