package com.zhihu.matisse.sample

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.zhihu.matisse.Matisse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CustomMatisseViewModel : ViewModel() {
    private val _results = MutableLiveData<List<Uri>>()
    val results: LiveData<List<Uri>> = _results

    private var _resultFlow: MutableStateFlow<List<String>> = MutableStateFlow(mutableListOf())
    val resultFlow: StateFlow<List<String>> = _resultFlow

    fun process(data: Intent) {
        Matisse.apply {
            obtainResult(data)?.let { its ->
                _results.value = its
            }
            obtainPathResult(data)?.let { pathResult ->
                _resultFlow.value=pathResult
            }
        }
    }
}