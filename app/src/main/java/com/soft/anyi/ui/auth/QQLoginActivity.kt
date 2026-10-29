package com.soft.anyi.ui.auth

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.soft.anyi.R
import com.soft.anyi.ui.common.Ui

/**
 * QQ 登录占位页：原生端暂未接入 QQ OpenSDK，
 * 提示用户使用账号密码登录（与 Flutter 端 qq_login 流程互补）。
 */
class QQLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(getColor(R.color.page_bg))
            setPadding(Ui.dp(ctx, 32), Ui.dp(ctx, 32), Ui.dp(ctx, 32), Ui.dp(ctx, 32))
        }
        root.addView(TextView(ctx).apply {
            text = "QQ 登录暂未在原生版开放\n请使用账号密码登录"
            gravity = Gravity.CENTER
            textSize = 15f
            setTextColor(getColor(R.color.t1))
            setPadding(0, 0, 0, Ui.dp(ctx, 20))
        })
        root.addView(TextView(ctx).apply {
            text = "返回"
            gravity = Gravity.CENTER
            textSize = 15f
            setTextColor(android.graphics.Color.WHITE)
            background = getDrawable(R.drawable.bg_btn_primary)
            setPadding(Ui.dp(ctx, 40), Ui.dp(ctx, 12), Ui.dp(ctx, 40), Ui.dp(ctx, 12))
            setOnClickListener { finish() }
        })
        setContentView(root)
    }
}
