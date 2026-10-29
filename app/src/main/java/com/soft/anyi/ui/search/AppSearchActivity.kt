package com.soft.anyi.ui.search

import android.os.Bundle
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.soft.anyi.data.AppItem
import com.soft.anyi.data.SoftRepo
import com.soft.anyi.ui.common.AppAction
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.ui.home.AppAdapter
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/** 搜索页（对应 AppSearchPage：即时搜索 + 结果列表） */
class AppSearchActivity : AppCompatActivity() {

    private var input: EditText? = null
    private lateinit var adapter: AppAdapter
    private lateinit var resultBox: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(R.color.page_bg))
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 12), Ui.dp(ctx, 16), 0)
        }

        input = EditText(ctx).apply {
            hint = "搜索软件"
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            singleLine = true
            background = ctx.getDrawable(R.drawable.bg_card)
            setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 10), Ui.dp(ctx, 14), Ui.dp(ctx, 10))
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
            }
        }
        root.addView(input, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = Ui.dp(ctx, 12) })

        resultBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val list = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            clipToPadding = false
            setPadding(0, 0, 0, Ui.dp(ctx, 32))
        }
        adapter = AppAdapter { AppAction.open(ctx, it) }
        list.adapter = adapter
        resultBox.addView(list, LinearLayout.LayoutParams(-1, -2))

        val scroll = androidx.core.widget.NestedScrollView(ctx).apply { addView(resultBox) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        supportActionBar?.title = "搜索"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun doSearch() {
        val k = input?.text?.toString()?.trim() ?: ""
        if (k.isEmpty()) return
        lifecycleScope.launch {
            try {
                val apps = SoftRepo.apps(keyword = k, force = true)
                adapter.submit(apps)
                if (apps.isEmpty()) Toasts.s("没有找到相关软件")
            } catch (e: Exception) {
                Toasts.e("搜索失败：${e.message}")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
