package com.zhihu.matisse.internal.loader

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MergeCursor
import android.provider.MediaStore
import androidx.loader.content.CursorLoader
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.internal.entity.Album
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec.onlyShowGif
import com.zhihu.matisse.internal.entity.SelectionSpec.onlyShowImages
import com.zhihu.matisse.internal.entity.SelectionSpec.onlyShowVideos
import com.zhihu.matisse.internal.utils.MediaStoreCompat

/**
 * Load images and videos into a single cursor.
 */
class AlbumMediaLoader private constructor(
    context: Context,
    selection: String,
    selectionArgs: Array<String>,
    private val enableCapture: Boolean
) : CursorLoader(context, QUERY_URI, PROJECTION, selection, selectionArgs, ORDER_BY) {

    override fun loadInBackground(): Cursor? {
        val result = super.loadInBackground()
        if (!enableCapture || !MediaStoreCompat.hasCameraFeature(context)) {
            return result
        }
        val dummy = MatrixCursor(PROJECTION).apply {
            addRow(arrayOf<Any>(Item.ITEM_ID_CAPTURE, Item.ITEM_DISPLAY_NAME_CAPTURE, "", 0, 0))
        }
        return MergeCursor(arrayOf(dummy, result))
    }

    override fun onContentChanged() {
        // FIXME a dirty way to fix loading multiple times
    }

    companion object {
        private val QUERY_URI = MediaStore.Files.getContentUri("external")
        private val PROJECTION = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            "duration"
        )

        // === params for album ALL && showSingleMediaType: false ===
        private const val SELECTION_ALL = ("(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0")
        private val SELECTION_ALL_ARGS = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        // ===========================================================
        // === params for album ALL && showSingleMediaType: true ===
        private const val SELECTION_ALL_FOR_SINGLE_MEDIA_TYPE =
            (MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0")

        // === params for ordinary album && showSingleMediaType: false ===
        private const val SELECTION_ALBUM = ("(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                + " AND "
                + " bucket_id=?"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0")

        // =========================================================
        // === params for ordinary album && showSingleMediaType: true ===
        private const val SELECTION_ALBUM_FOR_SINGLE_MEDIA_TYPE =
            (MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND "
                    + " bucket_id=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0")

        // === params for album ALL && showSingleMediaType: true && MineType=="image/gif"
        private const val SELECTION_ALL_FOR_GIF = (MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " AND "
                + MediaStore.MediaColumns.MIME_TYPE + "=?"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0")

        // ===============================================================
        // === params for ordinary album && showSingleMediaType: true  && MineType=="image/gif" ===
        private const val SELECTION_ALBUM_FOR_GIF = (MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " AND "
                + " bucket_id=?"
                + " AND "
                + MediaStore.MediaColumns.MIME_TYPE + "=?"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0")
        private const val ORDER_BY = MediaStore.Images.Media.DATE_MODIFIED + " DESC"

        // ===============================================================
        private fun getSelectionArgsForSingleMediaType(mediaType: Int): Array<String> {
            return arrayOf(mediaType.toString())
        }

        private fun getSelectionAlbumArgs(albumId: String): Array<String> {
            return arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                albumId
            )
        }

        // ===============================================================
        private fun getSelectionAlbumArgsForSingleMediaType(
            mediaType: Int,
            albumId: String
        ): Array<String> {
            return arrayOf(mediaType.toString(), albumId)
        }

        private fun getSelectionArgsForGifType(mediaType: Int): Array<String> {
            return arrayOf(mediaType.toString(), MimeType.GIF.mimeTypeName)
        }

        private fun getSelectionAlbumArgsForGifType(
            mediaType: Int,
            albumId: String
        ): Array<String> {
            return arrayOf(mediaType.toString(), albumId, MimeType.GIF.mimeTypeName)
        }

        fun newInstance(context: Context, album: Album, capture: Boolean): CursorLoader {
            val selection: String
            val selectionArgs: Array<String>
            val enableCapture: Boolean
            if (album.isAll) {
                when {
                    onlyShowGif -> {
                        selection = SELECTION_ALL_FOR_GIF
                        selectionArgs = getSelectionArgsForGifType(
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                        )
                    }
                    onlyShowImages -> {
                        selection = SELECTION_ALL_FOR_SINGLE_MEDIA_TYPE
                        selectionArgs = getSelectionArgsForSingleMediaType(
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                        )
                    }
                    onlyShowVideos -> {
                        selection = SELECTION_ALL_FOR_SINGLE_MEDIA_TYPE
                        selectionArgs = getSelectionArgsForSingleMediaType(
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                        )
                    }
                    else -> {
                        selection = SELECTION_ALL
                        selectionArgs = SELECTION_ALL_ARGS
                    }
                }
                enableCapture = capture
            } else {
                when {
                    onlyShowGif -> {
                        selection = SELECTION_ALBUM_FOR_GIF
                        selectionArgs = getSelectionAlbumArgsForGifType(
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE, album.id
                        )
                    }
                    onlyShowImages -> {
                        selection = SELECTION_ALBUM_FOR_SINGLE_MEDIA_TYPE
                        selectionArgs = getSelectionAlbumArgsForSingleMediaType(
                            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                            album.id
                        )
                    }
                    onlyShowVideos -> {
                        selection = SELECTION_ALBUM_FOR_SINGLE_MEDIA_TYPE
                        selectionArgs = getSelectionAlbumArgsForSingleMediaType(
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                            album.id
                        )
                    }
                    else -> {
                        selection = SELECTION_ALBUM
                        selectionArgs = getSelectionAlbumArgs(album.id)
                    }
                }
                enableCapture = false
            }
            return AlbumMediaLoader(context, selection, selectionArgs, enableCapture)
        }
    }
}