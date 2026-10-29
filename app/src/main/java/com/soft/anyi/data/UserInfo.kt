package com.soft.anyi.data

import org.json.JSONObject

/**
 * 用户信息模型（字段对准服务端 user 接口，兼容数字/字符串/布尔）。
 */
data class UserInfo(
    val id: Int = 0,
    val username: String = "",
    val nickname: String = "",
    val email: String = "",
    val avatar: String = "",
    val qq: String = "",
    val gender: Int = 0,
    val bio: String = "",
    val score: Int = 0,
    val money: String = "0",
    val isVip: Boolean = false,
    val vipExpire: String = "",
    val inviteCode: String = "",
    val jointime: String = "",
    val isAdmin: Boolean = false,
    val title: String = "",
    val splashImage: String = "",
    val signedToday: Boolean = false,
    val raw: String
) {
    companion object {
        private fun b(v: Any?) = v == true || v == 1 || v == "1" || v == "true"
        private fun i(v: Any?) = v?.toString()?.toIntOrNull() ?: 0
        private fun s(v: Any?) = v?.toString() ?: ""

        fun fromJson(j: JSONObject): UserInfo = UserInfo(
            id = i(j.opt("id")),
            username = s(j.opt("username")),
            nickname = s(j.opt("nickname")),
            email = s(j.opt("email")),
            avatar = s(j.opt("avatar")),
            qq = s(j.opt("qq")),
            gender = i(j.opt("gender")),
            bio = s(j.opt("bio")),
            score = i(j.opt("score")),
            money = s(j.opt("money")),
            // ★ 兼容 1 / "1" / true（Flutter 版 1014 修复的同款坑）
            isVip = b(j.opt("is_vip")),
            vipExpire = s(j.opt("vip_expire")),
            inviteCode = s(j.opt("invite_code")),
            jointime = s(j.opt("jointime")),
            isAdmin = b(j.opt("is_admin")),
            title = s(j.opt("title")),
            splashImage = s(j.opt("splash_image")),
            signedToday = if (j.has("signed_today")) b(j.opt("signed_today")) else false,
            raw = j.toString()
        )
    }
}
