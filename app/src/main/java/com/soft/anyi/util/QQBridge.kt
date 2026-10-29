package com.soft.anyi.util

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

import com.tencent.connect.common.Constants
import com.tencent.tauth.IUiListener
import com.tencent.tauth.Tencent
import com.tencent.tauth.UiError

import org.json.JSONObject

/**
 * QQ 登录原生桥（自写，不用第三方插件 —— 第三方把 AGP 写死不兼容）
 *
 * 用法：
 *   QQBridge.appId = cfg.qqAppId
 *   QQBridge.preheat(activity)               // 进登录页时预热（可选）
 *   QQBridge.login(activity) { res -> ... }  // res=null 表示用户取消；抛异常=失败
 */
object QQBridge {

    data class AuthResult(val openid: String, val accessToken: String, val uiMs: Long, val initMs: Long)

    var appId: String = ""

    private var activityRef: java.lang.ref.WeakReference<Activity>? = null
    private var callback: ((AuthResult?) -> Unit)? = null
    private var tencent: Tencent? = null
    private var initCostMs = -1L
    private var loginStartAt = 0L

    /** 进登录页时调用：提前建 Tencent 实例 + 声明设备信息授权，点登录更快 */
    fun preheat(activity: Activity) {
        if (appId.isEmpty()) return
        try {
            tencent = Tencent.createInstance(appId, activity.applicationContext)
            grantPermission()
        } catch (e: Throwable) {
            Log.w("QQBridge", "preheat fail: ${e.message}")
        }
    }

    fun isQQInstalled(activity: Activity): Boolean {
        val pm = activity.packageManager
        return try {
            pm.getPackageInfo("com.tencent.mobileqq", 0); true
        } catch (e: Throwable) {
            try { pm.getPackageInfo("com.tencent.mobileqqi", 0); true } catch (e2: Throwable) { false }
        }
    }

    /**
     * 发起 QQ 登录。
     * onResult(res)：res != null 成功；null = 用户取消。
     * 失败时 onResult 回调 null 前先 Toast 由调用方处理 —— 这里直接抛异常不方便跨回调，
     * 约定：onResult(null) 后查 lastError 拿失败原因。
     */
    var lastError: String = ""

    fun login(activity: Activity, onResult: (AuthResult?) -> Unit) {
        if (appId.isEmpty()) { lastError = "QQ AppID 未配置（后台缺少 AppID）"; onResult(null); return }
        if (callback != null) { lastError = "上一次登录尚未完成"; onResult(null); return }
        activityRef = java.lang.ref.WeakReference(activity)
        callback = onResult
        lastError = ""
        try {
            val t0 = System.currentTimeMillis()
            tencent = Tencent.createInstance(appId, activity.applicationContext)
            grantPermission() // ★ SDK≥3.5.7 不调这个会被 -6「用户未授权」拒绝
            initCostMs = System.currentTimeMillis() - t0
            loginStartAt = System.currentTimeMillis()
            tencent?.login(activity, "get_user_info", listener)
        } catch (e: Throwable) {
            callback = null
            lastError = "QQ 登录失败：${e.message}"
            onResult(null)
        }
    }

    /** 由 Activity.onActivityResult 转发进来 */
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == Constants.REQUEST_LOGIN) {
            Tencent.onActivityResultData(requestCode, resultCode, data, listener)
            return true
        }
        return false
    }

    fun logout(activity: Activity) {
        try { tencent?.logout(activity) } catch (_: Throwable) {}
    }

    /** ★ SDK 3.5.7+ 必须声明「已获设备信息授权」，反射兼容不同小版本签名 */
    private fun grantPermission() {
        val model = Build.MODEL ?: ""
        try {
            val m = Tencent::class.java.getMethod("setIsPermissionGranted", Boolean::class.javaPrimitiveType, String::class.java)
            val target = if (java.lang.reflect.Modifier.isStatic(m.modifiers)) null else tencent
            m.invoke(target, true, model)
            return
        } catch (_: Throwable) {}
        try {
            val m = Tencent::class.java.getMethod("setIsPermissionGranted", Boolean::class.javaPrimitiveType)
            val target = if (java.lang.reflect.Modifier.isStatic(m.modifiers)) null else tencent
            m.invoke(target, true)
            return
        } catch (_: Throwable) {}
        Log.w("QQBridge", "本 SDK 没有 setIsPermissionGranted，跳过")
    }

    private val listener = object : IUiListener {
        override fun onComplete(response: Any?) {
            val cb = callback; callback = null
            if (cb == null) return
            try {
                val jo = response as JSONObject
                val openid = jo.optString("openid", "")
                val token = jo.optString("access_token", "")
                if (openid.isEmpty() || token.isEmpty()) {
                    lastError = "QQ 未返回 openid/token"
                    cb(null)
                } else {
                    val uiMs = if (loginStartAt > 0) System.currentTimeMillis() - loginStartAt else -1
                    cb(AuthResult(openid, token, uiMs, initCostMs))
                }
            } catch (e: Throwable) {
                lastError = "QQ 返回解析失败：${e.message}"
                cb(null)
            }
        }

        override fun onError(e: UiError?) {
            val cb = callback; callback = null
            val msg = e?.errorMessage ?: "unknown"
            val detail = e?.errorDetail ?: ""
            val code = e?.errorCode ?: -1
            Log.e("QQBridge", "授权失败 code=$code msg=$msg detail=$detail")
            lastError = "QQ 授权失败 code=$code $msg"
            cb?.invoke(null)
        }

        override fun onCancel() {
            val cb = callback; callback = null
            lastError = ""
            cb?.invoke(null)
        }

        override fun onWarning(code: Int) {}
    }
}
