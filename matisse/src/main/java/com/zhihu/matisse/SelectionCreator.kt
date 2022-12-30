package com.zhihu.matisse

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo.*
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import com.zhihu.matisse.engine.ImageEngine
import com.zhihu.matisse.filter.Filter
import com.zhihu.matisse.internal.entity.CaptureStrategy
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.loader.AlbumMediaLoader
import com.zhihu.matisse.internal.model.SelectedItemCollection
import com.zhihu.matisse.listener.OnCheckedListener
import com.zhihu.matisse.listener.OnSelectedListener
import com.zhihu.matisse.listener.SelectionDelegate
import com.zhihu.matisse.ui.CaptureDelegateActivity
import com.zhihu.matisse.ui.MatisseActivity
import java.util.*

/**
 * Fluent API for building media select specification.
 *
 * @param matisse   a requester context wrapper.
 * @param mimeTypes MIME type set to select.
 */
class SelectionCreator internal constructor(
    private val matisse: Matisse,
    mimeTypes: Set<MimeType>,
    mediaTypeExclusive: Boolean
) {
    private val selectionSpec: SelectionSpec = SelectionSpec.cleanInstance.apply {
        this.mimeTypeSet = mimeTypes
        this.mediaTypeExclusive = mediaTypeExclusive
        this.orientation = SCREEN_ORIENTATION_UNSPECIFIED
    }

    /**
     * Whether to show only one media type if choosing medias are only images or videos.
     *
     * @param showSingleMediaType whether to show only one media type, either images or videos.
     * @return [SelectionCreator] for fluent API.
     * @see SelectionSpec.onlyShowImages
     * @see SelectionSpec.onlyShowVideos
     */
    fun showSingleMediaType(showSingleMediaType: Boolean): SelectionCreator {
        selectionSpec.showSingleMediaType = showSingleMediaType
        return this
    }

    /**
     * Theme for media selecting Activity.
     *
     *
     * There are two built-in themes:
     * 1. com.zhihu.matisse.R.style.Matisse_Zhihu;
     * 2. com.zhihu.matisse.R.style.Matisse_Dracula
     * you can define a custom theme derived from the above ones or other themes.
     *
     * @param themeId theme resource id. Default value is com.zhihu.matisse.R.style.Matisse_Zhihu.
     * @return [SelectionCreator] for fluent API.
     */
    fun theme(@StyleRes themeId: Int): SelectionCreator {
        selectionSpec.themeId = themeId
        return this
    }

    /**
     * Show a auto-increased number or a check mark when user select media.
     *
     * @param countable true for a auto-increased number from 1, false for a check mark. Default
     * value is false.
     * @return [SelectionCreator] for fluent API.
     */
    fun countable(countable: Boolean): SelectionCreator {
        selectionSpec.countable = countable
        return this
    }

    /**
     * Maximum selectable count.
     *
     * @param maxSelectable Maximum selectable count. Default value is 1.
     * @return [SelectionCreator] for fluent API.
     */
    fun maxSelectable(maxSelectable: Int): SelectionCreator {
        require(maxSelectable >= 1) { "maxSelectable must be greater than or equal to one" }
        check(!(selectionSpec.maxImageSelectable > 0 || selectionSpec.maxVideoSelectable > 0)) { "already set maxImageSelectable and maxVideoSelectable" }
        selectionSpec.maxSelectable = maxSelectable
        return this
    }

    /**
     * Only useful when [SelectionSpec.mediaTypeExclusive] set true and you want to set different maximum
     * selectable files for image and video media types.
     *
     * @param maxImageSelectable Maximum selectable count for image.
     * @param maxVideoSelectable Maximum selectable count for video.
     * @return [SelectionCreator] for fluent API.
     */
    fun maxSelectablePerMediaType(
        maxImageSelectable: Int,
        maxVideoSelectable: Int
    ): SelectionCreator {
        require(!(maxImageSelectable < 1 || maxVideoSelectable < 1)) { "max selectable must be greater than or equal to one" }
        selectionSpec.maxSelectable = -1
        selectionSpec.maxImageSelectable = maxImageSelectable
        selectionSpec.maxVideoSelectable = maxVideoSelectable
        return this
    }

    /**
     * Add filter to filter each selecting item.
     *
     * @param filter [Filter]
     * @return [SelectionCreator] for fluent API.
     */
    fun addFilter(filter: Filter): SelectionCreator {
        if (selectionSpec.filters == null) {
            selectionSpec.filters = mutableListOf()
        }
        selectionSpec.filters!!.toMutableList().add(filter)
        return this
    }

    /**
     * Determines whether the photo capturing is enabled or not on the media grid view.
     *
     *
     * If this value is set true, photo capturing entry will appear only on All Media's page.
     *
     * @param enable Whether to enable capturing or not. Default value is false;
     * @return [SelectionCreator] for fluent API.
     */
    fun capture(enable: Boolean): SelectionCreator {
        selectionSpec.capture = enable
        return this
    }

    /**
     * Show a original photo check options.Let users decide whether use original photo after select
     *
     * @param enable Whether to enable original photo or not
     * @return [SelectionCreator] for fluent API.
     */
    fun originalEnable(enable: Boolean): SelectionCreator {
        selectionSpec.originalable = enable
        return this
    }

    /**
     * Determines Whether to hide top and bottom toolbar in PreView mode ,when user tap the picture
     *
     * @return [SelectionCreator] for fluent API.
     */
    fun autoHideToolbarOnSingleTap(enable: Boolean): SelectionCreator {
        selectionSpec.autoHideToolbar = enable
        return this
    }

    /**
     * Maximum original size,the unit is MB. Only useful when {link@originalEnable} set true
     *
     * @param size Maximum original size. Default value is Integer.MAX_VALUE
     * @return [SelectionCreator] for fluent API.
     */
    fun maxOriginalSize(size: Int): SelectionCreator {
        selectionSpec.originalMaxSize = size
        return this
    }

    /**
     * Capture strategy provided for the location to save photos including internal and external
     * storage and also a authority for [androidx.core.content.FileProvider].
     *
     * @param captureStrategy [CaptureStrategy], needed only when capturing is enabled.
     * @return [SelectionCreator] for fluent API.
     */
    fun captureStrategy(captureStrategy: CaptureStrategy?): SelectionCreator {
        selectionSpec.captureStrategy = captureStrategy
        return this
    }

    /**
     * Set the desired orientation of this activity.
     *
     * @param orientation An orientation constant as used in [ScreenOrientation].
     * Default value is [android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT].
     * @return [SelectionCreator] for fluent API.
     * @see Activity.setRequestedOrientation
     */
    fun restrictOrientation(@ScreenOrientation orientation: Int): SelectionCreator {
        selectionSpec.orientation = orientation
        return this
    }

    /**
     * Set a fixed span count for the media grid. Same for different screen orientations.
     *
     *
     * This will be ignored when [.gridExpectedSize] is set.
     *
     * @param spanCount Requested span count.
     * @return [SelectionCreator] for fluent API.
     */
    fun spanCount(spanCount: Int): SelectionCreator {
        require(spanCount >= 1) { "spanCount cannot be less than 1" }
        selectionSpec.spanCount = spanCount
        return this
    }

    /**
     * Set expected size for media grid to adapt to different screen sizes. This won't necessarily
     * be applied cause the media grid should fill the view container. The measured media grid's
     * size will be as close to this value as possible.
     *
     * @param size Expected media grid size in pixel.
     * @return [SelectionCreator] for fluent API.
     */
    fun gridExpectedSize(size: Int): SelectionCreator {
        selectionSpec.gridExpectedSize = size
        return this
    }

    /**
     * Photo thumbnail's scale compared to the View's size. It should be a float value in (0.0,
     * 1.0].
     *
     * @param scale Thumbnail's scale in (0.0, 1.0]. Default value is 0.5.
     * @return [SelectionCreator] for fluent API.
     */
    fun thumbnailScale(scale: Float): SelectionCreator {
        require(!(scale <= 0f || scale > 1f)) { "Thumbnail scale must be between (0.0, 1.0]" }
        selectionSpec.thumbnailScale = scale
        return this
    }

    /**
     * Provide an image engine.
     *
     *
     * There are two built-in image engines:
     * - [com.zhihu.matisse.engine.impl.GlideEngine]
     * And you can implement your own image engine.
     *
     * @param imageEngine [ImageEngine]
     * @return [SelectionCreator] for fluent API.
     */
    fun imageEngine(imageEngine: ImageEngine?): SelectionCreator {
        selectionSpec.imageEngine = imageEngine
        return this
    }

    /**
     * Set listener for callback immediately when user select or unselect something.
     *
     *
     * It's a redundant API with [Matisse.obtainResult],
     * we only suggest you to use this API when you need to do something immediately.
     *
     * @param listener [OnSelectedListener]
     * @return [SelectionCreator] for fluent API.
     */
    fun setOnSelectedListener(listener: OnSelectedListener?): SelectionCreator {
        selectionSpec.onSelectedListener = listener
        return this
    }

    /**
     * Set listener for callback immediately when user check or uncheck original.
     *
     * @param listener [OnSelectedListener]
     * @return [SelectionCreator] for fluent API.
     */
    fun setOnCheckedListener(listener: OnCheckedListener?): SelectionCreator {
        selectionSpec.onCheckedListener = listener
        return this
    }

    /**
     * 是否显示使用原图按钮，默认不显示
     * @param show
     * @return
     */
    fun showUseOrigin(show: Boolean): SelectionCreator? {
        selectionSpec.showUseOrigin = show
        return this
    }

    fun enablePreview(b: Boolean): SelectionCreator? {
        selectionSpec.enablePreview = b
        return this
    }

    /**
     * Use case for avatar or any single selection of photo or video
     */
    fun allowsMultipleSelection(b: Boolean): SelectionCreator? {
        selectionSpec.allowsMultipleSelection = b
        return this
    }

    /**
     * Use case for extended video, max video length or default
     */
    fun maxVideoLength(length: Int): SelectionCreator? {
        selectionSpec.maxVideoLength = length
        return this
    }

    /**
     * Use case for extended video, is feature enabled
     */
    fun hasFeatureEnabled(enabled: Boolean): SelectionCreator? {
        selectionSpec.hasFeatureEnabled = enabled
        return this
    }

    /**
     * Use case for extended video, is feature enabled
     */
    fun dontShowVideoAlert(isShown: Boolean): SelectionCreator? {
        selectionSpec.isDontShowVideoAlert = isShown
        return this
    }

    /**
     * Use case for extended video, is feature enabled
     * alertTitle: Alert Title display
     * alertBody: Alert Body message displays
     * alertNBtn: Alert negative button label
     * alertPBtn: Alert positive button label
     */
    fun dontShowVideoAlertData(
        alertTitle: String,
        alertBody: String,
        alertNBtn: String,
        alertPBtn: String
    ): SelectionCreator? {
        selectionSpec.alertTitle = alertTitle
        selectionSpec.alertBody = alertBody
        selectionSpec.alertNBtn = alertNBtn
        selectionSpec.alertPBtn = alertPBtn
        return this
    }

    /**
     * Start to select media and wait for result.
     *
     * @param requestCode Identity of the request Activity or Fragment.
     */
    @Deprecated("")
    fun forResult(requestCode: Int) {
        val activity = matisse.activity ?: return
        val intent = Intent(activity, MatisseActivity::class.java)
        val fragment = matisse.fragment
        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode)
        } else {
            activity.startActivityForResult(intent, requestCode)
        }
    }

    fun forResult(launcher: ActivityResultLauncher<Intent?>) {
        val activity = matisse.activity ?: return
        val intent = Intent(activity, MatisseActivity::class.java)
        launcher.launch(intent)
    }

    /**
     * Start to select media and wait for result.
     *
     * @param requestCode Identity of the request Activity or Fragment.
     */
    fun forResult(requestCode: Int, selectedUris: List<Uri>?) {
        val activity: Activity = matisse.activity ?: return
        val intent = Intent(activity, MatisseActivity::class.java)
        if (selectedUris != null && selectedUris.size > 0) {
            val selection: ArrayList<Item> = AlbumMediaLoader.querySelection(activity, selectedUris)
            intent.putExtra(SelectedItemCollection.STATE_SELECTION, selection)
        }
        val fragment: Fragment? =matisse.fragment
        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode)
        } else {
            activity.startActivityForResult(intent, requestCode)
        }
    }


    /**
     * when selection event emit register can observe
     * @param delegate
     * @return
     */
    fun delegate(delegate: SelectionDelegate): SelectionCreator? {
        selectionSpec.delegate = delegate
        return this
    }

    fun showPreview(showPreview: Boolean): SelectionCreator {
        selectionSpec.showPreview = showPreview
        return this
    }

    /**
     * take photo directly
     */
    @Deprecated("")
    fun forCapture(requestCode: Int) {
        val activity = matisse.activity ?: return
        val intent = Intent(activity, CaptureDelegateActivity::class.java)
        val fragment = matisse.fragment
        if (fragment != null) {
            fragment.startActivityForResult(intent, requestCode)
        } else {
            activity.startActivityForResult(intent, requestCode)
        }
    }

    fun forCapture(launcher: ActivityResultLauncher<Intent>) {
        val activity = matisse.activity ?: return
        val intent = Intent(activity, CaptureDelegateActivity::class.java)
        launcher.launch(intent)
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @IntDef(
        SCREEN_ORIENTATION_UNSPECIFIED,
        SCREEN_ORIENTATION_LANDSCAPE,
        SCREEN_ORIENTATION_PORTRAIT,
        SCREEN_ORIENTATION_USER,
        SCREEN_ORIENTATION_BEHIND,
        SCREEN_ORIENTATION_SENSOR,
        SCREEN_ORIENTATION_NOSENSOR,
        SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
        SCREEN_ORIENTATION_SENSOR_PORTRAIT,
        SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
        SCREEN_ORIENTATION_REVERSE_PORTRAIT,
        SCREEN_ORIENTATION_FULL_SENSOR,
        SCREEN_ORIENTATION_USER_LANDSCAPE,
        SCREEN_ORIENTATION_USER_PORTRAIT,
        SCREEN_ORIENTATION_FULL_USER,
        SCREEN_ORIENTATION_LOCKED
    )
    @Retention(
        AnnotationRetention.SOURCE
    )
    internal annotation class ScreenOrientation
}