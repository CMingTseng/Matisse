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
package com.zhihu.matisse.engine.impl;

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import coil.load
import com.zhihu.matisse.engine.ImageEngine

/**
 * {@link ImageEngine} implementation using Coil.
 * https://github.com/coil-kt/coil
 * An image loading library for Android backed by Kotlin Coroutines
 * https://coil-kt.github.io/coil/migrating/
 * https://bleepcoder.com/coil/676698212/resize-images-wider-than-the-device-using-a-scale-factor
 * https://www.section.io/engineering-education/image-processing-with-coil-in-android/
 * https://www.gushiciku.cn/pl/gWZz/zh-tw
 */

class CoilEngine : ImageEngine {
    override fun supportAnimatedGif(): Boolean {
        return true
    }

    override fun loadThumbnail(context: Context, resize: Int, placeholder: Drawable, imageView: ImageView, uri: Uri) {
        imageView.load(uri) {
            size(resize, resize)
             // placeholder image is the image used
            // when our image url fails to load.
            placeholder(placeholder)
//            error(R.drawable.broken_image)
//            precision(Precision.EXACT)
//            scale(Scale.FILL)
            crossfade(true)
//            transformations(CircleCropTransformation())
        }
    }

    override fun loadGifThumbnail(context: Context, resize: Int, placeholder: Drawable, imageView: ImageView, uri: Uri) {
        imageView.load(uri) {
            size(resize, resize)
            // placeholder image is the image used
            // when our image url fails to load.
            placeholder(placeholder)
//            error(R.drawable.broken_image)
//            precision(Precision.EXACT)
//            scale(Scale.FILL)
            crossfade(true)
//            transformations(CircleCropTransformation())
        }
    }

    override fun loadImage(context: Context, resizeX: Int, resizeY: Int, imageView: ImageView, uri: Uri) {
        imageView.load(uri) {
            size(resizeX, resizeY)
            crossfade(true)
//            transformations(CircleCropTransformation())
        }
    }

    override fun loadGifImage(context: Context, resizeX: Int, resizeY: Int, imageView: ImageView, uri: Uri) {
        imageView.load(uri) {
            size(resizeX, resizeY)
            crossfade(true)
//            transformations(CircleCropTransformation())
        }
    }
}
