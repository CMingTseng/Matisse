package com.zhihu.matisse

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.zhihu.matisse.internal.entity.CaptureStrategy
import java.lang.ref.WeakReference


/**
 * Entry for Matisse's media selection.
 */
class Matisse {
    private constructor(fragment: Fragment) : this(fragment.requireActivity(), fragment)
    private constructor(activity: Activity, fragment: Fragment? = null) {
        context = WeakReference(activity)
        fragmentReference = fragment?.run { WeakReference(fragment) }
    }

    private val context: WeakReference<Activity>?
    private val fragmentReference: WeakReference<Fragment>?

    val activity: Activity? get() = context?.get()
    val fragment: Fragment? get() = fragmentReference?.get()

    /**
     * MIME types the selection constrains on.
     *
     *
     * Types not included in the set will still be shown in the grid but can't be chosen.
     *
     * @param mimeTypes MIME types set user can choose from.
     * @param mediaTypeExclusive Whether can choose images and videos at the same time during one
     * single choosing
     * process. true corresponds to not being able to choose images and videos at the same
     * time, and false corresponds to being able to do this.
     * @return [SelectionCreator] to build select specifications.
     * @see MimeType
     *
     * @see SelectionCreator
     */
    fun choose(mimeTypes: Set<MimeType>, mediaTypeExclusive: Boolean = true): SelectionCreator {
        return SelectionCreator(this, mimeTypes, mediaTypeExclusive)
    }

    @Deprecated(message = "")
    fun performCapture(captureStrategy: CaptureStrategy?, requestCode: Int) {
        choose(MimeType.ofImage(), false).capture(true)
            .captureStrategy(captureStrategy)
            .forCapture(requestCode)
    }


    fun performCapture(
        captureStrategy: CaptureStrategy?,
        launcher: ActivityResultLauncher<Intent>
    ) {
        choose(MimeType.ofImage(), false).capture(true)
            .captureStrategy(captureStrategy)
            .forCapture(launcher)
    }

    companion object {
        const val EXTRA_RESULT_SELECTION = "extra_result_selection"
        const val EXTRA_RESULT_SELECTION_PATH = "extra_result_selection_path"
        const val EXTRA_RESULT_ORIGINAL_ENABLE = "extra_result_original_enable"
        const val CHECK_STATE = "checkState"
        const val REQUEST_CODE_CAPTURE = 24
        /**
         * Start Matisse from an Activity.
         *
         *
         * This Activity's [Activity.onActivityResult] will be called when user
         * finishes selecting.
         *
         * @param activity Activity instance.
         * @return Matisse instance.
         */
        fun from(activity: Activity): Matisse {
            return Matisse(activity)
        }

        /**
         * Start Matisse from a Fragment.
         *
         *
         * This Fragment's [Fragment.onActivityResult] will be called when user
         * finishes selecting.
         *
         * @param fragment Fragment instance.
         * @return Matisse instance.
         */
        fun from(fragment: Fragment): Matisse {
            return Matisse(fragment)
        }

        /**
         * Obtain user selected media' [Uri] list in the starting Activity or Fragment.
         *
         * @param data Intent passed by [Activity.onActivityResult] or
         * [Fragment.onActivityResult].
         * @return User selected media' [Uri] list.
         */
        fun obtainResult(data: Intent?): List<Uri>? {
            return data?.getParcelableArrayListExtra( EXTRA_RESULT_SELECTION)
        }

        /**
         * Obtain user selected media path list in the starting Activity or Fragment.
         *
         * @param data Intent passed by [Activity.onActivityResult] or
         * [Fragment.onActivityResult].
         * @return User selected media path list.
         */
        fun obtainPathResult(data: Intent?): List<String>? {
            return data?.getStringArrayListExtra( EXTRA_RESULT_SELECTION_PATH)
        }

        /**
         * Obtain state whether user decide to use selected media in original
         *
         * @param data Intent passed by [Activity.onActivityResult] or
         * [Fragment.onActivityResult].
         * @return Whether use original photo
         */
        fun obtainOriginalState(data: Intent?): Boolean {
            return data?.getBooleanExtra( EXTRA_RESULT_ORIGINAL_ENABLE, false)
                ?: false
        }
    }
}