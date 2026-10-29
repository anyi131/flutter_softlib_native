package com.soft.anyi.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 工具箱仓库（对应 ToolboxService / jzs 系列接口 + media 影音接口）。
 */
object ToolboxRepo {
    suspend fun config(): JSONObject? = withContext(Dispatchers.IO) {
        try { Http.get("/api/softlib/jzs/config") } catch (_: Exception) { null }
    }

    suspend fun tools(): List<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val d = Http.get("/api/softlib/tool/index")
            val arr = d.optJSONArray("list") ?: d.optJSONArray("data")
                ?: (d.opt("data") as? org.json.JSONArray) ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun search(kw: String): List<JSONObject> = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/jzs/search", mapOf("kw" to kw))
        val arr = d.optJSONArray("list") ?: return@withContext emptyList()
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    suspend fun toggleFav(toolId: Int): Boolean = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/jzs/fav", mapOf("tool_id" to toolId))
        d.optBoolean("fav", d.optInt("status", 1) == 1)
    }

    suspend fun favList(): List<JSONObject> = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/jzs/fav_list")
        val arr = d.optJSONArray("list") ?: return@withContext emptyList()
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    suspend fun addHistory(toolId: Int) = withContext(Dispatchers.IO) {
        try { Http.post("/api/softlib/jzs/history", mapOf("tool_id" to toolId)) } catch (_: Exception) {}
    }

    suspend fun history(): List<JSONObject> = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/jzs/history_list")
        val arr = d.optJSONArray("list") ?: return@withContext emptyList()
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    /** toolbox1 系列（poem/hitokoto/unit_table/daily60 等单一数据源工具） */
    suspend fun box1(name: String): JSONObject? = withContext(Dispatchers.IO) {
        try { Http.post("/api/softlib/toolbox", mapOf("name" to name)) } catch (_: Exception) { null }
    }

    /** toolbox2（带参数工具：translate/exchange/ip 等） */
    suspend fun box2(name: String, params: Map<String, Any?> = emptyMap()): JSONObject? =
        withContext(Dispatchers.IO) {
            try {
                Http.post(
                    "/api/softlib/toolbox",
                    buildMap { put("name", name); putAll(params) }
                )
            } catch (_: Exception) { null }
        }

    /** media 系列（movie_home / movie_search / music_url / drama_list ...） */
    suspend fun media(op: String, params: Map<String, Any?> = emptyMap()): JSONObject? =
        withContext(Dispatchers.IO) {
            try {
                Http.post(
                    "/api/softlib/media/$op",
                    buildMap { put("op", op); putAll(params) }
                )
            } catch (_: Exception) { null }
        }

    /** 外链工具代理（服务端代抓，绕跨域/防盗链） */
    suspend fun proxy(toolId: Int, params: Map<String, Any?> = emptyMap()): JSONObject? =
        withContext(Dispatchers.IO) {
            try {
                Http.post(
                    "/api/softlib/toolbox/proxy",
                    buildMap { put("tool_id", toolId); putAll(params) }
                )
            } catch (_: Exception) { null }
        }
}
