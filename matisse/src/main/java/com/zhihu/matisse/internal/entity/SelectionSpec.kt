package com.zhihu.matisse.internal.entity


import android.content.pm.ActivityInfo
import androidx.annotation.StyleRes
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.R
import com.zhihu.matisse.engine.ImageEngine
import com.zhihu.matisse.engine.impl.GlideEngine
import com.zhihu.matisse.filter.Filter
import com.zhihu.matisse.listener.OnCheckedListener
import com.zhihu.matisse.listener.OnSelectedListener


class SelectionSpec {
    object InstanceHolder {
        val INSTANCE = SelectionSpec()
    }

    companion object {
        @JvmStatic
        fun getInstance(): SelectionSpec {
            return InstanceHolder.INSTANCE
        }

        @JvmStatic
        fun getCleanInstance(): SelectionSpec {
            val selectionSpec = SelectionSpec()
            selectionSpec.reset()
            return selectionSpec
        }
    }

    var mimeTypeSet: Set<MimeType>? = null
    @JvmField
    var mediaTypeExclusive = false
    @JvmField
    var showSingleMediaType = false

    @StyleRes
    @JvmField
    var themeId = 0

    @JvmField
    var orientation = 0

    @JvmField
    var countable = false

    @JvmField
    var maxSelectable = 0

    @JvmField
    var maxImageSelectable = 0

    @JvmField
    var maxVideoSelectable = 0
    var filters: MutableList<Filter>? = null
    @JvmField
    var capture = false
    var captureStrategy: CaptureStrategy? = null
    @JvmField
    var spanCount = 0
    @JvmField
    var gridExpectedSize = 0
    @JvmField
    var thumbnailScale = 0f
    open var imageEngine: ImageEngine? = null
    @JvmField
    var hasInited = false
    var onSelectedListener: OnSelectedListener? = null
    @JvmField
    var originalable = false
    @JvmField
    var autoHideToobar = false
    @JvmField
    var originalMaxSize = 0
    var onCheckedListener: OnCheckedListener? = null
    @JvmField
    var showPreview = false

    private fun reset() {
        mimeTypeSet = null
        mediaTypeExclusive = true
        showSingleMediaType = false
        themeId = R.style.Matisse_Zhihu
        orientation = 0
        countable = false
        maxSelectable = 1
        maxImageSelectable = 0
        maxVideoSelectable = 0
        filters = null
        capture = false
        captureStrategy = null
        spanCount = 3
        gridExpectedSize = 0
        thumbnailScale = 0.5f
        imageEngine = GlideEngine()
        hasInited = true
        originalable = false
        autoHideToobar = false
        originalMaxSize = Int.MAX_VALUE
        showPreview = true
    }

    fun singleSelectionModeEnabled(): Boolean {
        return !countable && (maxSelectable == 1 || maxImageSelectable == 1 && maxVideoSelectable == 1)
    }

    fun needOrientationRestriction(): Boolean {
        return orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    fun onlyShowImages(): Boolean {
        return showSingleMediaType && MimeType.ofImage().containsAll(mimeTypeSet!!)
    }

    fun onlyShowVideos(): Boolean {
        return showSingleMediaType && MimeType.ofVideo().containsAll(mimeTypeSet!!)
    }

    fun onlyShowGif(): Boolean {
        return showSingleMediaType && MimeType.ofGif().equals(mimeTypeSet)
    }
}
