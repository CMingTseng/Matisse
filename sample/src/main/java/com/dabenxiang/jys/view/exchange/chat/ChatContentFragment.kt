package com.dabenxiang.jys.view.exchange.chat

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dabenxiang.jys.R
import com.dabenxiang.jys.REQUEST_CODE_CHOOSE
import com.dabenxiang.jys.UriAdapter
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import com.zhihu.matisse.filter.Filter
import com.zhihu.matisse.filter.GifSizeFilter
import kotlinx.android.synthetic.main.activity_main.*


class ChatContentFragment : Fragment(), View.OnClickListener {
    private var mAdapter: UriAdapter? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dracula.text="開啟相簿"
        dracula.setOnClickListener(this)
        zhihu.visibility=View.GONE
        recyclerview.layoutManager = LinearLayoutManager(requireContext())
        recyclerview.adapter = UriAdapter().also {
            mAdapter = it
        }
    }

    override fun onClick(v: View?) {
        val rxPermissions = RxPermissions(requireActivity())
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe({ aBoolean: Boolean ->
                    if (aBoolean) {
                        v?.let {
                            startAction(it)
                        }
                    } else {
                        Toast.makeText(requireContext(), R.string.permission_request_denied, Toast.LENGTH_LONG)
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
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == AppCompatActivity.RESULT_OK) {
            mAdapter!!.setData(Matisse.obtainResult(data), Matisse.obtainPathResult(data))
        }
    }
}