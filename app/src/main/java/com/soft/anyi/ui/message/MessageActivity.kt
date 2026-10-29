package com.soft.anyi.ui.message

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.MessageRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch
import org.json.JSONObject

/** 消息中心（系统通知 / 公告），对应 Flutter message_page */
class MessageActivity : AppCompatActivity() {
    private val box = LinearLayout(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(R.color.page_bg))
        }
        setContentView(root)

        // 顶栏（返回 + 标题）
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 8), Ui.dp(ctx, 12), Ui.dp(ctx, 16), Ui.dp(ctx, 12))
        }
        bar.addView(TextView(ctx).apply {
            text = "‹"; textSize = 24f; setTextColor(ctx.getColor(R.color.t1))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = Ui.dp(ctx, 12) })
        bar.addView(TextView(ctx).apply {
            text = "消息中心"; textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ctx.getColor(R.color.t1))
        })
        root.addView(bar, LinearLayout.LayoutParams(-1, -2))

        box.orientation = LinearLayout.VERTICAL
        root.addView(android.widget.ScrollView(ctx).apply { addView(box) },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 4) })

        load()
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val pair = MessageRepo.list()
                render(pair.first)
            } catch (e: Exception) {
                Toasts.e(e.message ?: "加载失败")
            }
        }
    }

    private fun render(list: List<JSONObject>) {
        val ctx = this
        box.removeAllViews()
        if (list.isEmpty()) {
            box.addView(TextView(ctx).apply {
                text = "暂无消息"; gravity = Gravity.CENTER; setPadding(0, Ui.dp(ctx, 60), 0, 0)
                setTextColor(ctx.getColor(R.color.t3))
            })
            return
        }
        list.forEach { m ->
            box.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = ctx.getDrawable(R.drawable.bg_card)
                setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 12))
                addView(TextView(ctx).apply {
                    text = m.optString("title", "通知")
                    textSize = 15f; setTextColor(ctx.getColor(R.color.t1))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                addView(TextView(ctx).apply {
                    text = m.optString("content", ""); textSize = 13f
                    setTextColor(ctx.getColor(R.color.t2))
                }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 4) })
                addView(TextView(ctx).apply {
                    text = m.optString("created_at", ""); textSize = 11f
                    setTextColor(ctx.getColor(R.color.t3))
                }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 6) })
                val unread = m.optInt("is_read", 1) == 0
                if (unread) {
                    setOnClickListener {
                        lifecycleScope.launch {
                            try { MessageRepo.markRead(m.optInt("id")) } catch (_: Exception) {}
                        }
                    }
                }
            }, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = Ui.dp(ctx, 8); marginStart = Ui.dp(ctx, 12); marginEnd = Ui.dp(ctx, 12)
            })
        }
    }
}
