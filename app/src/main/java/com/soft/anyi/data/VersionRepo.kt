package com.soft.anyi.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 版本检查（对应 SoftService.checkVersion）：
 * /api/softlib/version/index → {code:1,data:{version,down_url,forced,enable_switch,...}}
 */
object VersionRepo {
    data class VersionInfo(
        val version: String = "",
        val url: String = "",
        val forced: Boolean = false,
        val desc: String = ""
    )

    suspend fun check(current: String): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val data = Http.get("/api/softlib/version/index")
            // 服务端 ORDER BY id DESC 取最新一条（Flutter 版是「取最旧」bug，这里修正为取最新）
            val v = data.optJSONObject("version")
                ?: (data.opt("data") as? JSONObject)
                ?: data
            val remote = v.optString("version", "").ifEmpty { v.optString("version_name") }
            val url = v.optString("dow_url", "").ifEmpty { v.optString("down_url").ifEmpty { v.optString("url") } }
            if (remote.isEmpty() || url.isEmpty()) return@withContext null
            if (!isNewer(current, remote)) return@withContext null
            VersionInfo(
                version = remote,
                url = url,
                forced = v.opt("forced") == 1 || v.optBoolean("forced_switch", false),
                desc = v.optString("desc", "")
            )
        } catch (e: Exception) {
            Log.w("Softlib.Ver", "checkVersion: ${e.message}")
            null
        }
    }

    /** 三段版本号比较：a > b ? */
    fun isNewer(a: String, b: String): Boolean {
        fun segs(s: String) = s.split('.').map { it.trim().toIntOrNull() ?: 0 }
        val sa = segs(a); val sb = segs(b)
        for (i in 0 until maxOf(sa.size, sb.size)) {
            val x = sa.getOrElse(i) { 0 }; val y = sb.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
