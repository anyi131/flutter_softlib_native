package com.soft.anyi.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.soft.anyi.SoftApp
import java.security.MessageDigest

/**
 * App 自身签名指纹（对应 Flutter 版 AppSign，防重打包）。
 * 把安装包签名 MD5 随请求带给服务端（X-App-Sign），服务端比对「官方登记的签名」。
 */
object AppSign {
    private var cache: String? = null

    fun value(ctx: Context = SoftApp.ctx()): String {
        cache?.let { return it }
        return try {
            val pm = ctx.packageManager
            val pkg = ctx.packageName
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(
                    pkg, PackageManager.GET_SIGNING_CERTIFICATES
                )
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION", "PackageManagerGetSignatures")
                pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures
            }
            val cert = signatures?.firstOrNull()?.toByteArray() ?: return ""
            val md = MessageDigest.getInstance("MD5").digest(cert)
            md.joinToString("") { "%02x".format(it) }.also { cache = it }
        } catch (_: Exception) {
            ""
        }
    }

    /** 请求头（拿不到签名就不带，服务端记 no_sign） */
    fun headers(): Map<String, String> {
        val s = value()
        if (s.isEmpty()) return emptyMap()
        return mapOf(
            "X-App-Sign" to s,
            "X-App-Ts" to (System.currentTimeMillis() / 1000).toString()
        )
    }
}
