package com.dabenxiang.jys.view.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dabenxiang.jys.R
import com.dabenxiang.jys.REQUEST_CODE_CHOOSE
import com.dabenxiang.jys.view.exchange.chat.ChatContentFragment
import com.zhihu.matisse.Matisse

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)
        supportFragmentManager.beginTransaction().let { ft ->
            ft.replace(R.id.container, ChatContentFragment())
        }.run {
            this.commitNow()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            Log.e("OnActivityResult ", Matisse.obtainOriginalState(data).toString())

        }
    }
}