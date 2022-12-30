package com.zhihu.matisse.internal.model

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.IncapableCause
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.ui.widget.CheckView
import com.zhihu.matisse.internal.utils.PathUtils
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils

class SelectedItemCollection(private val context: Context) {
    enum class MaxItemReach {
        NOT_REACH, IMAGE_REACH, VIDEO_REACH, MIX_REACH
    }

    private var items: MutableSet<Item>? = null

    var collectionType = COLLECTION_UNDEFINED
        private set

    val isEmpty: Boolean
        get() = items == null || items!!.isEmpty()

    val dataWithBundle: Bundle
        get() {
            val bundle = Bundle()
            bundle.putParcelableArrayList(STATE_SELECTION, ArrayList(items!!))
            bundle.putInt(STATE_COLLECTION_TYPE, collectionType)
            return bundle
        }

    fun onCreate(bundle: Bundle?) {
        if (bundle == null) {
            items = LinkedHashSet()
        } else {
            val saved: ArrayList<Item>? = bundle.getParcelableArrayList(STATE_SELECTION)
            items = LinkedHashSet(saved)
            collectionType = bundle.getInt(STATE_COLLECTION_TYPE, COLLECTION_UNDEFINED)
        }
    }

    fun setDefaultSelection(uris: List<Item>?) {
        items!!.addAll(uris!!)
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(STATE_SELECTION, ArrayList(items!!))
        outState.putInt(STATE_COLLECTION_TYPE, collectionType)
    }

    fun getDataWithBundle_(): Bundle  {
        val bundle = Bundle()
        bundle.putParcelableArrayList(STATE_SELECTION, ArrayList<Item>(items))
        bundle.putInt(STATE_COLLECTION_TYPE, collectionType)
        return bundle
    }

    fun add(item: Item): Boolean {
        require(!typeConflict(item)) { "Can't select images and videos at the same time." }
        val added = items!!.add(item)
        if (added) {
            if (collectionType == COLLECTION_UNDEFINED) {
                if (item.isImage) {
                    collectionType = COLLECTION_IMAGE
                } else if (item.isVideo) {
                    collectionType = COLLECTION_VIDEO
                }
            } else if (collectionType == COLLECTION_IMAGE) {
                if (item.isVideo) {
                    collectionType = COLLECTION_MIXED
                }
            } else if (collectionType == COLLECTION_VIDEO) {
                if (item.isImage) {
                    collectionType = COLLECTION_MIXED
                }
            }
        }
        return added
    }

    fun remove(item: Item): Boolean {
        val removed = items!!.remove(item)
        if (removed) {
            if (items!!.size == 0) {
                collectionType = COLLECTION_UNDEFINED
            } else {
                if (collectionType == COLLECTION_MIXED) {
                    refineCollectionType()
                }
            }
        }
        return removed
    }

    fun overwrite(items: List<Item>, collectionType: Int) {
        if (items.isEmpty()) {
            this.collectionType = COLLECTION_UNDEFINED
        } else {
            this.collectionType = collectionType
        }
        this.items!!.clear()
        this.items!!.addAll(items)
    }

    fun asList(): List<Item> {
        return ArrayList(items!!)
    }

    fun asListOfUri(): List<Uri> {
        val uris: MutableList<Uri> = ArrayList()
        for (item in items!!) {
            uris.add(item.contentUri)
        }
        return uris
    }

    fun asListOfString(): List<String> {
        val paths = items!!.mapNotNull { item ->
            PathUtils.getPath(context, item.contentUri)
        }
        return paths
    }

    fun isSelected(item: Item): Boolean {
        return items!!.contains(item)
    }

    fun isAcceptable(item: Item): IncapableCause? {
        if (maxSelectableReached()) {
            val maxSelectable = currentMaxSelectable()
            val cause = context.resources.getQuantityString(
                R.plurals.error_over_count,
                maxSelectable,
                maxSelectable
            )
            return IncapableCause(cause)
        } else if (typeConflict(item)) {
            return IncapableCause(context.getString(R.string.error_type_conflict))
        }
        return PhotoMetadataUtils.isAcceptable(context, item)
    }

    fun maxSelectableReached(): Boolean {
        return items!!.size == currentMaxSelectable()
    }

    private fun currentMaxSelectable(): Int {
        val spec = SelectionSpec
        return when {
            spec.maxSelectable > 0 -> spec.maxSelectable
            collectionType == COLLECTION_IMAGE -> spec.maxImageSelectable
            collectionType == COLLECTION_VIDEO -> spec.maxVideoSelectable
            else -> spec.maxSelectable
        }
    }

    private fun refineCollectionType() {
        var hasImage = false
        var hasVideo = false
        for (i in items!!) {
            if (i.isImage && !hasImage) hasImage = true
            if (i.isVideo && !hasVideo) hasVideo = true
        }
        if (hasImage && hasVideo) {
            collectionType = COLLECTION_MIXED
        } else if (hasImage) {
            collectionType = COLLECTION_IMAGE
        } else if (hasVideo) {
            collectionType = COLLECTION_VIDEO
        }
    }

    /**
     * Determine whether there will be conflict media types. A user can only select images and videos at the same time
     * while [SelectionSpec.mediaTypeExclusive] is set to false.
     */
    fun typeConflict(item: Item): Boolean {
        val isVideoOrMixCollection =
            collectionType == COLLECTION_VIDEO || collectionType == COLLECTION_MIXED
        val isImageOrMixCollection =
            collectionType == COLLECTION_IMAGE || collectionType == COLLECTION_MIXED
        return SelectionSpec.mediaTypeExclusive
                && (item.isImage && isVideoOrMixCollection || item.isVideo && isImageOrMixCollection)
    }

    fun count(): Int {
        return items!!.size
    }

    fun checkedNumOf(item: Item): Int {
        val index = items!!.indexOf(item)
        return if (index == -1) CheckView.UNCHECKED else index + 1
    }

    companion object {
        const val STATE_SELECTION = "state_selection"
        const val STATE_COLLECTION_TYPE = "state_collection_type"

        /**
         * Empty collection
         */
        const val COLLECTION_UNDEFINED = 0x00

        /**
         * Collection only with images
         */
        const val COLLECTION_IMAGE = 0x01

        /**
         * Collection only with videos
         */
        const val COLLECTION_VIDEO = 0x01 shl 1

        /**
         * Collection with images and videos.
         */
        const val COLLECTION_MIXED = COLLECTION_IMAGE or COLLECTION_VIDEO
    }
}