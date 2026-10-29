package com.soft.anyi.util

import android.content.Context

/** QQ 授权本地缓存（30 天）+ 待绑定凭据 —— 对应 Flutter 版 QQLogin 的缓存层 */
object QQAuthCache {
    private const val K_OID = "qq_cache_openid"
    private const val K_TOK = "qq_cache_token"
    private const val K_AT = "qq_cache_at"
    private const val DAYS = 30L

    /** ★ QQ 未绑定账号时暂存，注册请求带上即自动绑定 */
    var pendingOpenid: String = ""
    var pendingToken: String = ""
    val hasPending: Boolean get() = pendingOpenid.isNotEmpty() && pendingToken.isNotEmpty()
    fun clearPending() { pendingOpenid = ""; pendingToken = "" }

    fun save(openid: String, token: String) {
        if (openid.isEmpty() || token.isEmpty()) return
        val sp = ctx()?.getSharedPreferences("qq_auth", Context.MODE_PRIVATE) ?: return
        sp.edit().putString(K_OID, openid).putString(K_TOK, token)
            .putLong(K_AT, System.currentTimeMillis()).apply()
    }

    fun cached(): com.soft.anyi.util.QQBridge.AuthResult? {
        val sp = ctx()?.getSharedPreferences("qq_auth", Context.MODE_PRIVATE) ?: return null
        val oid = sp.getString(K_OID, "") ?: ""
        val tok = sp.getString(K_TOK, "") ?: ""
        val at = sp.getLong(K_AT, 0)
        if (oid.isEmpty() || tok.isEmpty() || at <= 0) return null
        val days = (System.currentTimeMillis() - at) / 86400000L
        if (days >= DAYS) return null
        return com.soft.anyi.util.QQBridge.AuthResult(oid, tok, -1, -1)
    }

    fun clear() {
        val sp = ctx()?.getSharedPreferences("qq_auth", Context.MODE_PRIVATE) ?: return
        sp.edit().remove(K_OID).remove(K_TOK).remove(K_AT).apply()
    }

    private var appCtx: Context? = null
    fun init(c: Context) { appCtx = c.applicationContext }
    private fun ctx(): Context? = appCtx
}
