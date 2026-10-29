package com.soft.anyi.ui.home

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.soft.anyi.R
import com.soft.anyi.data.AppItem
import com.soft.anyi.data.SoftRepo
import com.soft.anyi.ui.common.AppAction
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/**
 * 首页（对应 home_page）：
 * 顶部标题+搜索框 → 分类横滑 pill → 软件列表 → 轮播/公告由配置开关控制。
 */
class HomeFragment : Fragment() {

    private lateinit var root: LinearLayout
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var catRow: LinearLayout
    private var listHolder: LinearLayout? = null

    private var cats: List<AppCat> = emptyList()
    private var currentCat = 0
    private var apps: List<AppItem> = emptyList()
    private var loading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(R.color.page_bg))
        }

        // ── 标题 + 搜索入口 ──
        val titleBar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 12), Ui.dp(ctx, 16), Ui.dp(ctx, 8))
            addView(TextView(ctx).apply {
                text = "首页"
                textSize = 24f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ctx.getColor(R.color.t1))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(ctx).apply {
                text = "搜索"
                textSize = 14f
                setTextColor(ctx.getColor(R.color.brand))
                background = ctx.getDrawable(R.drawable.bg_pill_off)
                setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 6), Ui.dp(ctx, 14), Ui.dp(ctx, 6))
                setOnClickListener {
                    startActivity(
                        android.content.Intent(ctx, com.soft.anyi.ui.search.AppSearchActivity::class.java)
                    )
                }
            })
        }
        root.addView(titleBar)

        // ── 分类行（横向滚动） ──
        catRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(Ui.dp(ctx, 12), 0, Ui.dp(ctx, 12), Ui.dp(ctx, 4))
        }
        val catScroll = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(catRow)
        }
        root.addView(catScroll)

        // ── 列表（下拉刷新） ──
        refresh = SwipeRefreshLayout(ctx)
        val list = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        listHolder = list
        refresh.addView(list, ViewGroup.LayoutParams(-1, -1))
        refresh.setOnRefreshListener { load(force = true) }
        root.addView(refresh, LinearLayout.LayoutParams(-1, 0, 1f))

        load()
        return root
    }

    private fun load(force: Boolean = false) {
        if (loading) return
        loading = true
        refresh.isRefreshing = apps.isEmpty()
        lifecycleScope.launch {
            try {
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
        val list = listHolder ?: return
        list.removeAllViews()

        // 分类 pill
        catRow.removeAllViews()
        val all: List<AppCat> = listOf(AppCat(0, "全部")) + cats
        all.forEach { c ->            val on = c.id == currentCat
            catRow.addView(TextView(ctx).apply {
                text = c.title
                textSize = 13f
                setTextColor(ctx.getColor(if (on) R.color.brand else R.color.t2))
                background = ctx.getDrawable(if (on) R.drawable.bg_pill_on else R.drawable.bg_pill_off)
                setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 6), Ui.dp(ctx, 14), Ui.dp(ctx, 6))
                setOnClickListener {
                    if (currentCat == c.id) return@setOnClickListener
                    currentCat = c.id
                    load()
                }
            }, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = Ui.dp(ctx, 8) })
        }

        if (apps.isEmpty()) {
            list.addView(Ui.emptyView(ctx, "暂无软件"))
            return
        }
        apps.forEach { a -> list.addView(appCard(ctx, a)) }
    }

    private fun appCard(ctx: android.content.Context, a: AppItem): View {
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ctx.getDrawable(R.drawable.bg_card)
            setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 10), Ui.dp(ctx, 10), Ui.dp(ctx, 10))
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.topMargin = Ui.dp(ctx, 6)
            lp.marginStart = Ui.dp(ctx, 12)
            lp.marginEnd = Ui.dp(ctx, 12)
            layoutParams = lp
        }
        val iv = ImageView(ctx).apply {
            background = ctx.getDrawable(R.drawable.bg_icon)
            layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 46), Ui.dp(ctx, 46))
        }
        io.coil_image.CoilImage.loadList(ctx, a.icon, iv, Ui.dp(ctx, 46))
        card.addView(iv)

        val mid = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = Ui.dp(ctx, 10)
            }
            addView(TextView(ctx).apply {
                text = a.title
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ctx.getColor(R.color.t1))
                maxLines = 1
            })
            addView(TextView(ctx).apply {
                text = listOfNotNull(
                    a.version.takeIf { it.isNotBlank() },
                    a.sizeText.takeIf { it.isNotBlank() && it != "-" },
                    a.developer.takeIf { it.isNotBlank() }
                ).joinToString(" · ").ifBlank { "" }
                textSize = 11f
                setTextColor(ctx.getColor(R.color.t2))
                maxLines = 1
            }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(ctx, 3) })
        }
        card.addView(mid)

        card.addView(TextView(ctx).apply {
            text = a.priceTag
            textSize = 11f
            setTextColor(ctx.getColor(R.color.brand))
            background = ctx.getDrawable(R.drawable.bg_pill_off)
            setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 3), Ui.dp(ctx, 10), Ui.dp(ctx, 3))
        })

        card.setOnClickListener { AppAction.open(requireContext(), a) }
        return card
    }
}
