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
package com.zhihu.matisse.ui

import android.content.Intent
import android.database.Cursor
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.zhihu.matisse.*
import com.zhihu.matisse.databinding.ActivityMatisseBinding
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.model.AlbumCollection
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.internal.ui.AlbumPreviewActivity
import com.zhihu.matisse.internal.ui.MediaSelectionFragment
import com.zhihu.matisse.internal.ui.SelectedPreviewActivity
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter
import com.zhihu.matisse.internal.ui.adapter.AlbumsAdapter
import com.zhihu.matisse.internal.ui.widget.AlbumsSpinner
import com.zhihu.matisse.internal.ui.widget.IncapableDialog
import com.zhihu.matisse.internal.utils.MediaStoreCompat
import com.zhihu.matisse.internal.utils.PathUtils.Companion.getPath
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils.Companion.getSizeInMB
import com.zhihu.matisse.internal.utils.SingleMediaScanner
import java.util.*

/**
 * Main Activity to display albums and media content (images/videos) in each album
 * and also support media selecting operations.
 */
class MatisseActivity : AppCompatActivity(), AlbumCollection.AlbumCallbacks, OnItemSelectedListener, MediaSelectionFragment.SelectionProvider, View.OnClickListener, AlbumMediaAdapter.CheckStateListener, AlbumMediaAdapter.OnMediaClickListener, AlbumMediaAdapter.OnPhotoCapture {
    private var _binding: ActivityMatisseBinding? = null
    private val binding get() = _binding!!
    //  private lateinit var binding: ActivityMatisseBinding
    private val mAlbumCollection = AlbumCollection()
    private var mMediaStoreCompat: MediaStoreCompat? = null
    private val mSelectedCollection = SelectedItemCollection(this)
    private lateinit var mSpec: SelectionSpec
    private lateinit var albumsspinner: AlbumsSpinner
    private var mAlbumsAdapter: AlbumsAdapter? = null
    private var mOriginalEnable = false
    override fun onCreate(savedInstanceState: Bundle?) {
        // programmatically set theme before super.onCreate()
        SelectionSpec.getInstance()?.let {
            mSpec = it
            setTheme(mSpec.themeId)
            super.onCreate(savedInstanceState)
            if (!mSpec.hasInited) {
                setResult(RESULT_CANCELED)
                finish()
                return
            }
            _binding = ActivityMatisseBinding.inflate(layoutInflater)
//             binding = ActivityMatisseBinding.inflate(layoutInflater)
            val view = binding.root
            setContentView(view)
            if (mSpec.needOrientationRestriction()) {
                requestedOrientation = mSpec.orientation
            }
            if (mSpec.capture) {
                mMediaStoreCompat = MediaStoreCompat(this)
                if (mSpec.captureStrategy == null) throw RuntimeException("Don't forget to set CaptureStrategy.")
                mMediaStoreCompat!!.setCaptureStrategy(it.captureStrategy)
            }
            setSupportActionBar(binding.toolbar)
            val actionBar = supportActionBar
            actionBar!!.setDisplayShowTitleEnabled(false)
            actionBar.setDisplayHomeAsUpEnabled(true)
            val navigationIcon = binding.toolbar.navigationIcon
            val ta = theme.obtainStyledAttributes(intArrayOf(R.attr.album_element_color))
            val color = ta.getColor(0, 0)
            ta.recycle()
            navigationIcon!!.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            binding.buttonPreview.setOnClickListener(this)
            binding.buttonApply.setOnClickListener(this)
            binding.originalLayout.setOnClickListener(this)
            mSelectedCollection.onCreate(savedInstanceState)
            if (savedInstanceState != null) {
                mOriginalEnable = savedInstanceState.getBoolean(CHECK_STATE)
            }
            updateBottomToolbar()
            mAlbumsAdapter = AlbumsAdapter(this, null, false)
            albumsspinner = AlbumsSpinner(this)
            albumsspinner.setOnItemSelectedListener(this)
            albumsspinner.setSelectedTextView(binding.selectedAlbum)
            albumsspinner.setPopupAnchorView(binding.toolbar)
            albumsspinner.setAdapter(mAlbumsAdapter)
            mAlbumCollection.onCreate(this, this)
            mAlbumCollection.onRestoreInstanceState(savedInstanceState)
            mAlbumCollection.loadAlbums()
        } ?: run {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mSelectedCollection.onSaveInstanceState(outState)
        mAlbumCollection.onSaveInstanceState(outState)
        outState.putBoolean(CHECK_STATE, mOriginalEnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        mAlbumCollection.onDestroy()
        mSpec.onCheckedListener = null
        mSpec.onSelectedListener = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        if (requestCode == REQUEST_CODE_PREVIEW) {
            data?.let { intent ->
                intent.getBundleExtra(EXTRA_RESULT_BUNDLE)?.let { resultBundle ->
                    val selected: ArrayList<Item>? = resultBundle.getParcelableArrayList<Item>(SelectedItemCollection.STATE_SELECTION)
                    mOriginalEnable = data.getBooleanExtra(EXTRA_RESULT_ORIGINAL_ENABLE, false)
                    val collectionType = resultBundle.getInt(SelectedItemCollection.STATE_COLLECTION_TYPE, SelectedItemCollection.COLLECTION_UNDEFINED)
                    selected?.let { items ->
                        if (data.getBooleanExtra(EXTRA_RESULT_APPLY, false)) {
                            val result = Intent()
                            val selectedUris = ArrayList<Uri>()
                            val selectedPaths = ArrayList<String?>()
                            for (item in items) {
                                selectedUris.add(item.contentUri)
                                selectedPaths.add(getPath(this, item.contentUri))
                            }
                            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris)
                            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths)
                            result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable)
                            setResult(RESULT_OK, result)
                            finish()
                        } else {
                            mSelectedCollection.overwrite(items, collectionType)
                            val mediaSelectionFragment = supportFragmentManager.findFragmentByTag(MediaSelectionFragment::class.java.simpleName)
                            if (mediaSelectionFragment is MediaSelectionFragment) {
                                mediaSelectionFragment.refreshMediaGrid()
                            }
                            updateBottomToolbar()
                        }
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_CAPTURE) {
            // Just pass the data back to previous calling Activity.
            val contentUri = mMediaStoreCompat!!.currentPhotoUri
            val path = mMediaStoreCompat!!.currentPhotoPath
            val selected = ArrayList<Uri>()
            selected.add(contentUri)
            val selectedPath = ArrayList<String>()
            selectedPath.add(path)
            val result = Intent()
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selected)
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPath)
            setResult(RESULT_OK, result)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) this@MatisseActivity.revokeUriPermission(contentUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            SingleMediaScanner(this.applicationContext, path, object : SingleMediaScanner.ScanListener {
                override fun onScanFinish() {
                    Log.i("SingleMediaScanner", "scan finish!")
                }
            })
            finish()
        }
    }

    private fun updateBottomToolbar() {
        val selectedCount = mSelectedCollection.count()
        if (mSpec.isUseHeaderHint){
            binding.selectAlbumCountHint.text = String.format(getString(R.string.media_header_count_hint), selectedCount, mSpec.maxSelectable)
        }
        if (selectedCount == 0) {
            binding.buttonPreview!!.isEnabled = false
            binding.buttonApply.isEnabled = false
            binding.buttonApply!!.text = getString(R.string.button_apply_default)
        } else if (selectedCount == 1 && mSpec!!.singleSelectionModeEnabled()) {
            binding.buttonPreview!!.isEnabled = true
            binding.buttonApply!!.setText(R.string.button_apply_default)
            binding.buttonApply!!.isEnabled = true
        } else {
            binding.buttonPreview!!.isEnabled = true
            binding.buttonApply!!.isEnabled = true
            binding.buttonApply!!.text = getString(R.string.button_apply, selectedCount)
        }
        if (mSpec!!.originalable) {
            binding.originalLayout.visibility = View.VISIBLE
            updateOriginalState()
        } else {
            binding.originalLayout!!.visibility = View.INVISIBLE
        }
    }

    private fun updateOriginalState() {
        binding.original.setChecked(mOriginalEnable)
        if (countOverMaxSize() > 0) {
            if (mOriginalEnable) {
                val incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_size, mSpec!!.originalMaxSize))
                incapableDialog.show(supportFragmentManager,
                        IncapableDialog::class.java.name)
                binding.original.setChecked(false)
                mOriginalEnable = false
            }
        }
    }

    private fun countOverMaxSize(): Int {
        var count = 0
        val selectedCount = mSelectedCollection.count()
        for (i in 0 until selectedCount) {
            val item = mSelectedCollection.asList()[i]
            if (item.isImage) {
                val size = getSizeInMB(item.size)
                if (size > mSpec!!.originalMaxSize) {
                    count++
                }
            }
        }
        return count
    }

    override fun onClick(v: View) {
        if (v.id == R.id.button_preview) {
            val intent = Intent(this, SelectedPreviewActivity::class.java)
            intent.putExtra(EXTRA_DEFAULT_BUNDLE, mSelectedCollection.dataWithBundle)
            intent.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable)
            startActivityForResult(intent, REQUEST_CODE_PREVIEW)
        } else if (v.id == R.id.button_apply) {
            val result = Intent()
            val selectedUris = mSelectedCollection.asListOfUri() as ArrayList<Uri>
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris)
            val selectedPaths = mSelectedCollection.asListOfString() as ArrayList<String>
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths)
            result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable)
            setResult(RESULT_OK, result)
            finish()
        } else if (v.id == R.id.originalLayout) {
            val count = countOverMaxSize()
            if (count > 0) {
                val incapableDialog = IncapableDialog.newInstance("",
                        getString(R.string.error_over_original_count, count, mSpec!!.originalMaxSize))
                incapableDialog.show(supportFragmentManager,
                        IncapableDialog::class.java.name)
                return
            }
            mOriginalEnable = !mOriginalEnable
            binding.original.setChecked(mOriginalEnable)
            if (mSpec.onCheckedListener != null) {
                mSpec.onCheckedListener.onCheck(mOriginalEnable)
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        mAlbumCollection.setStateCurrentSelection(position)
        mAlbumsAdapter!!.cursor.moveToPosition(position)
        val album = Album.valueOf(mAlbumsAdapter!!.cursor)
        if (album.isAll && SelectionSpec.getInstance().capture) {
            album.addCaptureCount()
        }
        onAlbumSelected(album)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
    override fun onAlbumLoad(cursor: Cursor) {
        mAlbumsAdapter!!.swapCursor(cursor)
        // select default album.
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            cursor.moveToPosition(mAlbumCollection.currentSelection)
            albumsspinner.setSelection(this@MatisseActivity,
                    mAlbumCollection.currentSelection)
            val album = Album.valueOf(cursor)
            if (album.isAll && SelectionSpec.getInstance().capture) {
                album.addCaptureCount()
            }
            onAlbumSelected(album)
        }
    }

    override fun onAlbumReset() {
        mAlbumsAdapter!!.swapCursor(null)
    }

    private fun onAlbumSelected(album: Album) {
        if (album.isAll && album.isEmpty) {
            binding.container.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.container.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
            val fragment: Fragment = MediaSelectionFragment.newInstance(album)
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, fragment, MediaSelectionFragment::class.java.simpleName)
                    .commitAllowingStateLoss()
        }
    }

    override fun provideSelectedItemCollection(): SelectedItemCollection {
        return mSelectedCollection
    }

    override fun onUpdate() {
        // notify bottom toolbar that check state changed.
        updateBottomToolbar()
        if (mSpec.onSelectedListener != null) {
            mSpec.onSelectedListener.onSelected(
                    mSelectedCollection.asListOfUri(), mSelectedCollection.asListOfString())
        }
    }

    override fun onMediaClick(album: Album?, item: Item?, adapterPosition: Int) {
        val intent = Intent(this, AlbumPreviewActivity::class.java)
        intent.putExtra(EXTRA_ALBUM, album)
        intent.putExtra(EXTRA_ITEM, item)
        intent.putExtra(EXTRA_DEFAULT_BUNDLE, mSelectedCollection.dataWithBundle)
        intent.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, mOriginalEnable)
        startActivityForResult(intent, REQUEST_CODE_PREVIEW)
    }

    override fun capture() {
        if (mMediaStoreCompat != null) {
            mMediaStoreCompat!!.dispatchCaptureIntent(this, REQUEST_CODE_CAPTURE)
        }
    }
}