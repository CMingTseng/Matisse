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
            extractFrames(videoUrl, per_sec).flatMapMerge { bitmap ->
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
            }.collect { filepath ->
                _filepaths.emit(filepath)
            }
        }
    }

    suspend fun extractFrames(videoUrl: String, per_sec: Long = 5000L): Flow<Bitmap?> = flow {
        Log.e("VideoFrameExtractor", "Show extractFrames")
        MediaMetadataRetriever().apply {
            setDataSource(videoUrl)
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                val videoDuration = it.toLong()
                for (time in 0L until videoDuration step per_sec) {
                    val bitmap = getFrameAtTime(time * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                    emit(bitmap)
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