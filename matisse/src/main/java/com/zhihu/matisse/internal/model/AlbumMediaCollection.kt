package com.zhihu.matisse.internal.model

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.loader.AlbumMediaLoader
import java.lang.ref.WeakReference

class AlbumMediaCollection : LoaderManager.LoaderCallbacks<Cursor> {
    private var context: WeakReference<Context>? = null
    private var loaderManager: LoaderManager? = null
    private var callbacks: AlbumMediaCallbacks? = null

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val context = requireNotNull(context?.get()) {
            Log.e("AlbumCollection", "Require context.")
        }
        val album: Album = requireNotNull(args?.getParcelable(ARGS_ALBUM)) {
            Log.e("AlbumCollection", "Require album arguments.")
        }
        val isCaptureEnabled = args?.getBoolean(ARGS_ENABLE_CAPTURE, false) ?: false
        return AlbumMediaLoader.newInstance(context, album, album.isAll && isCaptureEnabled)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        callbacks?.onAlbumMediaLoad(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        callbacks?.onAlbumMediaReset()
    }

    fun onCreate(context: FragmentActivity, callbacks: AlbumMediaCallbacks) {
        this.loaderManager = LoaderManager.getInstance(context)
        this.context = WeakReference(context)
        this.callbacks = callbacks
    }

    fun onDestroy() {
        loaderManager?.destroyLoader(LOADER_ID)
        callbacks = null
    }

    fun load(target: Album?, loaderId: Int) {
        load(target, false, loaderId)
    }

    fun load(target: Album?, enableCapture: Boolean, loaderId: Int) {
        val args = Bundle().apply {
            putParcelable(ARGS_ALBUM, target)
            putBoolean(ARGS_ENABLE_CAPTURE, enableCapture)
        }
        loaderManager?.initLoader(loaderId, args, this)
    }

    interface AlbumMediaCallbacks {
        fun onAlbumMediaLoad(cursor: Cursor)
        fun onAlbumMediaReset()
    }

    companion object {
        private const val LOADER_ID = 2
        private const val ARGS_ALBUM = "args_album"
        private const val ARGS_ENABLE_CAPTURE = "args_enable_capture"
    }
}