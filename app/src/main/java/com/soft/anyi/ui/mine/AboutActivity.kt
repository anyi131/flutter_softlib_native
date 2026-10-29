package com.soft.anyi.ui.mine

import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.soft.anyi.R
import com.soft.anyi.SoftApp
import com.soft.anyi.ui.common.Ui

/** 关于软件（对应 mine/about.dart） */
class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("关于软件")

        val root = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.page_bg))
            setPadding(Ui.dp(this@AboutActivity, 12), Ui.dp(this@AboutActivity, 24), Ui.dp(this@AboutActivity, 12), Ui.dp(this@AboutActivity, 24))
        }
        root.addView(box)
        setContentView(root)

        val ver = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) { "1.0" }

        box.addView(TextView(this).apply {
            text = "安逸软件汇"
            textSize = 24f; setTypeface(typeface, Typeface.BOLD)
            setTextColor(getColor(R.color.t1)); gravity = android.view.Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2))
        box.addView(TextView(this).apply {
            text = "v$ver"
            textSize = 13f; setTextColor(getColor(R.color.t3)); gravity = android.view.Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@AboutActivity, 6) })

        val cfg = SoftApp.instance.config
        val desc = cfg?.aboutSlogan?.takeIf { it.isNotEmpty() }
            ?: "本应用提供安卓软件资源的浏览与下载服务。\n\n· 海量软件资源每日更新\n· 会员专享高速下载\n· 线报广场第一时间掌握活动"

        box.addView(TextView(this).apply {
            text = desc
            textSize = 14f; setTextColor(getColor(R.color.t2))
            setLineSpacing(Ui.dp(this@AboutActivity, 4).toFloat(), 1f)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@AboutActivity, 24) })

        box.addView(TextView(this).apply {
            text = "客服：${cfg?.serviceUrl ?: ""}"
            textSize = 13f; setTextColor(getColor(R.color.t3))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@AboutActivity, 24) })
    }
}
