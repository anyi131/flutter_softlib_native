package com.soft.anyi.data

import android.content.Context
import com.soft.anyi.util.Toasts
import org.json.JSONObject

/**
 * 用户会话（对应 Flutter 版 UserService 的 token/restore/save/logout）。
 * 本地存 token + 用户信息 JSON；请求头自动带 token。
 */
object Session {
    private var sp: android.content.SharedPreferences? = null
    private var token: String? = null
    private var info: UserInfo? = null

    val listeners = mutableListOf<(UserInfo?) -> Unit>()

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("session", Context.MODE_PRIVATE)
        token = sp?.getString("token", null)
        info = sp?.getString("user", null)?.let { raw ->
            try { UserInfo.fromJson(JSONObject(raw)) } catch (_: Exception) { null }
        }
    }

    fun token(): String? = token

    fun user(): UserInfo? = info

    fun isLoggedIn(): Boolean = !token.isNullOrEmpty()

    fun isAdmin(): Boolean = info?.isAdmin == true

    fun save(t: String, u: UserInfo?) {
        token = t
        info = u
        sp?.edit()?.putString("token", t)?.putString("user", u?.raw)?.apply()
        notifyChanged()
    }

    fun updateUser(u: UserInfo?) {
        info = u
        if (u != null) sp?.edit()?.putString("user", u.raw)?.apply()
        notifyChanged()
    }

    fun logout() {
        val t = token
        token = null
        info = null
        sp?.edit()?.clear()?.apply()
        notifyChanged()
        if (!t.isNullOrEmpty()) {
            Http.postAsync("/api/softlib/user/logout", mapOf("token" to t)) { _, _ -> }
        }
    }

    private fun notifyChanged() {
        val u = info
        listeners.forEach { l -> try { l(u) } catch (_: Exception) {} }
    }

    /** 需要登录的操作前检查；未登录提示并返回 false */
    fun requireLogin(): Boolean {
        if (isLoggedIn()) return true
        Toasts.s("请先登录")
        return false
    }
}
