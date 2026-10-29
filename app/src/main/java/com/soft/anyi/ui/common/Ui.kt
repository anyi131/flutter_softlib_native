package com.soft.anyi.ui.common

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.soft.anyi.R
import com.soft.anyi.data.AppItem

/** 公共 UI 小件（对应 design/kit.dart 的 Loading/Empty/卡片） */
object Ui {
    fun dp(context: Context, v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics).toInt()

    @JvmField
    val emptyView = { context: Context, text: String ->
        TextView(context).apply {
            this.text = text
            gravity = android.view.Gravity.CENTER
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.t2))
            setPadding(0, dp(context, 64), 0, dp(context, 64))
        }
    }

    @JvmField
    val loadingView = { context: Context ->
        android.widget.ProgressBar(context).apply {
            setPadding(0, dp(context, 48), 0, dp(context, 48))
        }
    }

    fun sectionTitle(context: Context, text: String): TextView =
        TextView(context).apply {
            text = text
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.t1))
        }
}

/**
 * 软件卡片点击行为统一处理（对应 AppAction）：
 * urlType=browser → 系统浏览器；webview → 内置；apk → 详情页。
 */
object AppAction {
    fun open(context: Context, item: AppItem) {
        when (item.urlType) {
            "browser" -> com.soft.anyi.util.ApkInstaller.openBrowser(item.url.ifBlank { item.file })
            "webview" -> context.startActivity(
                android.content.Intent(context, com.soft.anyi.ui.common.WebViewActivity::class.java)
                    .putExtra("url", item.url.ifBlank { item.file })
                    .putExtra("title", item.title)
            )
            else -> context.startActivity(
                android.content.Intent(context, com.soft.anyi.ui.details.DetailsActivity::class.java)
                    .putExtra("app_id", item.id)
                    .putExtra("item", item)
            )
        }
        com.soft.anyi.util.FavStore.addHistory(item.id)
        com.soft.anyi.data.SoftRepo.addView(item.id)
    }
}
