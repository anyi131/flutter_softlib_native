package com.soft.anyi.data

import android.content.Context
import com.soft.anyi.SoftApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 用户仓库（对应 Flutter 版 UserService 的业务方法）。
 * 登录/注册/找回/资料/签到/积分/邀请/卡密/兑换。
 */
object UserRepo {

    private fun saveFrom(data: JSONObject) {
        val token = data.optString("token")
        val userRaw = data.optJSONObject("user") ?: data.optJSONObject("info") ?: data
        if (token.isNotEmpty()) {
            Session.save(token, UserInfo.fromJson(userRaw))
        }
    }

    /** 登录（账号=邮箱或用户名） */
    suspend fun login(account: String, password: String): UserInfo = withContext(Dispatchers.IO) {
        val d = Http.post(
            "/api/softlib/user/login",
            mapOf("account" to account, "email" to account, "password" to password),
            auth = false
        )
        saveFrom(d)
        Session.user() ?: throw Http.ApiException(-1, "登录返回缺少用户信息")
    }

    /** QQ SDK 登录（openid + accessToken 换本站 token） */
    suspend fun qqSdkLogin(openid: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        val d = Http.post(
            "/api/softlib/user/qq_sdk_login",
            mapOf("openid" to openid, "access_token" to accessToken),
            auth = false
        )
        val token = d.optString("token")
        if (token.isEmpty()) return@withContext false
        Session.save(token, UserInfo.fromJson(d.optJSONObject("user") ?: d))
        true
    }

    suspend fun sendCode(email: String, scene: String = "register") = withContext(Dispatchers.IO) {
        Http.post("/api/softlib/user/send_code", mapOf("email" to email, "scene" to scene), auth = false)
    }

    suspend fun register(
        email: String, code: String, password: String,
        nickname: String = "", qq: String = "", invite: String = "",
        bindOpenid: String = "", bindToken: String = ""
    ) = withContext(Dispatchers.IO) {
        val d = Http.post(
            "/api/softlib/user/register",
            buildMap {
                put("email", email); put("code", code); put("password", password)
                if (nickname.isNotEmpty()) put("nickname", nickname)
                if (qq.isNotEmpty()) put("qq", qq)
                if (invite.isNotEmpty()) put("invite", invite)
                if (bindOpenid.isNotEmpty()) { put("bind_openid", bindOpenid); put("bind_token", bindToken) }
            },
            auth = false
        )
        saveFrom(d)
    }

    suspend fun resetPassword(email: String, code: String, password: String) = withContext(Dispatchers.IO) {
        Http.post(
            "/api/softlib/user/reset",
            mapOf("email" to email, "code" to code, "password" to password),
            auth = false
        )
    }

    suspend fun profile(): UserInfo = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/user/profile")
        val u = UserInfo.fromJson(d.optJSONObject("user") ?: d.optJSONObject("info") ?: d)
        Session.updateUser(u)
        u
    }

    suspend fun updateProfile(fields: Map<String, Any?>): UserInfo = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/user/update", fields)
        profile()
    }

    /** 签到；返回成功文案（含奖励） */
    suspend fun sign(): String = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/user/sign")
        d.optString("msg", "签到成功").ifEmpty { "签到成功" } +
            (if (d.has("score")) " +${d.optInt("score")}积分" else "")
    }

    suspend fun qqBound(): Boolean = withContext(Dispatchers.IO) {
        try {
            val d = Http.post("/api/softlib/user/qq_status")
            d.optInt("bound", 0) == 1 || d.optBoolean("bound")
        } catch (_: Exception) { false }
    }

    suspend fun bindQQ(openid: String, accessToken: String) = withContext(Dispatchers.IO) {
        Http.post(
            "/api/softlib/user/bind_qq",
            mapOf("openid" to openid, "access_token" to accessToken)
        )
    }

    suspend fun unbindQQ() = withContext(Dispatchers.IO) {
        Http.post("/api/softlib/user/unbind_qq")
    }

    /** 已绑定 QQ 的老账号补昵称/头像（只覆盖「QQ用户\d+」默认昵称） */
    suspend fun refreshQQProfile(openid: String, accessToken: String): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            try {
                val d = Http.post(
                    "/api/softlib/user/qq_refresh_profile",
                    mapOf("openid" to openid, "access_token" to accessToken)
                )
                val nick = d.optString("nickname")
                val face = d.optString("avatar")
                if (nick.isEmpty()) return@withContext null
                profile()
                Pair(nick, face)
            } catch (_: Exception) { null }
        }

    // ── 积分商城 / 卡密 ─────────────────────────────────────────

    data class Goods(val id: Int, val name: String, val price: Int, val stock: Int) {
        companion object {
            fun listFrom(d: JSONObject): List<Goods> {
                val arr = d.optJSONArray("goods") ?: d.optJSONArray("list") ?: return emptyList()
                return (0 until arr.length()).mapNotNull { n ->
                    val o = arr.optJSONObject(n) ?: return@mapNotNull null
                    Goods(o.optInt("id"), o.optString("name"), o.optInt("score", o.optInt("price")), o.optInt("stock"))
                }
            }
        }
    }

    suspend fun exchangeGoods(): List<Goods> = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/user/exchange_goods")
        Goods.listFrom(d)
    }

    suspend fun exchange(goods: String): String = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/user/exchange", mapOf("goods" to goods))
        d.optString("msg", "兑换成功")
    }

    suspend fun redeem(code: String): String = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/user/redeem", mapOf("code" to code))
        d.optString("msg", "兑换成功")
    }

    data class Card(val code: String, val name: String, val days: Int, val used: Boolean) {
        companion object {
            fun listFrom(d: JSONObject): List<Card> {
                val arr = d.optJSONArray("cards") ?: d.optJSONArray("list") ?: return emptyList()
                return (0 until arr.length()).mapNotNull { n ->
                    val o = arr.optJSONObject(n) ?: return@mapNotNull null
                    Card(
                        o.optString("code"), o.optString("name", o.optString("card_name")),
                        o.optInt("days"), o.optInt("is_used", o.optInt("used", 0)) == 1, o
                    )
                }
            }
        }
    }

    suspend fun cardShop(): Pair<List<Card>, List<Goods>> = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/user/card_shop")
        Pair(Card.listFrom(d), Goods.listFrom(d))
    }

    suspend fun cardBuy(cardId: Int): String = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/user/card_buy", mapOf("card_id" to cardId))
        d.optString("msg", "购买成功")
    }

    // ── 邀请 / 下载记录 ─────────────────────────────────────────

    data class InviteStats(val invites: Int, val score: Int, val code: String) {
        companion object {
            fun from(d: JSONObject) = InviteStats(
                d.optInt("invites", d.optInt("count")),
                d.optInt("score"),
                d.optString("invite_code").ifEmpty { Session.user()?.inviteCode ?: "" }
            )
        }
    }

    suspend fun inviteStats(): InviteStats = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/user/invite_stats")
        InviteStats.from(d)
    }

    /** 下载记录上报（我的-下载记录后台可见） */
    fun downloadLog(appName: String, appId: Int = 0) {
        if (!Session.isLoggedIn()) return
        val body = buildMap<String, Any> {
            put("name", appName)
            if (appId > 0) put("app_id", appId)
        }
        Http.postAsync("/api/softlib/user/download_log", body) { _, _ -> }
    }

    /** 捐赠榜 */
    suspend fun donateRank(): List<JSONObject> = withContext(Dispatchers.IO) {
        val d = Http.get("/api/softlib/user/donate_rank")
        val arr = d.optJSONArray("list") ?: d.optJSONArray("rank") ?: return@withContext emptyList()
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    /** 保存用户自定义开屏图 */
    suspend fun saveSplash(url: String) = withContext(Dispatchers.IO) {
        Http.post("/api/softlib/user/save_splash", mapOf("url" to url))
    }

    /** 设置自定义称号 */
    suspend fun setTitle(title: String) = withContext(Dispatchers.IO) {
        Http.post("/api/softlib/user/set_title", mapOf("title" to title))
    }
}
