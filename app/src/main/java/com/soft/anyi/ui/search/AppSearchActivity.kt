package com.soft.anyi.ui.search

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.soft.anyi.R
import com.soft.anyi.data.AppItem
import com.soft.anyi.data.SoftRepo
import com.soft.anyi.ui.common.AppAction
import com.soft.anyi.ui.common.AppCard
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Sfx
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/** 软件搜索页（对应 home/search_page.dart） */
class AppSearchActivity : AppCompatActivity() {
    private lateinit var box: ViewGroup
    private var results: List<AppItem> = emptyList()

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(R.color.page_bg))
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 12), Ui.dp(ctx, 16), 0)
        }
        // 顶栏
        root.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(TextView(ctx).apply {
                text = "←"
                textSize = 22f
                setPadding(0, 0, Ui.dp(ctx, 16), 0)
                setOnClickListener { finish() }
            })
            addView(TextView(ctx).apply {
                text = "搜索软件"
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ctx.getColor(R.color.t1))
            })
        })
        // 搜索框
        val input = EditText(ctx).apply {
            hint = "输入名称关键字"
            setSingleLine()
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            background = ctx.getDrawable(R.drawable.bg_card)
            setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 10), Ui.dp(ctx, 14), Ui.dp(ctx, 10))
            setOnEditorActionListener { v, _, event ->
                if (event == null) { search(v.text.toString()); true } else false
            }
        }
        root.addView(input, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 12) })
        // 结果容器
        box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.addView(box, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 8) })
        setContentView(root)
    }

    private fun search(kw: String) {
        val k = kw.trim()
        if (k.isEmpty()) return
        Sfx.play("click")
        lifecycleScope.launch {
            try {
                results = SoftRepo.search(k)
                render()
            } catch (e: Exception) {
                Toasts.e("搜索失败：${e.message}")
            }
        }
    }

    private fun render() {
        val ctx = this
        box.removeAllViews()
        if (results.isEmpty()) {
            box.addView(Ui.emptyView(ctx, "没有找到相关软件"))
            return
        }
        results.forEach { a -> box.addView(AppCard.build(ctx, a)) }
    }
}
