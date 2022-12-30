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
import com.zhihu.matisse.listener.SelectionDelegate

object SelectionSpec {
    @StyleRes
    var themeId = 0
    var mimeTypeSet: Set<MimeType>? = null
    var mediaTypeExclusive = false
    var showSingleMediaType = false
    var orientation = 0
    var countable = false
    var maxSelectable = 0
    var maxImageSelectable = 0
    var maxVideoSelectable = 0
    var filters: List<Filter>? = null
    var capture = false
    var captureStrategy: CaptureStrategy? = null
    var spanCount = 0
    var gridExpectedSize = 0
    var thumbnailScale = 0f
    var imageEngine: ImageEngine? = null
    var hasInit = false
    var onSelectedListener: OnSelectedListener? = null
    var originalable = false
    var autoHideToolbar = false
    var originalMaxSize = 0
    var onCheckedListener: OnCheckedListener? = null
    var delegate: SelectionDelegate? = null
    var showPreview = false

    var hasInited = false
    var enablePrevieworiginalable = false
    var autoHideToobar = false
    var enablePreview = false
    var allowsMultipleSelection = false
    var showUseOrigin = false
    var maxVideoLength = 0
    var hasFeatureEnabled = false
    var isDontShowVideoAlert = false
    var alertTitle: String? = null
    var alertBody: String? = null
    var alertNBtn: String? = null
    var alertPBtn: String? = null

    val cleanInstance: SelectionSpec
        get() = this.apply { reset() }

    val singleSelectionModeEnabled: Boolean
        get() = !countable
                && (maxSelectable == 1 || maxImageSelectable == 1 && maxVideoSelectable == 1)

    val needOrientationRestriction: Boolean
        get() = orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    val onlyShowImages: Boolean
        get() = showSingleMediaType && MimeType.ofImage().containsAll(mimeTypeSet!!)

    val onlyShowVideos: Boolean
        get() = showSingleMediaType && MimeType.ofVideo().containsAll(mimeTypeSet!!)

    val onlyShowGif: Boolean
        get() = showSingleMediaType && MimeType.ofGif() == mimeTypeSet

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
        hasInit = true
        originalable = false
        autoHideToolbar = false
        originalMaxSize = Int.MAX_VALUE

        enablePreview = false
        allowsMultipleSelection = true
        maxVideoLength = 15
        hasFeatureEnabled = false
        isDontShowVideoAlert = false
        alertTitle = "Long Video"
        alertBody = "Video longer than %ds will be trimmed when you upload."
        alertNBtn = "Dont Show"
        alertPBtn = "OK"
        showPreview = true
    }
}
