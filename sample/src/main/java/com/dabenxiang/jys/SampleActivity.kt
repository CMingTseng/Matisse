package com.dabenxiang.jys

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager

import com.tbruyelle.rxpermissions2.RxPermissions
import com.zhihu.matisse.BaseMatisse
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import com.zhihu.matisse.filter.Filter
import com.zhihu.matisse.filter.GifSizeFilter

import kotlinx.android.synthetic.main.activity_main.*


class SampleActivity : AppCompatActivity(), View.OnClickListener {
    private var mAdapter: UriAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        zhihu.setOnClickListener(this)
        dracula.setOnClickListener(this)
        recyclerview.layoutManager = LinearLayoutManager(this)
        recyclerview.adapter = UriAdapter().also {
            mAdapter = it
        }
    }

    override fun onClick(v: View?) {
        val rxPermissions = RxPermissions(this)
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe({ aBoolean: Boolean ->
                    if (aBoolean) {
                        v?.let {
                            startAction(it)
                        }
                    } else {
                        Toast.makeText(this, R.string.permission_request_denied, Toast.LENGTH_LONG)
                                .show()
                    }
                }) { obj: Throwable -> obj.printStackTrace() }
    }

    private fun startAction(v: View) {
        when (v.id) {
            R.id.dracula -> Matisse.from(this)
                    .choose(MimeType.ofImage())
                    .theme(R.style.Matisse_Dracula)
                    .countable(false)
                    .addFilter(GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                    .maxSelectable(3)
                    .originalEnable(true)
                    .maxOriginalSize(3)// ?MB
                    .imageEngine(GlideEngine())
                    .forResult(REQUEST_CODE_CHOOSE)
            else -> {
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            mAdapter!!.setData(Matisse.obtainResult(data), Matisse.obtainPathResult(data))
            Log.e("OnActivityResult ", Matisse.obtainOriginalState(data).toString())
        }
    }
}