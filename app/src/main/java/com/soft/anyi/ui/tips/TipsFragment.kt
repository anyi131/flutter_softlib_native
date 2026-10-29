package com.soft.anyi.ui.tips

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.soft.anyi.R
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/**
 * 线报页（对应 TipsPage：线报/热榜聚合）。
 * 数据源走 /api/softlib/toolbox proxy（newsnow 热榜）+ 站内爆料。
 */
class TipsFragment : Fragment() {

    data class Tip(val title: String, val url: String, val source: String, val time: String)

    private val tips = mutableListOf<Tip>()
    private lateinit var adapter: TipAdapter
    private lateinit var refresh: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(R.color.page_bg))
        }
        root.addView(TextView(ctx).apply {
            text = "  线报"
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ctx.getColor(R.color.t1))
            setPadding(0, Ui.dp(ctx, 12), 0, Ui.dp(ctx, 8))
        })

        refresh = SwipeRefreshLayout(ctx).apply { setColorSchemeColors(ctx.getColor(R.color.brand)) }
        val list = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            setPadding(0, 0, 0, Ui.dp(ctx, 72))
            clipToPadding = false
        }
        adapter = TipAdapter { tip ->
            com.soft.anyi.util.ApkInstaller.openBrowser(tip.url)
        }
        list.adapter = adapter
        refresh.addView(list, ViewGroup.LayoutParams(-1, -1))
        root.addView(refresh, LinearLayout.LayoutParams(-1, 0, 1f))
        refresh.setOnRefreshListener { load(true) }
        load(false)
        return root
    }

    private fun load(force: Boolean) {
        refresh.isRefreshing = tips.isEmpty()
        lifecycleScope.launch {
            try {
                // newsnow 热榜（服务端聚合）
                val d = ToolboxRepo.media("newsnow") ?: ToolboxRepo.box2("newsnow")
                tips.clear()
                val arr = d?.optJSONArray("list")
                    ?: d?.optJSONArray("data")
                    ?: d?.optJSONArray("items")
                arr?.let { a ->
                    (0 until a.length()).forEach { n ->
                        val o = a.optJSONObject(n) ?: return@forEach
                        tips.add(
                            Tip(
                                o.optString("title"),
                                o.optString("url"),
                                o.optString("source", "热榜"),
                                o.optString("time", "")
                            )
                        )
                    }
                }
                adapter.notifyDataSetChanged()
                if (tips.isEmpty()) Toasts.s("暂无线报")
            } catch (e: Exception) {
                Toasts.e("加载失败：${e.message}")
            } finally {
                refresh.isRefreshing = false
            }
        }
    }
}

class TipAdapter(private val onClick: (TipsFragment.Tip) -> Unit) :
    RecyclerView.Adapter<TipAdapter.VH>() {
    private val data = mutableListOf<TipsFragment.Tip>()
    fun submit(list: List<TipsFragment.Tip>) { data.clear(); data.addAll(list); notifyDataSetChanged() }
    class VH(val root: LinearLayout) : RecyclerView.ViewHolder(root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = ctx.getDrawable(R.drawable.bg_card)
            elevation = 2f
            setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 12))
            layoutParams = RecyclerView.LayoutParams(-1, -2).apply {
                marginStart = Ui.dp(ctx, 12); marginEnd = Ui.dp(ctx, 12); topMargin = Ui.dp(ctx, 6)
            }
        }
        return VH(row)
    }
    override fun onBindViewHolder(h: VH, pos: Int) {
        val t = data[pos]
        val ctx = h.root.context
        h.root.removeAllViews()
        h.root.addView(TextView(ctx).apply {
            text = t.title
            textSize = 15f
            setTextColor(ctx.getColor(R.color.t1))
            maxLines = 2
        })
        h.root.addView(TextView(ctx).apply {
            text = "${t.source}  ${t.time}"
            textSize = 12f
            setTextColor(ctx.getColor(R.color.t2))
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(ctx, 4) }
        })
        h.root.setOnClickListener { onClick(t) }
    }
    override fun getItemCount() = data.size
}
