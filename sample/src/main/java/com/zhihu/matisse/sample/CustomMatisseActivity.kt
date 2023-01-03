package com.zhihu.matisse.sample

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import com.zhihu.matisse.listener.SelectionDelegate
import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.widget.TextView
import com.zhihu.matisse.Matisse
import com.tbruyelle.rxpermissions2.RxPermissions
import android.widget.Toast
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.EditText
import com.zhihu.matisse.engine.ImageEngine
import com.zhihu.matisse.engine.impl.GlideEngine
import com.zhihu.matisse.engine.impl.PicassoEngine
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.zhihu.matisse.internal.entity.CaptureStrategy
import android.content.pm.ActivityInfo
import com.zhihu.matisse.internal.model.SelectedItemCollection.MaxItemReach
import android.provider.MediaStore
import android.database.CursorIndexOutOfBoundsException
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.filter.Filter
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.sample.databinding.ActivityCustomMatisseBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.IOException
import java.lang.IllegalStateException

/**
 * Custom Matisse
 */
class CustomMatisseActivity : AppCompatActivity(), View.OnClickListener, SelectionDelegate {
    private lateinit var binding: ActivityCustomMatisseBinding
    private lateinit var vm: CustomMatisseViewModel
    private var mSelectedUris: List<Uri>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomMatisseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this).get(CustomMatisseViewModel::class.java)

        binding.btnGo.setOnClickListener(this)
        vm.results.observe(this ) {its->
            binding.tvResult.text = ""
            mSelectedUris=its
            for (uri in its) {
                val or = getExifOrientation(this@CustomMatisseActivity, uri)
                binding.tvResult.append(uri.toString())
                binding.tvResult.append("\n")
            }
        }

        lifecycleScope.launchWhenStarted {
            binding.tvPathResult.text = ""
            vm.resultFlow.collect {pathResult->
                // Use absolute path result
                for (path in pathResult) {
//                    GlobalScope.launch(Dispatchers.IO) {
//                        VideoFrameExtractor
//                    }
                    binding.tvPathResult.append(path)
                    binding.tvPathResult.append("\n")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            data?.let {
                vm.process(it)
            }
        }
    }

    override fun onClick(v: View) {
        val rxPermissions = RxPermissions(this)
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .subscribe({ aBoolean: Boolean ->
                if (aBoolean) {
                    startAction()
                } else {
                    Toast.makeText(
                        this@CustomMatisseActivity,
                        R.string.permission_request_denied,
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    private fun startAction() {
        val imageCheckBox = findViewById<View>(R.id.cb_choice_image) as CheckBox
        val videoCheckBox = findViewById<View>(R.id.cb_choice_video) as CheckBox
        val zhihuRadioButton = findViewById<View>(R.id.rb_theme_zhihu) as RadioButton
        val draculaRadioButton = findViewById<View>(R.id.rb_theme_dracula) as RadioButton
        val customThemeButton = findViewById<View>(R.id.rb_theme_custom) as RadioButton
        val glideRadioButton = findViewById<View>(R.id.rb_glide) as RadioButton
        val picassoRadioButton = findViewById<View>(R.id.rb_picasso) as RadioButton
        val uilRadioButton = findViewById<View>(R.id.rb_uil) as RadioButton
        val selectCountEditor = findViewById<View>(R.id.et_selectable_count) as EditText
        val selectVideoCountEditor = findViewById<View>(R.id.et_video_selectable_count) as EditText
        val countableCheckBox = findViewById<View>(R.id.cb_countable) as CheckBox
        val captureCheckBox = findViewById<View>(R.id.cb_capture) as CheckBox
        val mimeTypes: Set<MimeType>
        mimeTypes = if (imageCheckBox.isChecked && videoCheckBox.isChecked) {
            MimeType.ofAll()
        } else if (imageCheckBox.isChecked) {
            MimeType.ofImage()
        } else {
            MimeType.ofVideo()
        }
        var imageEngine: ImageEngine? = null
        if (glideRadioButton.isChecked) {
            imageEngine = GlideEngine()
        } else if (picassoRadioButton.isChecked) {
            imageEngine = PicassoEngine()
        } else if (uilRadioButton.isChecked) {
            ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this))
            imageEngine = UILEngine()
        }
        val maxCount = selectCountEditor.text.toString()
        val maxVideoCount = selectVideoCountEditor.text.toString()
        val maxSelectable = maxCount.toInt()
        val maxVideoSeletable = maxVideoCount.toInt()
        var theme = R.style.Matisse_Dracula
        if (zhihuRadioButton.isChecked) {
            theme = R.style.Matisse_Zhihu
        } else if (draculaRadioButton.isChecked) {
            theme = R.style.Matisse_Dracula
        } else if (customThemeButton.isChecked) {
            theme = R.style.CustomTheme
        } else {
            // custom theme
        }
        val countable = countableCheckBox.isChecked
        val capture = captureCheckBox.isChecked
        Matisse.from(this)
            .choose(mimeTypes, false)
            .showSingleMediaType(true)
            .capture(capture)
            .captureStrategy(
                CaptureStrategy(true, "com.zhihu.matisse.sample.fileprovider")
            )
            .countable(countable)
            .maxSelectable(maxSelectable)
            .enablePreview(true)!!//                .maxSelectablePerMediaType(maxSelectable, maxVideoSeletable)
            .addFilter(GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
            .gridExpectedSize(
                resources.getDimensionPixelSize(R.dimen.grid_expected_size)
            )
            .originalEnable(true)
            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            .thumbnailScale(0.85f)
            .imageEngine(imageEngine)
            .theme(theme)
            .delegate(this)!!
            .allowsMultipleSelection(false)!! //.maxVideoLength(120)
            .hasFeatureEnabled(false)!!
            .dontShowVideoAlert(true)!!
            .forResult(REQUEST_CODE_CHOOSE, mSelectedUris)
    }

    override fun getCause(reach: MaxItemReach?): String? {
        return when (reach) {
            MaxItemReach.MIX_REACH -> "Mix cause"
            MaxItemReach.IMAGE_REACH -> "Image cause"
            MaxItemReach.VIDEO_REACH -> "Video cause"
            else -> "My cause"
        }
    }

    override fun onTapItem(item: Item?, isDontShow: Boolean?) {
        if (item != null) {
            Log.d("ACTIVITY_MATISSE", String.format("DURATION: %d seconds", item.duration / 1000))
        }
    }

    companion object {
        private const val REQUEST_CODE_CHOOSE = 23
        private val TAG = CustomMatisseActivity::class.java.simpleName
        const val LOCAL_CONTENT_SCHEME = "content"
        fun getSchemeOrNull(uri: Uri?): String? {
            return uri?.scheme
        }

        fun isLocalContentUri(uri: Uri?): Boolean {
            val scheme = getSchemeOrNull(uri)
            return LOCAL_CONTENT_SCHEME == scheme
        }

        fun exifOrientationFromDegrees(degrees: Int): Int {
            return when (degrees) {
                90 -> ExifInterface.ORIENTATION_ROTATE_90
                180 -> ExifInterface.ORIENTATION_ROTATE_180
                270 -> ExifInterface.ORIENTATION_ROTATE_270
                else -> 0
            }
        }

        fun getExifOrientation(context: Context, uri: Uri): Int {
            var orientation = ExifInterface.ORIENTATION_UNDEFINED
            if (uri.scheme != null && isLocalContentUri(uri)) {
                val projection = arrayOf(MediaStore.Images.ImageColumns.ORIENTATION)
                val cursor = context.contentResolver.query(uri, projection, null, null, null)
                if (null != cursor) {
                    try {
                        if (cursor.moveToFirst()) {
                            val degrees = cursor.getInt(0)
                            return exifOrientationFromDegrees(degrees)
                        }
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                    } catch (e: CursorIndexOutOfBoundsException) {
                        e.printStackTrace()
                    } finally {
                        cursor.close()
                    }
                }
            } else {
                try {
                    val originalExif = ExifInterface(uri.path!!)
                    orientation = originalExif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED
                    )
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return orientation
        }
    }
}