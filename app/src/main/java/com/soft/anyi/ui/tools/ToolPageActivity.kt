package com.soft.anyi.ui.tools

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.data.ToolboxRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 本地工具页（对应 toolbox1/toolbox2 系列：每日一句/单位换算/翻译/IP查询 等）。
 * 通用做法：调 /api/softlib/toolbox，把返回 JSON 渲染成「键值/表格/文本」。
 */
class ToolPageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra("name") ?: intent.getStringExtra("tool_id")?.let { "tool_$it" } ?: ""
        val ctx = this
        val root = LinearLayoutRoot(ctx)
        setContentView(root.scroll)

        supportActionBar?.title = name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch {
            val d = ToolboxRepo.box1(name)
            if (d == null) {
                Toasts.e("工具加载失败")
                return@launch
            }
            root.renderJson(d)
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    /** 简易 JSON 渲染容器 */
    private class LinearLayoutRoot(ctx: android.content.Context) {
        val box = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(com.soft.anyi.R.color.page_bg))
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 32))
        }
        val scroll = androidx.core.widget.NestedScrollView(ctx).apply { addView(box) }

        fun renderJson(j: JSONObject) {
            val ctx = box.context
            j.keys().forEach { k ->
                box.addView(android.widget.TextView(ctx).apply {
                    text = "$k：${j.optString(k)}"
                    textSize = 15f
                    setPadding(0, Ui.dp(ctx, 8), 0, Ui.dp(ctx, 8))
                })
            }
        }
    }
}
