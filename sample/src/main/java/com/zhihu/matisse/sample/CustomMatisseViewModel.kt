package com.zhihu.matisse.sample

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihu.matisse.Matisse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CustomMatisseViewModel : ViewModel() {
    private val _results = MutableLiveData<List<Uri>>()
    val results: LiveData<List<Uri>> = _results

    private var _resultFlow: MutableStateFlow<List<String>> = MutableStateFlow(mutableListOf())
    val resultFlow: StateFlow<List<String>> = _resultFlow

    private var _videoTypeFramesresultFlow: MutableStateFlow<List<String>> =
        MutableStateFlow(mutableListOf())
    val videoTypeFramesresultFlow: StateFlow<List<String>> = _videoTypeFramesresultFlow

    fun process(data: Intent) {
        Matisse.apply {
            obtainResult(data)?.let { its ->
                _results.value = its
            }
            obtainPathResult(data)?.let { pathResult ->
                _resultFlow.value = pathResult
            }
        }
    }

    fun processVideoFrames(
        save_path: String,
        sources: List<String>,
        save_image_type: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 100,
        per_sec: Long = 5000L
    ) {
        val extractor = VideoFrameExtractor.getExtractor()
        for (source in sources) {
            viewModelScope.launch {
                extractor.filepaths.collect {
                    Log.e("CustomMatisseViewModel", "Show get extract path : $it")
                }
                extractor.extractFramesToFile(save_path, source, per_sec, save_image_type, quality)
            }
        }
    }
}