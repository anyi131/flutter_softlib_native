package com.soft.anyi.data

import android.content.Context
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 软件仓库（对应 SoftService 的 app 部分）：
 * 列表（内存缓存 + dv 数据版本 + X-Data-Cached 304 语义）/ 分类 / 详情 / 浏览计数。
 */
object SoftRepo {
    private data class Entry(val data: Any, val at: Long)
    private const val TTL = 10 * 60 * 1000L

    private val cache = HashMap<String, Entry>()
    private var dataVersion = ""

    /** 下拉刷新清缓存 */
    @Synchronized
    fun clearCache() {
        cache.clear()
        dataVersion = ""
    }

    private fun ctx() = com.soft.anyi.SoftApp.ctx()

    @Synchronized
    private fun <T> hit(key: String): T? {
        val cfg = ConfigRepo.cached(ctx())
        val on = cfg?.featureApiCache ?: true
        if (!on) return null
        val e = cache[key] ?: return null
        @Suppress("UNCHECKED_CAST")
        return if (System.currentTimeMillis() - e.at < TTL) e.data as T else null
    }

    @Synchronized
    private fun put(key: String, data: Any) {
        cache[key] = Entry(data, System.currentTimeMillis())
    }

    /** 软件列表（缓存 + dv 版本协商） */
    suspend fun apps(catId: Int = 0, keyword: String = "", force: Boolean = false): List<AppItem> =
        withContext(Dispatchers.IO) {
            val key = "apps_${catId}_$keyword"
            if (!force) hit<List<AppItem>>(key)?.let { return@withContext it }

            val q = buildMap {
                if (catId > 0) put("cat_id", catId.toString())
                if (keyword.isNotBlank()) put("keyword", keyword.trim())
                if (dataVersion.isNotEmpty()) put("dv", dataVersion)
            }
            // get() 不透传响应头，这里直接走原始请求拿 X-Data-Version / X-Data-Cached
            val url = ApiHost.BASE + "/api/softlib/app/index" + q.entries.joinToString("&", "?") {
                "${it.key}=${java.net.URLEncoder.encode(it.value, "UTF-8")}"
            }
            val rb = okhttp3.Request.Builder().url(url).get()
            Http.commonHeaders().forEach { (k, v) -> rb.header(k, v) }
            Http.client.newCall(rb.build()).execute().use { r ->
                val dv = r.header("x-data-version") ?: ""
                val cachedFlag = r.header("x-data-cached") == "1"
                if (dv.isNotEmpty()) dataVersion = dv

                val body = r.body?.string() ?: "{}"
                val j = JSONObject(body)
                if (j.optInt("code") == 1) {
                    if (cachedFlag && !force) {
                        hit<List<AppItem>>(key)?.let { return@use it }
                        // 没有缓存 → 按正常数据解析，绝不递归（Flutter 版 1020 修复）
                    }
                    val list = AppItem.listFrom(j.opt("data"))
                    put(key, list)
                    return@use list
                }
                throw Http.ApiException(j.optInt("code"), j.optString("msg", "获取软件列表失败"))
            } ?: emptyList()
        }

    /** 分类 */
    suspend fun cats(force: Boolean = false): List<AppCat> = withContext(Dispatchers.IO) {
        if (!force) hit<List<AppCat>>("cats")?.let { return@withContext it }
        val data = Http.get("/api/softlib/app/cats")
        val list = AppCat.listFrom(data.opt("data") ?: data)
        put("cats", list)
        list
    }

    /** 详情（id 或 url 二选一） */
    suspend fun detail(id: Int = 0, url: String = ""): AppItem = withContext(Dispatchers.IO) {
        val data = Http.post(
            "/api/softlib/app/detail",
            buildMap {
                if (id > 0) put("id", id)
                if (url.isNotBlank()) put("url", url)
            }
        )
        val o = data.optJSONObject("app") ?: data.optJSONObject("info") ?: data
        AppItem.fromJson(o)
    }

    /** 浏览计数 */
    fun addView(id: Int) {
        Http.postAsync("/api/softlib/app/view", mapOf("id" to id)) { _, _ -> }
    }

    /** 蓝奏云文件夹列表（无数据库记录的 from_folder 条目） */
    suspend fun folder(url: String): List<AppItem> = withContext(Dispatchers.IO) {
        val data = Http.post("/api/softlib/app/folder", mapOf("url" to url))
        AppItem.listFrom(data.opt("data") ?: data.opt("list") ?: data)
    }

    /** 下载结果上报（后台「下载记录」） */
    fun reportDownload(appId: Int, ok: Boolean, reason: String = "") {
        val form = buildMap<String, Any?> {
            put("app_id", appId)
            put("ok", ok)
            if (reason.isNotEmpty()) put("reason", reason)
        }
        Http.postAsync("/api/softlib/app/download_result", form) { _, _ -> }
    }

    // ── 评论（对应 ReviewService，CommentsActivity 调用名） ──
    suspend fun reviews(appId: Int, page: Int = 1): List<ReviewRepo.Review> = ReviewRepo.list(appId, page)
    suspend fun reviewCreate(appId: Int, score: Int = 5, content: String) = ReviewRepo.create(appId, score, content)
    suspend fun reviewLike(id: Int) = ReviewRepo.like(id)
    suspend fun reviewReply(appId: Int, id: Int, content: String) = ReviewRepo.reply(id, content)

    /** 关键字搜索（对应 soft_search 接口） */
    suspend fun search(kw: String, limit: Int = 30): List<AppItem> {
        if (kw.isBlank()) return emptyList()
        val d = Http.post("/api/softlib/app/search", mapOf("keyword" to kw, "limit" to limit))
        val arr = d.optJSONArray("list") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map { AppItem.fromJson(it) }
    }
}
