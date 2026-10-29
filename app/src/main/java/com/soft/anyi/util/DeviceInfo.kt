package com.soft.anyi.util

import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.soft.anyi.SoftApp

/**
 * 设备信息（对应 DeviceInfoUtil）：型号 / 系统版本，随请求头带给服务端，
 * 后台操作日志展示「手机型号 / 系统版本」。
 */
object DeviceInfo {
    var model: String = ""
        private set
    var os: String = ""
        private set

    fun init(ctx: android.content.Context) {
        try {
            model = "${Build.BRAND} ${Build.MODEL}".trim()
            os = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        } catch (_: Exception) {
        }
        if (model.isBlank()) model = "未知设备"
        if (os.isBlank()) os = "Android"
    }

    val headers: Map<String, String>
        get() = buildMap {
            if (model.isNotEmpty()) put("X-Device-Model", model)
            if (os.isNotEmpty()) put("X-Os-Version", os)
        }

    fun screenWidthPx(): Int {
        val wm = SoftApp.ctx().getSystemService(WindowManager::class.java) ?: return 1080
        val m = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultMetrics?.let { m.setTo(it) }
        return m.widthPixels
    }
}
