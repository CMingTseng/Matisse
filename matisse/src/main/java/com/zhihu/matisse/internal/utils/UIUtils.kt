/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.utils;

import android.content.Context;

object UIUtils {
    @JvmStatic
    fun spanCount(context: Context, gridExpectedSize: Int): Int {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val expected = screenWidth.toFloat() / gridExpectedSize.toFloat()
        var spanCount = Math.round(expected)
        if (spanCount == 0) {
            spanCount = 1
        }
        return spanCount
    }
}

//TODO if class

//class UIUtils {
//    companion object {
//        @JvmStatic  //TODo add @JvmStatic let Java not use UIUtils.Companion.spanCount
//        fun spanCount(context: Context, gridExpectedSize: Int): Int {
//            val screenWidth = context.resources.displayMetrics.widthPixels
//            val expected = screenWidth.toFloat() / gridExpectedSize.toFloat()
//            var spanCount = Math.round(expected)
//            if (spanCount == 0) {
//                spanCount = 1
//            }
//            return spanCount
//        }
//    }
//}
