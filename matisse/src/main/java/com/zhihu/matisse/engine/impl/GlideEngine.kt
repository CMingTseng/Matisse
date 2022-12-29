package com.zhihu.matisse.engine.impl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.zhihu.matisse.engine.ImageEngine

/**
 * [ImageEngine] implementation using Glide.
 */
class GlideEngine : ImageEngine {
    override fun loadThumbnail(
        context: Context,
        resize: Int,
        placeholder: Drawable?,
        imageView: ImageView,
        uri: Uri?
    ) {
        Glide.with(context)
            .asBitmap() // some .jpeg files are actually gif
            .load(uri)
            .placeholder(placeholder)
            .override(resize, resize)
            .centerCrop()
            .into(imageView)
    }

    override fun loadGifThumbnail(
        context: Context,
        resize: Int,
        placeholder: Drawable?,
        imageView: ImageView,
        uri: Uri?
    ) {
        Glide.with(context)
            .asBitmap() // some .jpeg files are actually gif
            .load(uri)
            .placeholder(placeholder)
            .override(resize, resize)
            .centerCrop()
            .into(imageView)
    }

    override fun loadImage(
        context: Context,
        resizeX: Int,
        resizeY: Int,
        imageView: ImageView,
        uri: Uri?
    ) {
        Glide.with(context)
            .load(uri)
            .apply(
                RequestOptions()
                    .override(resizeX, resizeY)
                    .priority(Priority.HIGH)
                    .fitCenter()
            )
            .into(imageView)
    }

    override fun loadLargeImage(
        context: Context,
        resizeX: Int,
        resizeY: Int,
        imageView: SubsamplingScaleImageView,
        uri: Uri?
    ) {
        Glide.with(context)
            .asBitmap()
            .load(uri)
            .apply(
                RequestOptions()
                    .override(resizeX, resizeY)
                    .priority(Priority.HIGH)
                    .fitCenter()
            )
            .into(object : CustomViewTarget<View, Bitmap>(imageView) {
                override fun onLoadFailed(errorDrawable: Drawable?) {}
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    imageView.setImage(ImageSource.cachedBitmap(resource))
                }

                override fun onResourceCleared(placeholder: Drawable?) {}
            })
    }

    override fun loadGifImage(
        context: Context,
        resizeX: Int,
        resizeY: Int,
        imageView: ImageView,
        uri: Uri?
    ) {
        Glide.with(context)
            .asGif()
            .load(uri)
            .override(resizeX, resizeY)
            .priority(Priority.HIGH)
            .into(imageView)
    }

    override fun supportAnimatedGif(): Boolean {
        return true
    }
}