package com.zhihu.matisse.listener

import android.net.Uri

interface OnSelectedListener {
    /**
     * @param uriList  the selected item [Uri] list.
     * @param pathList the selected item file path list.
     */
    fun onSelected(uriList: List<Uri>, pathList: List<String>)
}