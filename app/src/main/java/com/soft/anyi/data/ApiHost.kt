package com.soft.anyi.data

/**
 * 服务端统一入口（对应 ApiHost，改域名只改这里）。
 */
object ApiHost {
    const val BASE = "https://flrjk.52yfx.cn"

    fun appPage(id: Int) = "$BASE/app.html?id=$id"

    const val ADMIN = "$BASE/admin/index.php"

    /** 自建蓝奏云解析服务 */
    const val LZY_PROXY = "https://www.52yfx.cn/lzy.php"

    /** 站内图片代理（外链图走服务器缓存，防第三方图床抖动） */
    fun proxyImg(url: String) = "$BASE/api/softlib/proxy/img?url=${java.net.URLEncoder.encode(url, "UTF-8")}"
}
