package com.soft.anyi.ui.details

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.Session
import com.soft.anyi.data.SoftRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Sfx
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/**
 * 评论页（对应 review 模块）：列表 + 点赞 + 回复 + 发布。
 */
class CommentsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "id"
        fun start(ctx: android.content.Context, appId: Int) {
            ctx.startActivity(android.content.Intent(ctx, CommentsActivity::class.java).putExtra(EXTRA_ID, appId))
        }
    }

    private lateinit var box: LinearLayout
    private lateinit var scroll: ScrollView
    private var appId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appId = intent.getIntExtra(EXTRA_ID, 0)
        val ctx = this
        scroll = ScrollView(ctx).apply { setBackgroundColor(ctx.getColor(R.color.page_bg)) }
        box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 14), Ui.dp(ctx, 14), Ui.dp(ctx, 40))
        }
        scroll.addView(box)
        setContentView(scroll)
        supportActionBar?.title = "评论"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 底部发布条
        val input = EditText(ctx).apply {
            hint = "说点什么…"
            singleLine = true
            textSize = 14f
            setBackgroundResource(R.drawable.bg_card)
            setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 8), Ui.dp(ctx, 12), Ui.dp(ctx, 8))
        }
        val send = TextView(ctx).apply {
            text = "发布"
            setTextColor(ctx.getColor(R.color.brand))
            gravity = Gravity.CENTER
            setPadding(Ui.dp(ctx, 14), 0, Ui.dp(ctx, 14), 0)
            setOnClickListener {
                val t = input.text.toString().trim()
                if (t.isEmpty()) { Toasts.s("先输入内容"); return@setOnClickListener }
                if (!Session.requireLogin()) return@setOnClickListener
                lifecycleScope.launch {
                    try {
                        SoftRepo.reviewCreate(appId, 5, t)
                        input.setText("")
                        Toasts.s("已发布")
                        Sfx.play("message")
                        load()
                    } catch (e: Exception) { Toasts.e(e.message ?: "发布失败") }
                }
            }
        }
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 6), Ui.dp(ctx, 10), Ui.dp(ctx, 6))
            setBackgroundColor(0xFFFFFFFF.toInt())
            addView(input, LinearLayout.LayoutParams(0, -2, 1f))
            addView(send)
        }
        addContentView(bar, android.widget.FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        load()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun load() {
        lifecycleScope.launch {
            try {
                val list = SoftRepo.reviews(appId)
                box.removeAllViews()
                if (list.isEmpty()) {
                    box.addView(center("还没有评论，来抢沙发"))
                    return@launch
                }
                list.forEach { r -> box.addView(item(r.raw)) }
            } catch (e: Exception) { Toasts.e(e.message ?: "加载失败") }
        }
    }

    private fun center(t: String) = TextView(this).apply {
        text = t; gravity = Gravity.CENTER; textSize = 14f
        setTextColor(getColor(R.color.t2))
        setPadding(0, Ui.dp(this@CommentsActivity, 40), 0, 0)
    }

    private fun item(r: org.json.JSONObject): View {
        val ctx = this
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = ctx.getDrawable(R.drawable.bg_card)
            setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 12))
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.topMargin = Ui.dp(ctx, 8)
            layoutParams = lp

            addView(TextView(ctx).apply {
                text = r.optString("nickname", "匿名用户") + "  ·  " + r.optString("created_at", "")
                textSize = 12f; setTextColor(ctx.getColor(R.color.t2))
            })
            addView(TextView(ctx).apply {
                text = r.optString("content")
                textSize = 14f; setTextColor(ctx.getColor(R.color.t1))
                setPadding(0, Ui.dp(ctx, 6), 0, Ui.dp(ctx, 6))
            })
            // 底部操作行
            val ops = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            fun op(label: String, onClick: () -> Unit) = TextView(ctx).apply {
                text = label
                textSize = 12f
                setTextColor(ctx.getColor(R.color.brand))
                setPadding(0, 0, Ui.dp(ctx, 18), 0)
                setOnClickListener { onClick() }
            }
            ops.addView(op("👍 赞 ${r.optInt("likes", 0)}") {
                if (!Session.requireLogin()) return@op
                lifecycleScope.launch {
                    try { SoftRepo.reviewLike(r.optInt("id")); load() } catch (e: Exception) { Toasts.e(e.message ?: "失败") }
                }
            })
            ops.addView(op("回复") {
                if (!Session.requireLogin()) return@op
                val input = EditText(ctx).apply { hint = "回复 @${r.optString("nickname", "")}" }
                AlertDialog.Builder(ctx)
                    .setTitle("回复")
                    .setView(input)
                    .setPositiveButton("发送") { _, _ ->
                        val t = input.text.toString().trim()
                        if (t.isNotEmpty()) lifecycleScope.launch {
                            try {
                                SoftRepo.reviewReply(appId, r.optInt("id"), t)
                                load()
                            } catch (e: Exception) { Toasts.e(e.message ?: "失败") }
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
            })
            addView(ops)
        }
    }
}
