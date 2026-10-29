package com.soft.anyi.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 消息仓库（对应 MessageService：list/wait/unread/read/remove）。
 */
object MessageRepo {
    data class Message(
        val id: Int,
        val title: String,
        val content: String,
        val time: String,
        val read: Boolean
    ) {
        companion object {
            fun fromJson(o: JSONObject) = Message(
                id = o.optInt("id"),
                title = o.optString("title"),
                content = o.optString("content"),
                time = o.optString("createtime").ifEmpty { o.optString("time") },
                read = o.optInt("is_read", 0) == 1 || o.optBoolean("read")
            )
        }
    }

    suspend fun list(page: Int = 1): Pair<List<Message>, Boolean> = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/message/index", mapOf("page" to page))
        val arr = d.optJSONArray("list") ?: d.optJSONArray("data") ?: d.optJSONArray("messages")
        val list = arr?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(Message::fromJson) } } ?: emptyList()
        Pair(list, d.optBoolean("has_more", page < d.optInt("total_pages", page)))
    }

    /** 未读数 */
    suspend fun unread(): Int = withContext(Dispatchers.IO) {
        try {
            val d = Http.post("/api/softlib/message/unread")
            d.optInt("count", d.optInt("unread", 0))
        } catch (_: Exception) { 0 }
    }

    /** 长轮询（新消息推送）；lastId 之前的不返回 */
    suspend fun wait(lastId: Int, timeoutSec: Int = 25): Message? = withContext(Dispatchers.IO) {
        try {
            val c = Http.longClient
            val rb = okhttp3.Request.Builder()
                .url(ApiHost.BASE + "/api/softlib/message/wait?last_id=$lastId")
            Http.commonHeaders().forEach { (k, v) -> rb.header(k, v) }
            c.newCall(rb.build()).execute().use { r ->
                val j = JSONObject(r.body?.string() ?: "{}")
                if (j.optInt("code") == 1) {
                    val d = j.optJSONObject("data") ?: JSONObject()
                    d.optJSONObject("message")?.let { return@withContext Message.fromJson(it) }
                    d.optJSONObject("msg")?.let { return@withContext Message.fromJson(it) }
                }
                null
            }
        } catch (_: Exception) { null }
    }

    suspend fun markRead(id: Int) = withContext(Dispatchers.IO) {
        try { Http.post("/api/softlib/message/read", mapOf("id" to id)) } catch (_: Exception) {}
    }

    suspend fun remove(id: Int) = withContext(Dispatchers.IO) {
        try { Http.post("/api/softlib/message/delete", mapOf("id" to id)) } catch (_: Exception) {}
    }
}
