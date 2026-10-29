package com.soft.anyi.data

import android.util.Log
import com.soft.anyi.SoftApp
import com.soft.anyi.util.AppSign
import com.soft.anyi.util.DeviceInfo
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 全 App 共用一个 HTTP 客户端（对应 Flutter 版 ApiHttp：连接池/TLS 复用）
 * + 统一请求头（设备信息 / 签名指纹）+ 响应解包（code==1 → data）。
 */
object Http {
    private const val TAG = "Softlib.Http"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /** 长任务（下载/上传/长轮询）用不超时客户端 */
    val longClient: OkHttpClient = client.newBuilder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    class ApiException(val code: Int, msg: String) : Exception(msg)

    /** 解包：{code:1,msg,data} → data；code!=1 → 抛 ApiException(msg) */
    @Throws(ApiException::class)
    fun unwrap(body: String?): JSONObject {
        val j = try { JSONObject(body ?: "{}") } catch (_: Exception) { JSONObject() }
        val code = j.optInt("code", 0)
        if (code != 1) throw ApiException(code, j.optString("msg", "请求失败"))
        return j.optJSONObject("data") ?: JSONObject()
    }

    fun commonHeaders(auth: Boolean = true): Map<String, String> = buildMap {
        DeviceInfo.headers.forEach { (k, v) -> put(k, v) }
        AppSign.headers().forEach { (k, v) -> put(k, v) }
        if (auth) Session.token()?.let { put("token", it) }
    }

    // ── 同步（协程 IO 里调） ────────────────────────────────────────

    fun get(path: String, query: Map<String, String> = emptyMap(), auth: Boolean = true): JSONObject {
        val url = buildUrl(path, query)
        val rb = Request.Builder().url(url).get()
        commonHeaders(auth).forEach { (k, v) -> rb.header(k, v) }
        client.newCall(rb.build()).execute().use { r ->
            return unwrap(r.body?.string())
        }
    }

    fun post(path: String, form: Map<String, Any?> = emptyMap(), auth: Boolean = true): JSONObject {
        val body = JSONObject().apply {
            form.forEach { (k, v) ->
                when (v) {
                    null -> putOpt(k, JSONObject.NULL)
                    else -> putOpt(k, v)
                }
            }
        }
        val rb = Request.Builder().url(ApiHost.BASE + path)
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        commonHeaders(auth).forEach { (k, v) -> rb.header(k, v) }
        client.newCall(rb.build()).execute().use { r ->
            return unwrap(r.body?.string())
        }
    }

    // ── 异步（不阻塞） ─────────────────────────────────────────────

    fun getAsync(path: String, query: Map<String, String> = emptyMap(), cb: (JSONObject?, Exception?) -> Unit) {
        val url = buildUrl(path, query)
        val rb = Request.Builder().url(url).get()
        commonHeaders().forEach { (k, v) -> rb.header(k, v) }
        client.newCall(rb.build()).enqueue(wrap(cb))
    }

    fun postAsync(path: String, form: Map<String, Any?> = emptyMap(), cb: (JSONObject?, Exception?) -> Unit) {
        val body = JSONObject().apply {
            form.forEach { (k, v) -> if (v != null) putOpt(k, v) }
        }
        val rb = Request.Builder().url(ApiHost.BASE + path)
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        commonHeaders().forEach { (k, v) -> rb.header(k, v) }
        client.newCall(rb.build()).enqueue(wrap(cb))
    }

    private fun buildUrl(path: String, query: Map<String, String>): String {
        val sb = StringBuilder(ApiHost.BASE).append(path)
        if (query.isNotEmpty()) {
            sb.append(if (path.contains('?')) '&' else '?')
            query.entries.forEachIndexed { i, (k, v) ->
                if (i > 0) sb.append('&')
                sb.append(k).append('=').append(java.net.URLEncoder.encode(v, "UTF-8"))
            }
        }
        return sb.toString()
    }

    private fun wrap(cb: (JSONObject?, Exception?) -> Unit): Callback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.w(TAG, "net fail: ${e.message}")
            cb(null, e)
        }
        override fun onResponse(call: Call, r: Response) {
            try {
                cb(unwrap(r.body?.string()), null)
            } catch (e: Exception) {
                cb(null, e)
            }
        }
    }
}
