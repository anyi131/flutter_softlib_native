package com.soft.anyi.ui.square

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
import coil.load
import com.soft.anyi.R
import com.soft.anyi.data.PostRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Session
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/**
 * 广场页（对应 SquarePage）：帖子流 + 发布入口 + 点赞。
 */
class SquareFragment : Fragment() {

    private val posts = mutableListOf<PostRepo.Post>()
    private var page = 1
    private var hasMore = true
    private lateinit var adapter: PostAdapter
    private lateinit var refresh: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(R.color.page_bg))
        }
        // 顶栏：标题 + 发布
        root.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 12), Ui.dp(ctx, 16), Ui.dp(ctx, 8))
            addView(TextView(ctx).apply {
                text = "广场"
                textSize = 24f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ctx.getColor(R.color.t1))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            addView(TextView(ctx).apply {
                text = "发布"
                textSize = 14f
                setTextColor(ctx.getColor(android.R.color.white))
                background = ctx.getDrawable(R.drawable.bg_pill_on)
                setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 8))
                setOnClickListener {
                    if (Session.requireLogin()) {
                        startActivity(android.content.Intent(ctx, PostCreateActivity::class.java))
                    }
                }
            })
        })

        refresh = SwipeRefreshLayout(ctx).apply { setColorSchemeColors(ctx.getColor(R.color.brand)) }
        val list = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            setPadding(0, 0, 0, Ui.dp(ctx, 72))
            clipToPadding = false
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    val lm = rv.layoutManager as LinearLayoutManager
                    if (hasMore && lm.findLastVisibleItemPosition() >= posts.size - 3) loadMore()
                }
            })
        }
        adapter = PostAdapter(posts) { p ->
            startActivity(android.content.Intent(ctx, PostDetailActivity::class.java).putExtra("post", p.raw))
        }
        list.adapter = adapter
        refresh.addView(list, ViewGroup.LayoutParams(-1, -1))
        root.addView(refresh, LinearLayout.LayoutParams(-1, 0, 1f))
        refresh.setOnRefreshListener { page = 1; load() }
        load()
        return root
    }

    private fun load() {
        refresh.isRefreshing = posts.isEmpty()
        lifecycleScope.launch {
            try {
                val (list, more) = PostRepo.list(page)
                if (page == 1) posts.clear()
                posts.addAll(list)
                hasMore = more
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toasts.e("加载失败：${e.message}")
            } finally {
                refresh.isRefreshing = false
            }
        }
    }

    private fun loadMore() {
        page += 1
        load()
    }
}

class PostAdapter(
    private val data: List<PostRepo.Post>,
    private val onClick: (PostRepo.Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.VH>() {

    class VH(val root: LinearLayout) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = ctx.getDrawable(R.drawable.bg_card)
            elevation = 2f
            setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 12))
            layoutParams = RecyclerView.LayoutParams(-1, -2).apply {
                marginStart = Ui.dp(ctx, 12); marginEnd = Ui.dp(ctx, 12); topMargin = Ui.dp(ctx, 6)
            }
        }
        return VH(card)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = data[pos]
        val ctx = h.root.context
        h.root.removeAllViews()
        // 作者行
        h.root.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(android.widget.ImageView(ctx).apply {
                setImageResource(R.mipmap.ic_launcher)
                background = ctx.getDrawable(R.drawable.bg_icon)
                clipToOutline = true
                layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 34), Ui.dp(ctx, 34))
                load(p.avatar.ifBlank { null }) { crossfade(true) }
            })
            addView(TextView(ctx).apply {
                text = p.nickname
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ctx.getColor(R.color.t1))
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginStart = Ui.dp(ctx, 8) }
            })
            addView(TextView(ctx).apply {
                text = p.time
                textSize = 11f
                setTextColor(ctx.getColor(R.color.t2))
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { marginStart = Ui.dp(ctx, 8) }
            })
        })
        // 正文
        h.root.addView(TextView(ctx).apply {
            text = p.content
            textSize = 15f
            setTextColor(ctx.getColor(R.color.t1))
            maxLines = 6
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(ctx, 8) }
        })
        // 图片（最多 3 张）
        if (p.images.isNotEmpty()) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(ctx, 8) }
            }
            p.images.take(3).forEach { u ->
                row.addView(android.widget.ImageView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 92), Ui.dp(ctx, 92)).apply {
                        marginEnd = Ui.dp(ctx, 6)
                    }
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    clipToOutline = true
                    background = ctx.getDrawable(R.drawable.bg_icon)
                    load(u) { crossfade(true) }
                })
            }
            h.root.addView(row)
        }
        // 点赞/评论
        h.root.addView(TextView(ctx).apply {
            text = "♥ ${p.likes}   💬 ${p.comments}"
            textSize = 12f
            setTextColor(ctx.getColor(R.color.t2))
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(ctx, 8) }
        })
        h.root.setOnClickListener { onClick(p) }
    }

    override fun getItemCount() = data.size
}
