package com.soft.anyi.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** 广场帖子仓库（对应 PostService） */
object PostRepo {
    data class PostCat(val id: Int, val title: String) {
        companion object {
            fun listFrom(d: JSONObject): List<PostCat> {
                val arr = d.optJSONArray("cats") ?: d.optJSONArray("data") ?: return emptyList()
                return (0 until arr.length()).mapNotNull { n ->
                    arr.optJSONObject(n)?.let { PostCat(it.optInt("id"), it.optString("title")) }
                }
            }
        }
    }

    data class Post(
        val id: Int,
        val userId: Int,
        val nickname: String,
        val avatar: String,
        val content: String,
        val images: List<String>,
        val video: String,
        val likes: Int,
        val comments: Int,
        val liked: Boolean,
        val time: String,
        val catId: Int,
        val raw: JSONObject = JSONObject()
    ) {
        companion object {
            fun fromJson(o: JSONObject): Post {
                val imgs = o.optJSONArray("images")?.let { a ->
                    (0 until a.length()).mapNotNull { a.optString(it).takeIf { s -> s.isNotEmpty() } }
                } ?: emptyList()
                return Post(
                    id = o.optInt("id"),
                    userId = o.optInt("user_id"),
                    nickname = o.optString("nickname").ifEmpty { o.optString("username") },
                    avatar = o.optString("avatar"),
                    content = o.optString("content"),
                    images = imgs,
                    video = o.optString("video"),
                    likes = o.optInt("likes", o.optInt("like_count", 0)),
                    comments = o.optInt("comments", o.optInt("comment_count", 0)),
                    liked = o.optBoolean("liked", o.optInt("is_like", 0) == 1),
                    time = o.optString("createtime").ifEmpty { o.optString("time") },
                    catId = o.optInt("cat_id"),
                    raw = o
                )
            }

            fun listFrom(d: JSONObject): List<Post> {
                val arr = d.optJSONArray("list") ?: d.optJSONArray("posts")
                    ?: (d.opt("data") as? JSONArray) ?: return emptyList()
                return (0 until arr.length()).mapNotNull { n ->
                    arr.optJSONObject(n)?.let {
                        try { fromJson(it) } catch (_: Exception) { null }
                    }
                }
            }
        }
    }

    data class Comment(
        val id: Int, val nickname: String, val avatar: String,
        val content: String, val time: String, val replyTo: String
    ) {
        companion object {
            fun fromJson(o: JSONObject) = Comment(
                o.optInt("id"), o.optString("nickname").ifEmpty { o.optString("username") },
                o.optString("avatar"), o.optString("content"),
                o.optString("createtime").ifEmpty { o.optString("time") },
                o.optString("reply_nickname", "")
            )
        }
    }

    suspend fun cats(): List<PostCat> = withContext(Dispatchers.IO) {
        val d = Http.get("/api/softlib/post/cats")
        PostCat.listFrom(d)
    }

    suspend fun posts(page: Int = 1, catId: Int = 0): List<Post> = withContext(Dispatchers.IO) {
        val d = Http.post(
            "/api/softlib/post/index",
            buildMap { put("page", page); if (catId > 0) put("cat_id", catId) }
        )
        Post.listFrom(d)
    }

    suspend fun detail(id: Int): Post? = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/post/detail", mapOf("id" to id))
        d.optJSONObject("post")?.let { try { Post.fromJson(it) } catch (_: Exception) { null } }
    }

    suspend fun create(content: String, images: List<String>, video: String, catId: Int = 0): Boolean =
        withContext(Dispatchers.IO) {
            val d = Http.post(
                "/api/softlib/post/create",
                mapOf(
                    "content" to content,
                    "images" to images.joinToString(","),
                    "video" to video,
                    "cat_id" to catId
                )
            )
            d.length() >= 0 // unwrap 已保证 code==1
            true
        }

    suspend fun like(id: Int): Int? = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/post/like", mapOf("id" to id))
        if (d.has("likes")) d.optInt("likes") else null
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO) {
        Http.post("/api/softlib/post/delete", mapOf("id" to id))
    }

    suspend fun comments(postId: Int): List<Comment> = withContext(Dispatchers.IO) {
        val d = Http.post("/api/softlib/post/comment_list", mapOf("post_id" to postId))
        val arr = d.optJSONArray("list") ?: d.optJSONArray("comments") ?: return@withContext emptyList()
        (0 until arr.length()).mapNotNull { n ->
            arr.optJSONObject(n)?.let { try { Comment.fromJson(it) } catch (_: Exception) { null } }
        }
    }

    suspend fun comment(postId: Int, content: String, replyId: Int = 0): Boolean =
        withContext(Dispatchers.IO) {
            Http.post(
                "/api/softlib/post/comment",
                buildMap { put("post_id", postId); put("content", content); if (replyId > 0) put("reply_id", replyId) }
            )
            true
        }

    suspend fun deleteComment(id: Int) = withContext(Dispatchers.IO) {
        Http.post("/api/softlib/post/comment_del", mapOf("id" to id))
    }
}
