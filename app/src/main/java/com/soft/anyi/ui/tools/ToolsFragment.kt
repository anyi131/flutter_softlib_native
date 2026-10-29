package com.soft.anyi.ui.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.soft.anyi.R
import com.soft.anyi.data.ToolboxRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 工具页（对应 ToolHome）：
 * 分类分组 + 工具九宫格 + 搜索；点击跳 ToolPageActivity / ToolBrowserActivity / PlayerActivity。
 */
class ToolsFragment : Fragment() {

    data class Tool(
        val id: Int, val name: String, val icon: String,
        val kind: String, val link: String, val group: String
    ) {
        companion object {
            fun fromJson(o: JSONObject): Tool = Tool(
                o.optInt("id"),
                o.optString("name").ifEmpty { o.optString("title") },
                o.optString("icon"),
                o.optString("kind", "local").ifEmpty { "local" },
                o.optString("link", ""),
                o.optString("group", "").ifEmpty { o.optString("cat", "常用") }
            )
        }
    }

    private var tools: List<Tool> = emptyList()
    private var filtered: List<Tool> = emptyList()
    private lateinit var adapter: ToolAdapter
    private lateinit var box: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(R.color.page_bg))
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 12), Ui.dp(ctx, 16), Ui.dp(ctx, 72))
        }
        val scroll = androidx.core.widget.NestedScrollView(ctx).apply { addView(box) }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(R.color.page_bg))
        }
        // 标题 + 搜索
        val input = EditText(ctx).apply {
            hint = "搜索工具"
            setSingleLine()
            background = ctx.getDrawable(R.drawable.bg_card)
            setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 10), Ui.dp(ctx, 14), Ui.dp(ctx, 10))
            addTextChangedListenerCompat { kw ->
                filtered = if (kw.isNullOrBlank()) tools else tools.filter {
                    it.name.contains(kw.trim(), true)
                }
                render()
            }
        }
        root.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, Ui.dp(ctx, 8))
            addView(TextView(ctx).apply {
                text = "工具"
                textSize = 24f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ctx.getColor(R.color.t1))
            })
            addView(input, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 8) })
        })
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        adapter = ToolAdapter { tool -> openTool(tool) }
        load()
        return root
    }

    private fun EditText.addTextChangedListenerCompat(cb: (String?) -> Unit) {
        addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = cb(s?.toString())
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    private fun openTool(tool: Tool) {
        val ctx = requireContext()
        ToolboxRepo // 语义占位
        when (tool.kind) {
            "web", "webview", "link" -> ctx.startActivity(
                android.content.Intent(ctx, ToolBrowserActivity::class.java)
                    .putExtra("url", tool.link).putExtra("title", tool.name)
            )
            "audio" -> ctx.startActivity(
                android.content.Intent(ctx, PlayerActivity::class.java)
                    .putExtra("title", tool.name).putExtra("mode", "audio").putExtra("url", tool.link)
            )
            "video" -> ctx.startActivity(
                android.content.Intent(ctx, PlayerActivity::class.java)
                    .putExtra("title", tool.name).putExtra("mode", "video").putExtra("url", tool.link)
            )
            else -> ctx.startActivity(
                android.content.Intent(ctx, ToolPageActivity::class.java)
                    .putExtra("tool_id", tool.id).putExtra("name", tool.name)
            )
        }
        lifecycleScope.launch { ToolboxRepo.addHistory(tool.id) }
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val arr = ToolboxRepo.tools()
                tools = arr.map { Tool.fromJson(it) }
                filtered = tools
                render()
            } catch (e: Exception) {
                Toasts.e("工具加载失败：${e.message}")
            }
        }
    }

    private fun render() {
        val ctx = context ?: return
        box.removeAllViews()
        if (filtered.isEmpty()) {
            box.addView(Ui.emptyView(ctx, "暂无工具"))
            return
        }
        // 按组渲染
        val groups = filtered.groupBy { it.group.ifBlank { "常用" } }
        groups.forEach { (group, items) ->
            box.addView(Ui.sectionTitle(ctx, group).apply {
                setPadding(0, Ui.dp(ctx, 12), 0, Ui.dp(ctx, 8))
            })
            val grid = RecyclerView(ctx).apply {
                layoutManager = GridLayoutManager(ctx, 4)
                overScrollMode = View.OVER_SCROLL_NEVER
                isNestedScrollingEnabled = false
            }
            grid.adapter = ToolGridAdapter(items) { openTool(it) }
            box.addView(grid, ViewGroup.LayoutParams(-1, -2))
        }
    }
}

/** 工具九宫格适配器 */
class ToolGridAdapter(
    private val items: List<ToolsFragment.Tool>,
    private val onClick: (ToolsFragment.Tool) -> Unit
) : RecyclerView.Adapter<ToolGridAdapter.VH>() {

    class VH(val root: LinearLayout) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val cell = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(Ui.dp(ctx, 4), Ui.dp(ctx, 10), Ui.dp(ctx, 4), Ui.dp(ctx, 10))
            layoutParams = RecyclerView.LayoutParams(-1, -2)
        }
        return VH(cell)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val t = items[pos]
        val ctx = h.root.context
        h.root.removeAllViews()
        val icon = TextView(ctx).apply {
            text = t.icon.ifBlank { "🧰" }
            textSize = 26f
            gravity = Gravity.CENTER
            background = ctx.getDrawable(R.drawable.bg_icon)
            layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 48), Ui.dp(ctx, 48))
        }
        val name = TextView(ctx).apply {
            text = t.name
            textSize = 12f
            setTextColor(ctx.getColor(R.color.t1))
            gravity = Gravity.CENTER
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(ctx, 6) }
        }
        h.root.addView(icon); h.root.addView(name)
        h.root.setOnClickListener { onClick(t) }
    }

    override fun getItemCount() = items.size
}

/** 工具列表适配器（备用：搜索结果） */
class ToolAdapter(private val onClick: (ToolsFragment.Tool) -> Unit) :
    RecyclerView.Adapter<ToolAdapter.VH>() {
    private val data = mutableListOf<ToolsFragment.Tool>()
    fun submit(list: List<ToolsFragment.Tool>) { data.clear(); data.addAll(list); notifyDataSetChanged() }
    class VH(val root: TextView) : RecyclerView.ViewHolder(root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(TextView(parent.context).apply {
            textSize = 15f
            setPadding(Ui.dp(parent.context, 16), Ui.dp(parent.context, 12), Ui.dp(parent.context, 16), Ui.dp(parent.context, 12))
        })
    override fun onBindViewHolder(h: VH, pos: Int) {
        val t = data[pos]
        h.root.text = "${t.icon} ${t.name}"
        h.root.setOnClickListener { onClick(t) }
    }
    override fun getItemCount() = data.size
}
