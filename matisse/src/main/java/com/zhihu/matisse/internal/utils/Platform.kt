package com.zhihu.matisse.internal.utils;

import android.os.Build;

/**
 * @author JoongWon Baik
 */
object Platform {
    @JvmStatic
    fun hasICS(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH
    }

    @JvmStatic
    fun hasKitKat(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
    }
}
