package com.zhihu.matisse.internal.utils;

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri

/**
 * @author 工藤
 * @email gougou@16fan.com
 * create at 2018年10月23日12:17:59
 * description:媒体扫描
 */
class SingleMediaScanner(context: Context, mPath: String, mListener: ScanListener) : MediaScannerConnection.MediaScannerConnectionClient {
    private var mMsc: MediaScannerConnection? = null
    private var mPath: String? = null
    private var mListener: ScanListener? = null

    interface ScanListener {
        /**
         * scan finish
         */
        fun onScanFinish()
    }

    fun SingleMediaScanner(context: Context, mPath: String, mListener: ScanListener) {
        this.mPath = mPath
        this.mListener = mListener
        mMsc = MediaScannerConnection(context, this)
        mMsc!!.connect()
    }

    override fun onMediaScannerConnected() {
        mMsc!!.scanFile(mPath, null)
    }

    override fun onScanCompleted(mPath: String, mUri: Uri) {
        mMsc!!.disconnect()
        if (mListener != null) {
            mListener!!.onScanFinish()
        }
    }
}
