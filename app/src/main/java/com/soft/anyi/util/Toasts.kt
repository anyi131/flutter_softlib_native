package com.soft.anyi.util

import android.widget.Toast
import com.soft.anyi.SoftApp

/** 轻提示（对应 ToastUtil） */
object Toasts {
    private var last: Toast? = null
    private var lastMsg = ""
    private var lastAt = 0L

    @Synchronized
    private fun show(msg: String, err: Boolean) {
        val now = System.currentTimeMillis()
        if (msg == lastMsg && now - lastAt < 1500) return // 1.5s 去抖
        lastMsg = msg; lastAt = now
        last?.cancel()
        last = Toast.makeText(SoftApp.ctx(), msg, if (err) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
        last?.show()
    }

    fun s(msg: String) = show(msg, false)
    fun e(msg: String) = show(msg, true)
}
