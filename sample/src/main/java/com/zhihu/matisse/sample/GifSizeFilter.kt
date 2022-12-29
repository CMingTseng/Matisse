package com.zhihu.matisse.sample

import android.content.Context
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.filter.Filter
import com.zhihu.matisse.internal.entity.IncapableCause
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils.getBitmapBound
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils.getSizeInMB

internal class GifSizeFilter(
    private val minWidth: Int,
    private val minHeight: Int,
    private val maxSize: Int
) : Filter() {

    override fun constraintTypes(): Set<MimeType> {
        return MimeType.ofGif()
    }

    override fun filter(context: Context, item: Item): IncapableCause? {
        if (!needFiltering(context, item))
            return null
        val size = getBitmapBound(context.contentResolver, item.contentUri)
        return if (size.x < minWidth || size.y < minHeight || item.size > maxSize) {
            IncapableCause(
                form = IncapableCause.DIALOG,
                message = context.getString(
                    R.string.error_gif,
                    minWidth,
                    getSizeInMB(maxSize.toLong()).toString()
                )
            )
        } else {
            null
        }
    }
}