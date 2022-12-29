package com.zhihu.matisse.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri

object ImageUtil {

    fun isLongBitmap(context: Context, uri: Uri): Boolean {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return false
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(input, null, options)
            input.close()
            options.outWidth * 3 <= options.outHeight
        } catch (e: Exception) {
            false
        }
    }
}