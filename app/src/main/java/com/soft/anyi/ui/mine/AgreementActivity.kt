package com.soft.anyi.ui.mine

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.soft.anyi.R
import com.soft.anyi.data.AppConfig
import com.soft.anyi.SoftApp
import com.soft.anyi.ui.common.Ui

/** 用户协议/隐私政策（对应 mine/agreement.dart） */
class AgreementActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val kind = intent.getIntExtra("kind", 0) // 0=用户协议 1=隐私政策
        setTitle(if (kind == 0) "用户协议" else "隐私政策")

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(getColor(R.color.page_bg))
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@AgreementActivity, 16), Ui.dp(this@AgreementActivity, 16),
                Ui.dp(this@AgreementActivity, 16), Ui.dp(this@AgreementActivity, 24))
        }
        scroll.addView(box, ViewGroup.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)

        val cfg: AppConfig? = SoftApp.instance.config
        val body = if (kind == 0) (cfg?.agreement ?: "") else (cfg?.privacy ?: "")
        box.addView(TextView(this).apply {
            text = body.ifEmpty { "暂未配置，请联系管理员。" }
            textSize = 14f
            setTextColor(getColor(R.color.t2))
            setLineSpacing(Ui.dp(this@AgreementActivity, 4), 1f)
        })
    }
}
