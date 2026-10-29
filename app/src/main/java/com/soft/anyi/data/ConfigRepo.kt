package com.soft.anyi.data

import android.content.Context
import com.soft.anyi.util.Toasts
import org.json.JSONObject

/**
 * 远程配置仓库（对应 SoftService.fetchConfig）：
 * 磁盘缓存优先 → 网络刷新；force 时只走网络。
 */
object ConfigRepo {
    private const val SP = "config"
    private const val KEY = "cfg_json"
    private var inFlight = false

    fun cached(ctx: Context): AppConfig? {
        val raw = ctx.getSharedPreferences(SP, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        return try { AppConfig.fromJson(JSONObject(raw)) } catch (_: Exception) { null }
    }

    fun saveCache(ctx: Context, cfg: AppConfig) {
        // AppConfig 不存原始 JSON，从网络响应直接存（见 fetch）
    }

    fun saveCacheRaw(ctx: Context, raw: JSONObject) {
        ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .edit().putString(KEY, raw.toString()).apply()
    }

    /** 先回缓存再刷新网络；返回磁盘缓存（可能为 null） */
    suspend fun fetchCachedFirst(ctx: Context): AppConfig? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val cached = cached(ctx)
        try {
            val data = Http.get("/api/softlib/config/index")
            saveCacheRaw(ctx, data)
        } catch (e: Exception) {
            // 网络失败静默，用缓存兜底
        }
        cached(ctx) ?: cached(ctx) // 再次读（网络成功后已是新数据）
    }

    /** 强制刷新（维护页「重新加载」/下拉刷新） */
    suspend fun fetchForce(ctx: Context): AppConfig? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val data = Http.get("/api/softlib/config/index")
            saveCacheRaw(ctx, data)
            cached(ctx)
        } catch (e: Exception) {
            Toasts.e("配置刷新失败：${e.message}")
            cached(ctx)
        }
    }
}
