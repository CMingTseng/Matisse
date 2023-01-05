package com.zhihu.matisse.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.zhihu.matisse.Matisse.Companion.EXTRA_RESULT_SELECTION
import com.zhihu.matisse.Matisse.Companion.EXTRA_RESULT_SELECTION_PATH
import com.zhihu.matisse.Matisse.Companion.REQUEST_CODE_CAPTURE
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter.OnPhotoCapture
import com.zhihu.matisse.internal.utils.MediaStoreCompat
import com.zhihu.matisse.internal.utils.SingleMediaScanner
/**
 * Main Activity to display albums and media content (images/videos) in each album
 * and also support media selecting operations.
 */
class CaptureDelegateActivity : AppCompatActivity(), OnPhotoCapture {
    private var spec = SelectionSpec
    private var mediaStoreCompat: MediaStoreCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!spec.hasInit) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        check(spec.capture) { "capture must set true!" }
        if (spec.captureStrategy == null) throw RuntimeException("Don't forget to set CaptureStrategy.")
        mediaStoreCompat = MediaStoreCompat(this)
        mediaStoreCompat!!.setCaptureStrategy(spec.captureStrategy)
        capture()
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(R.anim.matisse_anim_empty, R.anim.matisse_anim_empty)
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(R.anim.matisse_anim_empty, R.anim.matisse_anim_empty)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            finish()
            return
        }
        if (requestCode ==  REQUEST_CODE_CAPTURE) {
            // Just pass the data back to previous calling Activity.
            val contentUri = mediaStoreCompat!!.currentPhotoUri!!
            val path = mediaStoreCompat!!.currentPhotoPath!!
            val selected = arrayListOf(contentUri)
            val selectedPath = arrayListOf(path)
            val result = Intent()
            result.putParcelableArrayListExtra( EXTRA_RESULT_SELECTION, selected)
            result.putStringArrayListExtra(
                EXTRA_RESULT_SELECTION_PATH,
                selectedPath
            )
            setResult(RESULT_OK, result)
            SingleMediaScanner(
                this.applicationContext,
                path,
                object : SingleMediaScanner.ScanListener {
                    override fun onScanFinish() {
                        Log.i("SingleMediaScanner", "scan finish!")
                    }
                })
            finish()
        }
    }

    override fun capture() {
        if (mediaStoreCompat != null) {
            mediaStoreCompat!!.dispatchCaptureIntent(this,  REQUEST_CODE_CAPTURE)
        }
    }
}