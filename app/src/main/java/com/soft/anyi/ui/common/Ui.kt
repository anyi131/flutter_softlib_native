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

    @JvmField
    val card = { context: Context ->
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_card)
            val pad = dp(context, 12)
            setPadding(pad, pad, pad, pad)
        }
    }

    @JvmField
    val sectionTitle = { context: Context, text: String ->
        TextView(context).apply {
            this.text = text
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.t1))
        }
    }

    @JvmField
    val pill = { context: Context, text: String, onClick: (View) -> Unit ->
        TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.white))
            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_pill_on)
            gravity = android.view.Gravity.CENTER
            val p = dp(context, 14)
            setPadding(p, dp(context, 9), p, dp(context, 9))
            setOnClickListener(onClick)
        }
    }

    /** 空态（兼容旧调用：emptyView(ctx)） */
    fun emptyView1(context: Context): View = emptyView(context, "暂无内容")
    fun sectionTitle1(context: Context): TextView = sectionTitle(context, "")

    fun sectionTitle(context: Context, text: String): TextView =
        TextView(context).apply {
            this.text = text
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
                    .putExtra("item_json", item.toJson().toString())
            )
        }
        com.soft.anyi.util.FavStore.addHistory(item.id)
        com.soft.anyi.data.SoftRepo.addView(item.id)
    }
}
