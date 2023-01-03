package com.zhihu.matisse.sample

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class VideoFrameExtractor {
    private val workscope = CoroutineScope(Dispatchers.IO)
    private var _filepaths: MutableStateFlow<String?> = MutableStateFlow(null)
    val filepaths: StateFlow<String?> = _filepaths

    companion object {
        private val LOCK = Any()
        private var instance: VideoFrameExtractor? = null

        @JvmStatic
        fun getExtractor(): VideoFrameExtractor {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = VideoFrameExtractor()
                }
            }
            return instance!!
        }
    }

    //extractBitmapToFile
    fun extractFramesToFile(
        save_path: String,
        videoUrl: String,
        per_sec: Long = 5000L,
        save_image_type: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 100
    ) {
        workscope.launch {
            //FIXME  extractFrames not work
            extractFrames(videoUrl, per_sec).flatMapConcat { bitmap ->
                Log.e("VideoFrameExtractor", "Show extractFramesToFile flatMapConcat : $bitmap")
                if (bitmap != null) {
                    extractBitmapToFile(
                        bitmap,
                        type = save_image_type,
                        quality = quality,
                        path = save_path
                    )
                } else {
                    flow {
                        emit(null)
                    }
                }
            }.collect {
                Log.e("VideoFrameExtractor", "Show extract path : $it")
                _filepaths.emit(it)
            }
        }
    }

    suspend fun extractFrames(videoUrl: String, per_sec: Long = 5000L): Flow<Bitmap?> = flow {
        Log.e("VideoFrameExtractor", "Show extractFrames")
        MediaMetadataRetriever().apply {
            setDataSource(videoUrl)
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                var duration = it.toLong()
                var millsecPerThumbnail: Long = 3000
                if (duration <= 20 * 1000) {
                    millsecPerThumbnail = 1000
                } else if (duration > 60 * 1000) {
                    millsecPerThumbnail = 5000
                }
                val thumbnailCount =
                    Math.ceil((duration * 1f / millsecPerThumbnail).toDouble()).toInt()
                var millSec = 0L
                for (i in 0 until thumbnailCount) {
                    if (millSec > duration) {
                        millSec = duration
                    }
                    //                    thumbnailMillSecList.add(millSec)
                    Log.d("VideoFrameExtractor", "getThumbnail()  [\$i] time:\$millSec")
                    val bitmap = getFrameAtTime(
                        TimeUnit.MICROSECONDS.convert(
                            millSec,
                            TimeUnit.MILLISECONDS
                        )
                    )
                    emit(bitmap)
                    millSec += millsecPerThumbnail
                }
            }
        }
    }

    suspend fun extractBitmapToFile(
        bitmap: Bitmap,
        type: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 100,
        path: String,
        name: String = System.currentTimeMillis().toString()
    ): Flow<String?> = flow {
        val file = File(path, "${name}.${type.name}")
        val outputStream = FileOutputStream(file)
        bitmap.compress(type, quality, outputStream)
        outputStream.close()
        emit(file.absolutePath)
    }
}