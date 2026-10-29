package com.soft.anyi.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.soft.anyi.data.Session

/**
 * 页面跳转工具（对应 Pager.push / launchUrl）。
 */
object Pager {
    fun open(ctx: Context, url: String, inApp: Boolean = true) {
        if (inApp) {
            WebViewActivity.start(ctx, url)
        } else {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    fun openBrowser(ctx: Context, url: String) {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
