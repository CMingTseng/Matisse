package com.zhihu.matisse.internal.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri

class SingleMediaScanner(
    context: Context,
    private val path: String,
    private val listener: ScanListener?
) : MediaScannerConnectionClient {

    private val msc: MediaScannerConnection = MediaScannerConnection(context, this)

    init {
        msc.connect()
    }

    override fun onMediaScannerConnected() {
        msc.scanFile(path, null)
    }

    override fun onScanCompleted(path: String, uri: Uri) {
        msc.disconnect()
        listener?.onScanFinish()
    }

    interface ScanListener {
        /**
         * scan finish
         */
        fun onScanFinish()
    }
}