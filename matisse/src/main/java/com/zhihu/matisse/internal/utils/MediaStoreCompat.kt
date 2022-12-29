package com.zhihu.matisse.internal.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.os.EnvironmentCompat
import androidx.fragment.app.Fragment
import com.zhihu.matisse.internal.entity.CaptureStrategy
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

class MediaStoreCompat(activity: AppCompatActivity, fragment: Fragment? = null) {
    private val context: WeakReference<AppCompatActivity> = WeakReference(activity)
    private val fragment: WeakReference<Fragment>? = fragment?.run { WeakReference(fragment) }
    private var captureStrategy: CaptureStrategy? = null

    var currentPhotoUri: Uri? = null
        private set

    var currentPhotoPath: String? = null
        private set

    fun setCaptureStrategy(strategy: CaptureStrategy?) {
        captureStrategy = strategy
    }

    fun dispatchCaptureIntent(context: AppCompatActivity, requestCode: Int) {
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (captureIntent.resolveActivity(context.packageManager) != null) {
            try {
                val photoFile = createImageFile()
                if (photoFile != null) {
                    currentPhotoPath = photoFile.absolutePath
                    currentPhotoUri = FileProvider.getUriForFile(
                        this.context.get()!!,
                        captureStrategy!!.authority, photoFile
                    )
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
                    captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    if (this.fragment != null) {
                        this.fragment.get()!!.startActivityForResult(captureIntent, requestCode)
                    } else {
                        this.context.get()!!.startActivityForResult(captureIntent, requestCode)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = String.format("JPEG_%s.jpg", timeStamp)
        var storageDir: File?
        if (captureStrategy!!.isPublic) {
            storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            if (!storageDir.exists()) storageDir.mkdirs()
        } else {
            storageDir = context.get()!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        }
        if (captureStrategy!!.directory != null) {
            storageDir = File(storageDir, captureStrategy!!.directory!!)
            if (!storageDir.exists())
                storageDir.mkdirs()
        }

        // Avoid joining path components manually
        val tempFile = File(storageDir, imageFileName)

        // Handle the situation that user's external storage is not ready
        return if (Environment.MEDIA_MOUNTED != EnvironmentCompat.getStorageState(tempFile)) {
            null
        } else {
            tempFile
        }
    }

    companion object {
        /**
         * Checks whether the device has a camera feature or not.
         *
         * @param context a context to check for camera feature.
         * @return true if the device has a camera feature. false otherwise.
         */
        fun hasCameraFeature(context: Context): Boolean {
            val pm = context.applicationContext.packageManager
            return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
        }
    }
}