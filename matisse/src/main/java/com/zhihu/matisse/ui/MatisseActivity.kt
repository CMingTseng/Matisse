package com.zhihu.matisse.ui

import android.content.Intent
import android.database.Cursor
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.model.AlbumCollection
import com.zhihu.matisse.internal.model.AlbumCollection.AlbumCallbacks
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.internal.ui.AlbumPreviewActivity
import com.zhihu.matisse.internal.ui.BasePreviewActivity
import com.zhihu.matisse.internal.ui.MediaSelectionFragment
import com.zhihu.matisse.internal.ui.MediaSelectionFragment.SelectionProvider
import com.zhihu.matisse.internal.ui.SelectedPreviewActivity
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter.*
import com.zhihu.matisse.internal.ui.adapter.AlbumsAdapter
import com.zhihu.matisse.internal.ui.widget.AlbumsSpinner
import com.zhihu.matisse.internal.ui.widget.CheckRadioView
import com.zhihu.matisse.internal.ui.widget.IncapableDialog
import com.zhihu.matisse.internal.utils.MediaStoreCompat
import com.zhihu.matisse.internal.utils.PathUtils
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils
import com.zhihu.matisse.internal.utils.SingleMediaScanner
import java.util.*

/**
 * Main Activity to display albums and media content (images/videos) in each album
 * and also support media selecting operations.
 */
class MatisseActivity : AppCompatActivity(), AlbumCallbacks,
    OnItemSelectedListener, View.OnClickListener, CheckStateListener, OnMediaClickListener,
    SelectionProvider, OnPhotoCapture {

    private val albumCollection = AlbumCollection()
    private val selectedCollection = SelectedItemCollection(this)
    private val spec = SelectionSpec
    private var mediaStoreCompat: MediaStoreCompat? = null
    private var albumsSpinner: AlbumsSpinner? = null
    private var albumsAdapter: AlbumsAdapter? = null
    private var buttonPreview: TextView? = null
    private var buttonApply: TextView? = null
    private var container: View? = null
    private var emptyView: View? = null
    private var originalLayout: LinearLayout? = null
    private var original: CheckRadioView? = null
    private var originalEnable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // programmatically set theme before super.onCreate()
        setTheme(spec.themeId)
        super.onCreate(savedInstanceState)
        if (!spec.hasInit) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        setContentView(R.layout.activity_matisse)
        if (spec.needOrientationRestriction) {
            requestedOrientation = spec.orientation
        }
        if (spec.capture) {
            mediaStoreCompat = MediaStoreCompat(this)
            if (spec.captureStrategy == null) throw RuntimeException("Don't forget to set CaptureStrategy.")
            mediaStoreCompat!!.setCaptureStrategy(spec.captureStrategy)
        }
        val toolbar = findViewById<View>(R.id.toolbar) as? Toolbar
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            val navigationIcon = toolbar.navigationIcon
            if (navigationIcon != null) {
                val ta = theme.obtainStyledAttributes(intArrayOf(R.attr.album_element_color))
                val color = ta.getColor(0, 0)
                ta.recycle()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    navigationIcon.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
                } else {
                    navigationIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                }
            }
        }
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
        buttonPreview = findViewById<View>(R.id.button_preview) as TextView
        buttonApply = findViewById<View>(R.id.button_apply) as TextView
        buttonPreview!!.setOnClickListener(this)
        buttonApply!!.setOnClickListener(this)
        container = findViewById(R.id.container)
        emptyView = findViewById(R.id.empty_view)
        original = findViewById(R.id.original)
        originalLayout = findViewById(R.id.originalLayout)
        originalLayout?.setOnClickListener(this)
        selectedCollection.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            originalEnable = savedInstanceState.getBoolean(CHECK_STATE)
        }
        updateBottomToolbar()
        albumsAdapter = AlbumsAdapter(this, null, false)
        albumsSpinner = AlbumsSpinner(this)
        albumsSpinner!!.setOnItemSelectedListener(this)
        albumsSpinner!!.setSelectedTextView(findViewById<View>(R.id.selected_album) as TextView)
        albumsSpinner!!.setPopupAnchorView(findViewById(R.id.toolbar))
        albumsSpinner!!.setAdapter(albumsAdapter)
        albumCollection.onCreate(this, this)
        albumCollection.onRestoreInstanceState(savedInstanceState)
        albumCollection.loadAlbums()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedCollection.onSaveInstanceState(outState)
        albumCollection.onSaveInstanceState(outState)
        outState.putBoolean("checkState", originalEnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        albumCollection.onDestroy()
        spec.onCheckedListener = null
        spec.onSelectedListener = null
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
        if (requestCode == REQUEST_CODE_CAPTURE) {
            // Just pass the data back to previous calling Activity.
            val contentUri = mediaStoreCompat!!.currentPhotoUri!!
            val path = mediaStoreCompat!!.currentPhotoPath!!
            val selected = arrayListOf(contentUri)
            val selectedPath = arrayListOf(path)
            val result = Intent()
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selected)
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPath)
            setResult(RESULT_OK, result)
            SingleMediaScanner(this.applicationContext, path,
                object : SingleMediaScanner.ScanListener {
                    override fun onScanFinish() {
                        Log.i(
                            "SingleMediaScanner",
                            "scan finish!"
                        )
                    }
                })
            finish()
        }
    }

    private fun updateBottomToolbar() {
        val selectedCount = selectedCollection.count()
        if (selectedCount == 0) {
            buttonPreview!!.isEnabled = false
            buttonApply!!.isEnabled = false
            buttonApply!!.text = getString(R.string.button_apply_default)
        } else if (selectedCount == 1 && spec.singleSelectionModeEnabled) {
            buttonPreview!!.isEnabled = true
            buttonApply!!.setText(R.string.button_apply_default)
            buttonApply!!.isEnabled = true
        } else {
            buttonPreview!!.isEnabled = true
            buttonApply!!.isEnabled = true
            buttonApply!!.text = getString(R.string.button_apply, selectedCount)
        }
        if (spec.originalable) {
            originalLayout!!.visibility = View.VISIBLE
            updateOriginalState()
        } else {
            originalLayout!!.visibility = View.INVISIBLE
        }
    }

    private fun updateOriginalState() {
        original!!.setChecked(originalEnable)
        if (countOverMaxSize() > 0) {
            if (originalEnable) {
                val incapableDialog = IncapableDialog.newInstance(
                    "",
                    getString(R.string.error_over_original_size, spec.originalMaxSize)
                )
                incapableDialog.show(supportFragmentManager, IncapableDialog::class.java.name)
                original!!.setChecked(false)
                originalEnable = false
            }
        }
    }

    private fun countOverMaxSize(): Int {
        var count = 0
        val selectedCount = selectedCollection.count()
        for (i in 0 until selectedCount) {
            val item = selectedCollection.asList()[i]
            if (item.isImage) {
                val size = PhotoMetadataUtils.getSizeInMB(item.size)
                if (size > spec.originalMaxSize) {
                    count++
                }
            }
        }
        return count
    }

    private val previewLauncher = registerForActivityResult(
        StartActivityForResult(),
        ActivityResultCallback { activityResult ->
            if (activityResult.resultCode != RESULT_OK) {
                return@ActivityResultCallback
            }
            val data = activityResult.data ?: return@ActivityResultCallback
            val resultBundle = data.getBundleExtra(BasePreviewActivity.EXTRA_RESULT_BUNDLE)
                ?: return@ActivityResultCallback
            val selected: List<Item> =
                resultBundle.getParcelableArrayList(SelectedItemCollection.STATE_SELECTION)
                    ?: emptyList()
            originalEnable =
                data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, false)
            val collectionType = resultBundle.getInt(
                SelectedItemCollection.STATE_COLLECTION_TYPE,
                SelectedItemCollection.COLLECTION_UNDEFINED
            )
            if (data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_APPLY, false)) {
                val result = Intent()
                val selectedUris = ArrayList<Uri>()
                val selectedPaths = ArrayList<String>()
                selected.forEach { item ->
                    selectedUris.add(item.contentUri)
                    selectedPaths.add(PathUtils.getPath(this@MatisseActivity, item.contentUri)!!)
                }
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris)
                result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths)
                result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
                setResult(RESULT_OK, result)
                finish()
            } else {
                selectedCollection.overwrite(selected, collectionType)
                val mediaSelectionFragment = supportFragmentManager.findFragmentByTag(
                    MediaSelectionFragment::class.java.simpleName
                )
                if (mediaSelectionFragment is MediaSelectionFragment) {
                    mediaSelectionFragment.refreshMediaGrid()
                }
                updateBottomToolbar()
            }
        })

    override fun onClick(v: View) {
        if (v.id == R.id.button_preview) {
            val intent = Intent(this, SelectedPreviewActivity::class.java)
            intent.putExtra(
                BasePreviewActivity.EXTRA_DEFAULT_BUNDLE,
                selectedCollection.dataWithBundle
            )
            intent.putExtra(BasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
            previewLauncher.launch(intent)
        } else if (v.id == R.id.button_apply) {
            val result = Intent()
            val selectedUris = selectedCollection.asListOfUri() as ArrayList<Uri>
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris)
            val selectedPaths = selectedCollection.asListOfString() as ArrayList<String>
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths)
            result.putExtra(EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
            setResult(RESULT_OK, result)
            finish()
        } else if (v.id == R.id.originalLayout) {
            val count = countOverMaxSize()
            if (count > 0) {
                val incapableDialog = IncapableDialog.newInstance(
                    "",
                    getString(R.string.error_over_original_count, count, spec.originalMaxSize)
                )
                incapableDialog.show(supportFragmentManager, IncapableDialog::class.java.name)
                return
            }
            originalEnable = !originalEnable
            original!!.setChecked(originalEnable)
            spec.onCheckedListener?.onCheck(originalEnable)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        albumCollection.setStateCurrentSelection(position)
        albumsAdapter!!.cursor.moveToPosition(position)
        val album = Album.valueOf(albumsAdapter!!.cursor)
        if (album.isAll && spec.capture) {
            album.addCaptureCount()
        }
        onAlbumSelected(album)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onAlbumLoad(cursor: Cursor) {
        albumsAdapter!!.swapCursor(cursor)
        // select default album.
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            cursor.moveToPosition(albumCollection.currentSelection)
            albumsSpinner!!.setSelection(this@MatisseActivity, albumCollection.currentSelection)
            val album = Album.valueOf(cursor)
            if (album.isAll && spec.capture) {
                album.addCaptureCount()
            }
            onAlbumSelected(album)
        }
    }

    override fun onAlbumReset() {
        albumsAdapter!!.swapCursor(null)
    }

    private fun onAlbumSelected(album: Album) {
        if (album.isAll && album.isEmpty) {
            container!!.visibility = View.GONE
            emptyView!!.visibility = View.VISIBLE
        } else {
            container!!.visibility = View.VISIBLE
            emptyView!!.visibility = View.GONE
            val fragment: Fragment = MediaSelectionFragment.newInstance(album)
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment, MediaSelectionFragment::class.java.simpleName)
                .commitAllowingStateLoss()
        }
    }

    override fun onUpdate() {
        // notify bottom toolbar that check state changed.
        updateBottomToolbar()
        spec.onSelectedListener?.onSelected(
            selectedCollection.asListOfUri(),
            selectedCollection.asListOfString()
        )
    }

    override fun onMediaClick(album: Album?, item: Item, adapterPosition: Int) {
        val intent = Intent(this, AlbumPreviewActivity::class.java)
        intent.putExtra(AlbumPreviewActivity.EXTRA_ALBUM, album)
        intent.putExtra(AlbumPreviewActivity.EXTRA_ITEM, item)
        intent.putExtra(
            BasePreviewActivity.EXTRA_DEFAULT_BUNDLE,
            selectedCollection.dataWithBundle
        )
        intent.putExtra(BasePreviewActivity.EXTRA_RESULT_ORIGINAL_ENABLE, originalEnable)
        previewLauncher.launch(intent)
    }

    override fun provideSelectedItemCollection(): SelectedItemCollection {
        return selectedCollection
    }

    override fun capture() {
        if (mediaStoreCompat != null) {
            mediaStoreCompat!!.dispatchCaptureIntent(this, REQUEST_CODE_CAPTURE)
        }
    }

    companion object {
        const val EXTRA_RESULT_SELECTION = "extra_result_selection"
        const val EXTRA_RESULT_SELECTION_PATH = "extra_result_selection_path"
        const val EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable"
        const val CHECK_STATE = "checkState"
        const val REQUEST_CODE_CAPTURE = 24
    }
}