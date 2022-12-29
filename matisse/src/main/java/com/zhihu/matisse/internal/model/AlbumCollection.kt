package com.zhihu.matisse.internal.model

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.zhihu.matisse.internal.loader.AlbumLoader
import java.lang.ref.WeakReference

class AlbumCollection : LoaderManager.LoaderCallbacks<Cursor> {
    private var context: WeakReference<Context>? = null
    private var loaderManager: LoaderManager? = null
    private var callbacks: AlbumCallbacks? = null
    private var loadFinished = false

    var currentSelection = 0
        private set

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val context = requireNotNull(context?.get()) {
            Log.e("AlbumCollection", "Require context.")
        }
        loadFinished = false
        return AlbumLoader.newInstance(context)
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        if (!loadFinished) {
            loadFinished = true
            callbacks?.onAlbumLoad(data)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        callbacks?.onAlbumReset()
    }

    fun onCreate(activity: FragmentActivity, callbacks: AlbumCallbacks?) {
        context = WeakReference(activity)
        loaderManager = activity.supportLoaderManager
        this.callbacks = callbacks
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            return
        }
        currentSelection = savedInstanceState.getInt(STATE_CURRENT_SELECTION)
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_CURRENT_SELECTION, currentSelection)
    }

    fun onDestroy() {
        if (loaderManager != null) {
            loaderManager!!.destroyLoader(LOADER_ID)
        }
        callbacks = null
    }

    fun loadAlbums() {
        loaderManager!!.initLoader(LOADER_ID, null, this)
    }

    fun setStateCurrentSelection(currentSelection: Int) {
        this.currentSelection = currentSelection
    }

    interface AlbumCallbacks {
        fun onAlbumLoad(cursor: Cursor)
        fun onAlbumReset()
    }

    companion object {
        private const val LOADER_ID = 1
        private const val STATE_CURRENT_SELECTION = "state_current_selection"
    }
}