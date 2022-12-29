package com.zhihu.matisse

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils
import java.util.*

/**
 * MIME Type enumeration to restrict selectable media on the selection activity.
 * Matisse only supports images and videos.
 *
 * Good example of mime types Android supports:
 * https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/media/java/android/media/MediaFile.java
 */
enum class MimeType(
    val mimeTypeName: String,
    private val extensions: Set<String>
) {
    // ============== images ==============
    JPEG("image/jpeg", setOf("jpg", "jpeg")),
    PNG("image/png", setOf("png")),
    GIF("image/gif", setOf("gif")),
    BMP("image/x-ms-bmp", setOf("bmp")),
    WEBP("image/webp", setOf("webp")),

    // ============== videos ==============
    MPEG("video/mpeg", setOf("mpeg", "mpg")),
    MP4("video/mp4", setOf("mp4", "m4v")),
    QUICKTIME("video/quicktime", setOf("mov")),
    THREEGPP("video/3gpp", setOf("3gp", "3gpp")),
    THREEGPP2("video/3gpp2", setOf("3g2", "3gpp2")),
    MKV("video/x-matroska", setOf("mkv")),
    WEBM("video/webm", setOf("webm")),
    TS("video/mp2ts", setOf("ts")),
    AVI("video/avi", setOf("avi"));

    override fun toString(): String {
        return mimeTypeName
    }

    fun checkType(resolver: ContentResolver, uri: Uri?): Boolean {
        val map = MimeTypeMap.getSingleton()
        if (uri == null) {
            return false
        }
        val type = map.getExtensionFromMimeType(resolver.getType(uri))
        var path: String? = null
        // lazy load the path and prevent resolve for multiple times
        var pathParsed = false
        for (extension in extensions) {
            if (extension == type) {
                return true
            }
            if (!pathParsed) {
                // we only resolve the path for one time
                path = PhotoMetadataUtils.getPath(resolver, uri)
                if (!path.isNullOrEmpty()) {
                    path = path.lowercase(Locale.US)
                }
                pathParsed = true
            }
            if (path != null && path.endsWith(extension)) {
                return true
            }
        }
        return false
    }

    companion object {
        fun ofAll(): Set<MimeType> {
            return values().toSet()
        }

        fun of(type: MimeType, vararg rest: MimeType): Set<MimeType> {
            return setOf(type, *rest)
        }

        fun ofImage(): Set<MimeType> {
            return setOf(JPEG, GIF, PNG, BMP, WEBP)
        }

        fun ofGif(): Set<MimeType> {
            return setOf(GIF)
        }

        fun ofVideo(): Set<MimeType> {
            return setOf(MPEG, MP4, QUICKTIME, THREEGPP, THREEGPP2, MKV, WEBM, TS, AVI)
        }

        fun isImage(mimeType: String?): Boolean {
            return mimeType?.startsWith("image") ?: false
        }

        fun isVideo(mimeType: String?): Boolean {
            return mimeType?.startsWith("video") ?: false
        }

        fun isGif(mimeType: String?): Boolean {
            return mimeType == GIF.toString()
        }
    }
}