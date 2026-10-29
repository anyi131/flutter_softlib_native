package com.soft.anyi.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** 评论评分仓库（对应 ReviewService） */
object ReviewRepo {
    data class Summary(val avg: Double, val count: Int, val dist: Map<Int, Int>)

    data class Review(
        val id: Int, val nickname: String, val avatar: String,
        val score: Int, val content: String, val images: List<String>,
        val likes: Int, val liked: Boolean, val time: String,
        val reply: String, val userId: Int
    )

    suspend fun summary(appId: Int): Summary = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/review/summary", mapOf("app_id" to appId))
        val dist = mutableMapOf<Int, Int>()
        d.optJSONObject("dist")?.let { o ->
            for (star in 1..5) dist[star] = o.optInt(star.toString(), 0)
        }
        Summary(d.optDouble("avg", 0.0), d.optInt("count"), dist)
    }

    suspend fun list(appId: Int, page: Int = 1, star: Int = 0): List<Review> =
        withContext(Dispatchers.IO) {
            val d = Http.post(
                "/api/softlib/review/list",
                buildMap {
                    put("app_id", appId); put("page", page)
                    if (star in 1..5) put("star", star)
                }
            )
            val arr = d.optJSONArray("list") ?: d.optJSONArray("reviews") ?: return@withContext emptyList()
            (0 until arr.length()).mapNotNull { n ->
                val o = arr.optJSONObject(n) ?: return@mapNotNull null
                try {
                    val imgs = o.optJSONArray("images")?.let { a ->
                        (0 until a.length()).mapNotNull { a.optString(it).takeIf { s -> s.isNotEmpty() } }
                    } ?: emptyList()
                    Review(
                        o.optInt("id"), o.optString("nickname").ifEmpty { o.optString("username") },
                        o.optString("avatar"), o.optInt("score", o.optInt("star", 5)),
                        o.optString("content"), imgs, o.optInt("likes", 0),
                        o.optBoolean("liked", o.optInt("is_like", 0) == 1),
                        o.optString("createtime").ifEmpty { o.optString("time") },
                        o.optString("reply", ""), o.optInt("user_id")
                    )
                } catch (_: Exception) { null }
            }
        }

    suspend fun create(appId: Int, score: Int, content: String, images: List<String> = emptyList()) =
        withContext(Dispatchers.IO) {
            Http.post(
                "/api/softlib/review/create",
                mapOf("app_id" to appId, "score" to score, "content" to content, "images" to images.joinToString(","))
            )
        }

    suspend fun like(id: Int): Int? = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/review/like", mapOf("id" to id))
        if (d.has("likes")) d.optInt("likes") else null
    }

    suspend fun remove(id: Int) = withContext(Dispatchers.IO) {
        Http.post("/api/softlib/review/delete", mapOf("id" to id))
    }

    suspend fun reply(id: Int, content: String) = withContext(Dispatchers.IO) {
        Http.post("/api/softlib/review/reply", mapOf("id" to id, "content" to content))
    }
}

/** 单独解锁（对应 UnlockService：某些软件单买解锁，不走会员） */
object UnlockRepo {
    data class Status(val unlocked: Boolean, val price: String, val owned: Boolean)

    suspend fun status(appId: Int): Status = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/unlock/status", mapOf("app_id" to appId))
        Status(d.optBoolean("unlocked"), d.optString("price"), d.optBoolean("owned"))
    }

    suspend fun buy(appId: Int): JSONObject = withContext(Dispatchers.IO) {
        Http.post("/api/softlib/unlock/buy", mapOf("app_id" to appId))
    }
}

/** 心愿单（对应 WishService） */
object WishRepo {
    suspend fun list(page: Int = 1, keyword: String = ""): JSONObject = withContext(Dispatchers.IO) {
        Http.post(
            "/api/softlib/wishlist",
            buildMap { put("page", page); if (keyword.isNotBlank()) put("keyword", keyword) }
        )
    }

    suspend fun add(appName: String, desc: String): String = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/wishadd", mapOf("name" to appName, "desc" to desc))
        d.optString("msg", "已提交")
    }

    suspend fun vote(id: Int): String = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/wishvote", mapOf("id" to id))
        d.optString("msg", "已支持")
    }

    suspend fun mine(): List<JSONObject> = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/wishmine")
        val arr = d.optJSONArray("list") ?: return@withContext emptyList()
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }
}

/** 心愿单仓库别名（MineFragment 用 Wish.list/add） */
object Wish {
    val list get() = WishRepo.list
    val mine get() = WishRepo.mine
    suspend fun add(name: String, desc: String): String = WishRepo.add(name, desc)
    suspend fun vote(id: Int): String = WishRepo.vote(id)
}

/** 任务中心（对应 TaskService：输入口令领取奖励） */
object TaskRepo {
    suspend fun config(): JSONObject? = withContext(Dispatchers.IO) {
        try { Http.post("/api/softlib/taskpwdinfo") } catch (_: Exception) { null }
    }

    suspend fun claim(password: String): String = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/taskpwd", mapOf("password" to password))
        d.optString("msg", "领取成功").ifEmpty { "领取成功" }
    }

    suspend fun myApplies(): List<JSONObject> = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/taskapplymine")
        val arr = d.optJSONArray("list") ?: return@withContext emptyList()
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    suspend fun apply(content: String): String = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/taskapply", mapOf("content" to content))
        d.optString("msg", "已提交")
    }
}

/** 评论仓库适配（CommentsActivity 调用名对齐）——已并入 SoftRepo.kt */

