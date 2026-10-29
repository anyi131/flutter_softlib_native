package com.soft.anyi.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.soft.anyi.R
import com.soft.anyi.data.AppCat
import com.soft.anyi.data.AppItem
import com.soft.anyi.data.SoftRepo
import com.soft.anyi.ui.common.AppAction
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.ui.details.AppDetailsActivity
import com.soft.anyi.ui.search.AppSearchActivity
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/**
 * 首页（对应 HomeComponent）：
 * 标题栏 + 搜索入口 + 分类胶囊 + 软件列表（置顶/新标记/评分/下载量）。
 */
class HomeFragment : Fragment() {

    private var cats: List<AppCat> = emptyList()
    private var currentCat = 0
    private var apps: List<AppItem> = emptyList()
    private var loading = false

    private lateinit var list: RecyclerView
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var catRow: LinearLayout
    private lateinit var adapter: AppAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(R.color.page_bg))
        }

        // ── 标题栏 ──
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 12), Ui.dp(ctx, 16), Ui.dp(ctx, 8))
            addView(TextView(ctx).apply {
                text = "首页"
                textSize = 24f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ctx.getColor(R.color.t1))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(ctx).apply {
                text = "🔍"
                textSize = 20f
                setPadding(Ui.dp(ctx, 8), Ui.dp(ctx, 8), 0, Ui.dp(ctx, 8))
                setOnClickListener {
                    startActivity(Intent(ctx, AppSearchActivity::class.java))
                }
            })
        }
        root.addView(bar)

        // ── 分类行 ──
        catRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(Ui.dp(ctx, 12), 0, Ui.dp(ctx, 12), Ui.dp(ctx, 4))
        }
        // 横向滚动容器
        val hScroll = android.widget.HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(catRow)
        }
        root.addView(hScroll)

        // ── 列表 ──
        refresh = SwipeRefreshLayout(ctx).apply {
            setColorSchemeColors(ctx.getColor(R.color.brand))
        }
        list = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            setPadding(0, 0, 0, Ui.dp(ctx, 72))
            clipToPadding = false
        }
        adapter = AppAdapter { item -> AppAction.open(requireContext(), item) }
        list.adapter = adapter
        refresh.addView(list, ViewGroup.LayoutParams(-1, -1))
        root.addView(refresh, LinearLayout.LayoutParams(-1, 0, 1f))

        refresh.setOnRefreshListener { load(force = true) }
        load()

        return root
    }

    private fun horizontalScrollViewHost(): Nothing? = null

    private fun load(force: Boolean = false) {
        if (loading) return
        loading = true
        refresh.isRefreshing = apps.isEmpty()
        view?.let { v ->
            if (apps.isEmpty()) {
                // 简易加载占位：复用时先移除旧占位
                (v as? ViewGroup)?.let { vg ->
                    vg.findViewWithTag<View>("loading")?.let { vg.removeView(it) }
                    val lv = Ui.loadingView(v.context)
                    lv.tag = "loading"
                    vg.addView(lv, LinearLayout.LayoutParams(-1, -2).apply {
                        topMargin = Ui.dp(v.context, 120)
                    })
                }
            }
        }
        lifecycleScope.launch {
            try {
                launch { cats = SoftRepo.cats(force) }
                launch { cats = SoftRepo.cats(force) }
                apps = SoftRepo.apps(currentCat, force = force)
                render()
            } catch (e: Exception) {
                Toasts.e("加载失败：${e.message}")
            } finally {
                loading = false
                refresh.isRefreshing = false
            }
        }
    }

    private fun render() {
        val ctx = context ?: return
        // 分类胶囊
        catRow.removeAllViews()
        val chips = mutableListOf<Pair<Int, String>>()
        chips.add(0 to "全部")
        cats.forEach { chips.add(it.id to it.title) }
        chips.forEach { (id, title) ->
            val on = id == currentCat
            catRow.addView(TextView(ctx).apply {
                text = title
                textSize = 13f
                setTextColor(ctx.getColor(if (on) android.R.color.white else R.color.t2))
                background = ctx.getDrawable(if (on) R.drawable.bg_pill_on else R.drawable.bg_pill_off)
                setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 6), Ui.dp(ctx, 14), Ui.dp(ctx, 6))
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginEnd = Ui.dp(ctx, 8) }
                setOnClickListener {
                    if (currentCat != id) { currentCat = id; load(force = true) }
                }
            })
        }
        adapter.submit(apps)
    }
}

/** 软件列表适配器（对应 AppCard：图标/标题/版本/大小/下载量/评分/新&顶角标） */
class AppAdapter(private val onClick: (AppItem) -> Unit) :
    RecyclerView.Adapter<AppAdapter.VH>() {

    private val data = mutableListOf<AppItem>()

    fun submit(items: List<AppItem>) {
        data.clear(); data.addAll(items); notifyDataSetChanged()
    }

    class VH(val card: LinearLayout) : RecyclerView.ViewHolder(card)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val d = ctx.resources.displayMetrics.density
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            elevation = 2f
            background = ctx.getDrawable(R.drawable.bg_card)
            setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 12), Ui.dp(ctx, 12), Ui.dp(ctx, 12))
            layoutParams = RecyclerView.LayoutParams(-1, -2).apply {
                marginStart = Ui.dp(ctx, 12); marginEnd = Ui.dp(ctx, 12)
                topMargin = Ui.dp(ctx, 6)
            }
        }
        return VH(card)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = data[pos]
        val ctx = h.card.context
        h.card.removeAllViews()
        val d = ctx.resources.displayMetrics.density

        // 图标
        val iv = ImageView(ctx).apply {
            setImageResource(R.mipmap.ic_launcher)
            background = ctx.getDrawable(R.drawable.bg_icon)
            clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 52), Ui.dp(ctx, 52))
            scaleType = ImageView.ScaleType.CENTER_CROP
            load(if (item.icon.startsWith("http")) item.icon
                else com.soft.anyi.data.ApiHost.BASE + item.icon) {
                crossfade(true)
                error(R.mipmap.ic_launcher)
            }
        }
        h.card.addView(iv)

        // 文本区
        val texts = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = Ui.dp(ctx, 12)
            }
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(ctx).apply {
                    text = item.title
                    textSize = 16f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(ctx.getColor(R.color.t1))
                    maxLines = 1
                })
                if (item.isNew) addView(badge(ctx, "新", "#E53935"))
                if (item.isTop) addView(badge(ctx, "顶", "#6750A4"))
                if (item.isVipItem) addView(badge(ctx, "VIP", "#F5A623"))
            })
            addView(TextView(ctx).apply {
                text = "v${item.version} · ${item.size} · ${item.downCount}次下载"
                textSize = 12f
                setTextColor(ctx.getColor(R.color.t2))
                maxLines = 1
            })
            if (item.scoreCount > 0) {
                addView(TextView(ctx).apply {
                    text = "★ ${"%.1f".format(item.scoreAvg)} (${item.scoreCount}人评分)"
                    textSize = 12f
                    setTextColor(0xFFF5A623.toInt())
                })
            }
        }
        h.card.addView(texts)

        h.card.setOnClickListener { onClick(item) }
    }

    private fun badge(ctx: android.content.Context, text: String, color: String): TextView =
        TextView(ctx).apply {
            this.text = text
            textSize = 9f
            setTextColor(android.graphics.Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor(color))
                cornerRadius = Ui.dp(ctx, 4).toFloat()
            }
            setPadding(Ui.dp(ctx, 4), 0, Ui.dp(ctx, 4), 0)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginStart = Ui.dp(ctx, 6) }
        }

    override fun getItemCount() = data.size
}
