package com.soft.anyi.ui.wish

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.MiscRepos.Wish
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch
import org.json.JSONObject

/** 心愿单（对应 wish/wish.dart） */
class WishActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("心愿单")

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(getColor(R.color.page_bg))
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@WishActivity, 16), Ui.dp(this@WishActivity, 16),
                Ui.dp(this@WishActivity, 16), Ui.dp(this@WishActivity, 24))
        }
        scroll.addView(box, ViewGroup.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)

        val input = TextView(this).apply {
            text = "+ 提个心愿"
            textSize = 14f
            setTextColor(androidx.core.content.ContextCompat.getColor(this@WishActivity, R.color.brand))
            setPadding(0, Ui.dp(this@WishActivity, 4), 0, Ui.dp(this@WishActivity, 12))
        }
        box.addView(input)

        lifecycleScope.launch {
            try {
                val list = Wish.list()
                if (list.isEmpty()) {
                    box.addView(TextView(this@WishActivity).apply {
                        text = "还没有心愿，快来提一个"
                        textSize = 14f; setTextColor(getColor(R.color.t2))
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, Ui.dp(this@WishActivity, 100), 0, 0)
                    })
                } else list.forEach { w -> box.addView(item(w)) }
            } catch (e: Exception) {
                Toasts.e("加载失败：${e.message}")
            }
        }

        input.setOnClickListener {
            android.app.AlertDialog.Builder(this).apply {
                setTitle("提个心愿")
                val et = android.widget.EditText(this@WishActivity).apply {
                    hint = "想要什么软件或功能？"
                    setSingleLine(false); minLines = 2
                }
                setView(et)
                setPositiveButton("提交") { _, _ ->
                    val t = et.text.toString().trim()
                    if (t.isEmpty()) return@setPositiveButton
                    lifecycleScope.launch {
                        try { Wish.add(t, ""); Toasts.s("已提交"); recreate() } catch (e: Exception) { Toasts.e("提交失败：${e.message}") }
                    }
                }
                setNegativeButton("取消", null)
            }.show()
        }
    }

    private fun item(w: JSONObject): TextView = TextView(this).apply {
        val votes = w.optInt("votes", 0)
        text = "${w.optString("title")}（👍 $votes）"
        textSize = 15f
        setTextColor(getColor(R.color.t2))
        setPadding(Ui.dp(this@WishActivity, 12), Ui.dp(this@WishActivity, 12), Ui.dp(this@WishActivity, 12), Ui.dp(this@WishActivity, 12))
        background = androidx.core.content.ContextCompat.getDrawable(this@WishActivity, R.drawable.bg_card)
        LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)
            .let { it.topMargin = Ui.dp(this@WishActivity, 8); layoutParams = it }
        setOnClickListener {
            lifecycleScope.launch {
                try { Wish.vote(w.optInt("id")); Toasts.s("已点赞"); } catch (e: Exception) { Toasts.e(e.message ?: "失败") }
            }
        }
    }
}
