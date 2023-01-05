package com.zhihu.matisse.internal.entity

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Parcelable
import com.zhihu.matisse.R
import com.zhihu.matisse.internal.loader.AlbumLoader
import kotlinx.parcelize.Parcelize

@Parcelize
class Album(
    val id: String,
    val coverUri: Uri?,
    private val displayName: String?,
    private var counter: Long,
) : Parcelable {

    val isAll: Boolean get() = ALBUM_ID_ALL == id
    val isEmpty: Boolean get() = counter == 0L
    val count: Long get() = counter

    fun addCaptureCount() {
        counter++
    }

    fun getDisplayName(context: Context): String? {
        return if (isAll) {
            context.getString(R.string.album_name_all)
        } else {
            displayName
        }
    }

    companion object {
        const val ALBUM_ID_ALL: String = "-1"
        const val ALBUM_NAME_ALL = "All"

        /**
         * Constructs a new [Album] entity from the [Cursor].
         * This method is not responsible for managing cursor resource, such as close, iterate, and so on.
         */
        fun valueOf(cursor: Cursor): Album {
            val column = cursor.getString(cursor.getColumnIndex(AlbumLoader.COLUMN_URI))
            return Album(
                cursor.getString(cursor.getColumnIndex("bucket_id")),
                Uri.parse(column ?: ""),
                cursor.getString(cursor.getColumnIndex("bucket_display_name")),
                cursor.getLong(cursor.getColumnIndex(AlbumLoader.COLUMN_COUNT))
            )
        }
    }
}