/*
 * Copyright (C) 2014 nohana, Inc.
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.utils;

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log

import com.zhihu.matisse.R
import com.zhihu.matisse.internal.entity.Item
import com.zhihu.matisse.internal.entity.SelectionSpec
import com.zhihu.matisse.internal.entity.IncapableCause

import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

class PhotoMetadataUtils {
    companion object {
        @JvmStatic
        val TAG = "PhotoMetadataUtils"
        val MAX_WIDTH = 1600
        val SCHEME_CONTENT = "content"

        @JvmStatic
        fun getPath(resolver: ContentResolver, uri: Uri?): String? {
            if (uri == null) {
                return null
            }
            if (SCHEME_CONTENT == uri.scheme) {
                var cursor: Cursor? = null
                return try {
                    cursor = resolver.query(uri, arrayOf(MediaStore.Images.ImageColumns.DATA),
                            null, null, null)
                    if (cursor == null || !cursor.moveToFirst()) {
                        null
                    } else cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA))
                } finally {
                    cursor?.close()
                }
            }
            return uri.path
        }

        @JvmStatic
        fun getPixelsCount(resolver: ContentResolver, uri: Uri?): Int {
            val size = getBitmapBound(resolver, uri)
            return size.x * size.y
        }

        @JvmStatic
        fun getBitmapSize(uri: Uri, activity: Activity): Point {
            val resolver = activity.contentResolver
            val imageSize = getBitmapBound(resolver, uri)
            var w = imageSize.x
            var h = imageSize.y
            if (shouldRotate(resolver, uri)) {
                w = imageSize.y
                h = imageSize.x
            }
            if (h == 0) return Point(MAX_WIDTH, MAX_WIDTH)
            val metrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            val screenWidth = metrics.widthPixels.toFloat()
            val screenHeight = metrics.heightPixels.toFloat()
            val widthScale = screenWidth / w
            val heightScale = screenHeight / h
            return if (widthScale > heightScale) {
                Point((w * widthScale).toInt(), (h * heightScale).toInt())
            } else Point((w * widthScale).toInt(), (h * heightScale).toInt())
        }

        @JvmStatic
        fun getBitmapBound(resolver: ContentResolver, uri: Uri?): Point {
            var inputstream: InputStream? = null
            return try {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                inputstream = resolver.openInputStream(uri!!)
                BitmapFactory.decodeStream(inputstream, null, options)
                val width = options.outWidth
                val height = options.outHeight
                Point(width, height)
            } catch (e: FileNotFoundException) {
                Point(0, 0)
            } finally {
                if (inputstream != null) {
                    try {
                        inputstream.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        @JvmStatic
        fun isAcceptable(context: Context, item: Item): IncapableCause? {
            if (!isSelectableType(context, item)) {
                return IncapableCause(context.getString(R.string.error_file_type))
            }
            if (SelectionSpec.getInstance().filters != null) {
                for (filter in SelectionSpec.getInstance().filters) {
                    val incapableCause = filter.filter(context, item)
                    if (incapableCause != null) {
                        return incapableCause
                    }
                }
            }
            return null
        }

        private fun isSelectableType(context: Context?, item: Item): Boolean {
            if (context == null) {
                return false
            }
            val resolver = context.contentResolver
            for (type in SelectionSpec.getInstance().mimeTypeSet) {
                if (type.checkType(resolver, item.contentUri)) {
                    return true
                }
            }
            return false
        }

        private fun shouldRotate(resolver: ContentResolver, uri: Uri): Boolean {
            val exif: ExifInterface
            exif = try {
                ExifInterfaceCompat.newInstance(getPath(resolver, uri))
            } catch (e: IOException) {
                Log.e(TAG, "could not read exif info of the image: $uri")
                return false
            }
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
            return (orientation == ExifInterface.ORIENTATION_ROTATE_90
                    || orientation == ExifInterface.ORIENTATION_ROTATE_270)
        }

        @JvmStatic
        fun getSizeInMB(sizeInBytes: Long): Float {
            val df = NumberFormat.getNumberInstance(Locale.US) as DecimalFormat
            df.applyPattern("0.0")
            var result = df.format((sizeInBytes.toFloat() / 1024 / 1024).toDouble())
            Log.e(TAG, "getSizeInMB: $result")
            result = result.replace(",".toRegex(), ".") // in some case , 0.0 will be 0,0
            return java.lang.Float.valueOf(result)
        }

        @JvmStatic
        fun sizeFormatter(size: Long): String {
            val fileSize = size.toDouble()
            return if (fileSize < 1024) {
                fileSize.toString() + "B"
            } else if (fileSize > 1024 && fileSize < 1024 * 1024) {
                ((fileSize / 1024 * 100.0).roundToInt() / 100.0).toString() + "KB"
            } else {
                ((fileSize / (1024 * 1204) * 100.0).roundToInt() / 100.0).toString() + "MB"
            }
        }
    }

    private fun PhotoMetadataUtils() {
        throw AssertionError("oops! the utility class is about to be instantiated...")
    }

    //https://github.com/aminography/CommonUtils/blob/master/library/src/main/java/com/aminography/commonutils/HumanizeUtils.kt

    val String.formatAsFileSize: String
        get() = toLong().formatAsFileSize

    val Int.formatAsFileSize: String
        get() = toLong().formatAsFileSize

    val Long.formatAsFileSize: String
        get() = log2(if (this != 0L) toDouble() else 1.0).toInt().div(10).let {
            val precision = when (it) {
                0 -> 0; 1 -> 1; else -> 2
            }
            val prefix = arrayOf("", "K", "M", "G", "T", "P", "E", "Z", "Y")
            String.format("%.${precision}f ${prefix[it]}B", toDouble() / 2.0.pow(it * 10.0))
        }
}
