package com.zhihu.matisse.sample

import android.Manifest
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import com.zhihu.matisse.filter.Filter
import com.zhihu.matisse.internal.entity.CaptureStrategy
import com.zhihu.matisse.listener.OnCheckedListener
import com.zhihu.matisse.listener.OnSelectedListener

class MatisseSampleActivity : AppCompatActivity(), View.OnClickListener {
    private var adapter: UriAdapter? = null
    private var matisse: Matisse? = null

    private val captureLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (RESULT_OK == result.resultCode) {
            val data = result.data
            adapter!!.setData(Matisse.obtainResult(data), Matisse.obtainPathResult(data))
            Log.e("On capture  ", java.lang.String.valueOf(Matisse.obtainOriginalState(data)))
        }
    }
    private val pickerLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (RESULT_OK == result.resultCode) {
            val data = result.data
            adapter!!.setData(Matisse.obtainResult(data), Matisse.obtainPathResult(data))
            Log.e("OnActivityResult ", java.lang.String.valueOf(Matisse.obtainOriginalState(data)))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_o)
        findViewById<View>(R.id.zhihu).setOnClickListener(this)
        findViewById<View>(R.id.dracula).setOnClickListener(this)
        findViewById<View>(R.id.only_gif).setOnClickListener(this)
//        findViewById<View>(R.id.capture).setOnClickListener(this)

        val recyclerView = findViewById<View>(R.id.recyclerview) as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = UriAdapter().also { adapter = it }

        matisse = Matisse.from(this@MatisseSampleActivity)
    }

    override fun onClick(v: View) {
        val rxPermissions = RxPermissions(this)
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .subscribe({ aBoolean: Boolean ->
                if (aBoolean) {
                    startAction(v)
                } else {
                    Toast.makeText(
                        this@MatisseSampleActivity,
                        R.string.permission_request_denied,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }) { obj: Throwable -> obj.printStackTrace() }
    }

    private fun startAction(v: View) {
        when (v.id) {
            R.id.capture -> matisse!!.performCapture(
                CaptureStrategy(
                    true,
                    "com.zhihu.matisse.sample.kt.fileprovider",
                    "test"
                ), captureLauncher
            )
            R.id.zhihu -> Matisse.from(this@MatisseSampleActivity)
                .choose(MimeType.ofImage(), false)
                .countable(true)
                .capture(true)
                .captureStrategy(
                    CaptureStrategy(
                        true,
                        "com.zhihu.matisse.sample.kt.fileprovider",
                        "test"
                    )
                )
                .maxSelectable(9)
                .addFilter(GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                .gridExpectedSize(resources.getDimensionPixelSize(R.dimen.grid_expected_size))
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .thumbnailScale(0.85f)
                .imageEngine(GlideEngine())
                .setOnSelectedListener(object : OnSelectedListener {
                    override fun onSelected(uriList: List<Uri>, pathList: List<String>) {
                        Log.e("onSelected", "onSelected: pathList=$pathList")
                    }
                })
                .showSingleMediaType(true)
                .maxOriginalSize(10)
                .autoHideToolbarOnSingleTap(true)
                .setOnCheckedListener(object : OnCheckedListener {
                    override fun onCheck(isChecked: Boolean) {
                        Log.e("isChecked", "onCheck: isChecked=$isChecked")
                    }
                })
                .forResult(pickerLauncher)
            R.id.dracula -> Matisse.from(this@MatisseSampleActivity)
                .choose(MimeType.ofVideo())
                .showSingleMediaType(true)
                .theme(R.style.Matisse_Dracula)
                .countable(false)
                .addFilter(GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                .maxSelectable(9)
                .originalEnable(true)
                .maxOriginalSize(10)
                .imageEngine(GlideEngine())
                .forResult(pickerLauncher)
            R.id.only_gif -> Matisse.from(this@MatisseSampleActivity)
                .choose(MimeType.of(MimeType.GIF), false)
                .countable(true)
                .maxSelectable(9)
                .addFilter(GifSizeFilter(320, 320, 5 * Filter.K * Filter.K))
                .gridExpectedSize(resources.getDimensionPixelSize(R.dimen.grid_expected_size))
                .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                .thumbnailScale(0.85f)
                .imageEngine(GlideEngine())
                .showSingleMediaType(true)
                .originalEnable(true)
                .maxOriginalSize(10)
                .autoHideToolbarOnSingleTap(true)
                .forResult(pickerLauncher)
            else -> {
            }
        }
        adapter!!.setData(null, null)
    }

    private class UriAdapter : RecyclerView.Adapter<UriAdapter.UriViewHolder>() {
        private var mUris: List<Uri>? = null
        private var mPaths: List<String>? = null
        fun setData(uris: List<Uri>?, paths: List<String>?) {
            mUris = uris
            mPaths = paths
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UriViewHolder {
            return UriViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.uri_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: UriViewHolder, position: Int) {
            holder.uri.text = mUris!![position].toString()
            holder.path.text = mPaths!![position]
            holder.uri.alpha = if (position % 2 == 0) 1.0f else 0.54f
            holder.path.alpha = if (position % 2 == 0) 1.0f else 0.54f
        }

        override fun getItemCount(): Int {
            return if (mUris == null) 0 else mUris!!.size
        }

        class UriViewHolder(contentView: View) : ViewHolder(contentView) {
            val uri: TextView = contentView.findViewById<View>(R.id.uri) as TextView
            val path: TextView = contentView.findViewById<View>(R.id.path) as TextView
        }
    }
}