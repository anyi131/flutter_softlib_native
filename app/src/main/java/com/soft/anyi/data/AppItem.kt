package com.soft.anyi.data

import org.json.JSONArray
import org.json.JSONObject

/** 软件条目（字段对准 /api/softlib/app/index） */
data class AppItem(
    val id: Int,
    val title: String,
    val provider: String,      // lzy | local
    val url: String,           // 蓝奏云分享地址（lzy）
    val file: String,          // 服务器直链（local）
    val icon: String,
    val size: String,
    val version: String,
    val description: String,
    val catId: Int,
    val weigh: Int = 0,
    val views: Int = 0,
    val downCount: Int = 0,
    val uploadDate: String = "",
    val ageRating: String = "16+",
    val screenshots: List<String>,
    val scoreAvg: Double = 0.0,
    val scoreCount: Int = 0,
    val isVipOnly: Boolean = false,
    val vipPrice: String = "",
    val isNew: Boolean = false,
    val fromFolder: Boolean = false,
    val isTop: Boolean = false,
    val urlType: String = "apk",   // apk | browser | webview
    val adminNote: String = ""
) {
    val isLocal: Boolean get() = provider == "local"

    /** 会员专享（后台开关优先，兼容旧的「会员专区」分类 catId==5） */
    val isVipItem: Boolean get() = isVipOnly || catId == 5

    val downloadTarget: String get() = if (isLocal) file else url

    companion object {
        private fun i(v: Any?) = v?.toString()?.toDoubleOrNull()?.toInt() ?: 0
        private fun s(v: Any?) = v?.toString() ?: ""

        fun fromJson(j: JSONObject): AppItem = AppItem(
            id = i(j.opt("id")),
            title = s(j.opt("title")),
            provider = s(j.opt("provider")),
            url = s(j.opt("url")),
            file = s(j.opt("file")),
            icon = s(j.opt("icon")),
            size = s(j.opt("size")),
            version = s(j.opt("version")),
            description = s(j.opt("description")),
            catId = i(j.opt("cat_id")),
            weigh = i(j.opt("weigh")),
            views = i(j.opt("views")),
            downCount = i(j.opt("down_count")),
            uploadDate = s(j.opt("upload_date")),
            ageRating = s(j.opt("age_rating")).ifEmpty { "16+" },
            screenshots = j.optJSONArray("screenshots")?.let { a: JSONArray ->
                (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() }
            } ?: emptyList(),
            scoreAvg = s(j.opt("score_avg")).toDoubleOrNull() ?: 0.0,
            scoreCount = i(j.opt("score_count")),
            isVipOnly = j.opt("is_vip") == true || i(j.opt("is_vip")) == 1,
            vipPrice = s(j.opt("vip_price")),
            isNew = j.opt("is_new") == true || i(j.opt("is_new")) == 1,
            fromFolder = j.opt("from_folder") == true || i(j.opt("from_folder")) == 1,
            isTop = j.opt("is_top") == true || i(j.opt("is_top")) == 1,
            urlType = s(j.opt("url_type")).ifEmpty { "apk" },
            adminNote = s(j.opt("admin_note"))
        )

        fun listFrom(data: Any?): List<AppItem> {
            val arr: JSONArray = when (data) {
                is JSONArray -> data
                is JSONObject -> data.optJSONArray("list")
                    ?: data.optJSONArray("data")
                    ?: data.optJSONArray("apps")
                    ?: JSONArray()
                else -> JSONArray()
            }
            return (0 until arr.length()).mapNotNull { n ->
                val o = arr.optJSONObject(n) ?: return@mapNotNull null
                try { fromJson(o) } catch (_: Exception) { null }
            }
        }
    }
}

/** 软件分类 */
data class AppCat(val id: Int, val title: String, val icon: String = "") {
    companion object {
        fun listFrom(data: Any?): List<AppCat> {
            val arr = data as? JSONArray ?: return emptyList()
            return (0 until arr.length()).mapNotNull { n ->
                val o = arr.optJSONObject(n) ?: return@mapNotNull null
                try {
                    AppCat(o.optInt("id"), o.optString("title"), o.optString("icon"))
                } catch (_: Exception) { null }
            }
        }
    }
}
