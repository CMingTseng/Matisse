package com.zhihu.matisse.internal.entity

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import com.zhihu.matisse.MimeType
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class Item(
    val id: Long,
    val mimeType: String?,
    val size: Long,
    val duration: Long,// only for video, in ms
) : Parcelable {

    @IgnoredOnParcel
    val contentUri: Uri

    val isCapture: Boolean get() = id == ITEM_ID_CAPTURE
    val isImage: Boolean get() = MimeType.isImage(mimeType)
    val isGif: Boolean get() = MimeType.isGif(mimeType)
    val isVideo: Boolean get() = MimeType.isVideo(mimeType)

    init {
        val contentUri = when {
            isImage -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }
        this.contentUri = ContentUris.withAppendedId(contentUri, id)
    }

    companion object {

        const val ITEM_ID_CAPTURE: Long = -1
        const val ITEM_DISPLAY_NAME_CAPTURE = "Capture"

        fun valueOf(cursor: Cursor): Item {
            return Item(
                cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)),
                cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)),
                cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)),
                cursor.getLong(cursor.getColumnIndex("duration"))
            )
        }
    }
}