package com.soft.anyi.ui.details

import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.ApiHost
import com.soft.anyi.data.AppItem
import com.soft.anyi.data.Session
import com.soft.anyi.data.SoftRepo
import com.soft.anyi.ui.common.Pager
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.ui.mine.VipActivity
import com.soft.anyi.util.ApkInstaller
import com.soft.anyi.util.FavStore
import com.soft.anyi.util.Prefs
import com.soft.anyi.util.Sfx
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 软件详情页（对应 app_details_page）：
 * 头图海报 / 图标+标题+标签 / 下载卡 / 介绍 / 截图横滑 / 评论。
 */
class DetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "id"
        fun start(ctx: android.content.Context, id: Int) {
            ctx.startActivity(Intent(ctx, DetailsActivity::class.java).putExtra(EXTRA_ID, id))
        }
    }

    private lateinit var box: LinearLayout
    private var app: AppItem? = null
    private var downloading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val ctx = this
        val scroll = ScrollView(ctx).apply { setBackgroundColor(ctx.getColor(R.color.page_bg)) }
        box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 14), Ui.dp(ctx, 14), Ui.dp(ctx, 40))
        }
        scroll.addView(box)
        setContentView(scroll)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        load(id)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun load(id: Int) {
        lifecycleScope.launch {
            try {
                app = SoftRepo.detail(id)
                SoftRepo.view(id) // 浏览计数
                render()
            } catch (e: Exception) {
                Toasts.e("加载失败：${e.message}")
            }
        }
    }

    private fun render() {
        val ctx = this
        val a = app ?: return
        box.removeAllViews()

        // ── 头部：图标 + 名称 + 版本/大小 ──
        val head = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = ImageView(ctx).apply {
            setImageResource(R.mipmap.ic_launcher)
            setBackgroundResource(R.drawable.bg_icon)
            clipToOutline = true
            Ui.dp(ctx, 64).let { s ->
                layoutParams = LinearLayout.LayoutParams(s, s)
                (layoutParams as LinearLayout.LayoutParams).marginEnd = Ui.dp(ctx, 12)
            }
        }
        io.coil_image.CoilImage.load(ctx, a.icon, icon)
        head.addView(icon)
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(ctx).apply {
                text = a.title; textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ctx.getColor(R.color.t1))
            })
            addView(TextView(ctx).apply {
                text = listOfNotNull(
                    a.version.takeIf { it.isNotEmpty() },
                    a.size.takeIf { it.isNotEmpty() },
                    a.developer.takeIf { it.isNotEmpty() }
                ).joinToString(" · ")
                textSize = 12f; setTextColor(ctx.getColor(R.color.t2))
            }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(ctx, 4) })
        }
        head.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        // 收藏
        val favIcon = TextView(ctx).apply {
            textSize = 22f
            text = if (FavStore.isFav(a.id)) "♥" else "♡"
            setTextColor(ctx.getColor(if (FavStore.isFav(a.id)) R.color.brand else R.color.t2))
            setPadding(Ui.dp(ctx, 10), 0, Ui.dp(ctx, 10), 0)
            setOnClickListener {
                val now = FavStore.toggleFav(a.id)
                text = if (now) "♥" else "♡"
                setTextColor(ctx.getColor(if (now) R.color.brand else R.color.t2))
                Toasts.s(if (now) "已收藏" else "已取消收藏")
            }
        }
        head.addView(favIcon)
        box.addView(head)

        // ── 状态标签行 ──
        val vipOnly = a.isVipItem && !Session.isAdmin()
        val tagText = a.priceTag
            .let { if (it == "免费") "免费下载" else if (it == "会员") "会员专享" else "$it 购买" }
        box.addView(TextView(ctx).apply {
            text = tagText
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundResource(R.drawable.bg_pill_on)
            setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 4), Ui.dp(ctx, 10), Ui.dp(ctx, 4))
        }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(ctx, 12) })

        // ── 下载按钮 ──
        val btn = TextView(ctx).apply {
            text = if (downloading) "正在解析…" else "立即下载"
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundResource(R.drawable.bg_pill_on)
            setPadding(0, Ui.dp(ctx, 14), 0, Ui.dp(ctx, 14))
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setOnClickListener { download() }
        }
        box.addView(btn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 16) })

        // ── 介绍 ──
        if (a.description.isNotEmpty()) {
            section("应用介绍")
            box.addView(TextView(ctx).apply {
                text = a.description
                textSize = 14f
                setTextColor(ctx.getColor(R.color.t1))
                setLineSpacing(Ui.dp(ctx, 3).toFloat(), 1f)
            })
        }

        // ── 截图 ──
        if (a.screenshots.isNotEmpty()) {
            section("应用截图")
            val h = HorizontalScrollView(ctx).apply { isHorizontalScrollBarEnabled = false }
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            a.screenshots.forEach { url ->
                val iv = ImageView(ctx).apply {
                    adjustViewBounds = true
                    setBackgroundResource(R.drawable.bg_card)
                    clipToOutline = true
                    layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 160), Ui.dp(ctx, 280)).apply {
                        marginEnd = Ui.dp(ctx, 8)
                    }
                    setOnClickListener {
                        PhotoViewer.show(ctx, a.screenshots, a.screenshots.indexOf(url))
                    }
                }
                io.coil_image.CoilImage.load(ctx, ApiHost.proxyImg(url), iv)
                row.addView(iv)
            }
            h.addView(row)
            box.addView(h, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 8) })
        }

        // ── 评论入口 ──
        section("评论 (${a.scoreCount})")
        box.addView(TextView(ctx).apply {
            text = "查看全部评论 / 发表评论"
            setTextColor(ctx.getColor(R.color.brand))
            textSize = 14f
            setOnClickListener { CommentsActivity.start(ctx, a.id) }
        })
    }

    private fun section(t: String) {
        box.addView(TextView(this).apply {
            text = t
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(getColor(R.color.t1))
        }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(this@DetailsActivity, 22) })
    }

    private fun download() {
        val a = app ?: return
        if (downloading) return
        // 会员专享校验
        if (a.isVipItem && !Session.isLoggedIn()) {
            Toasts.s("该资源为会员专享，请先开通会员")
            startActivity(Intent(this, VipActivity::class.java))
            return
        }
        downloading = true
        Sfx.play("download")
        lifecycleScope.launch {
            try {
                val url = a.downloadTarget
                if (url.isEmpty()) throw com.soft.anyi.data.Http.ApiException(-1, "未解析到直链")
                com.soft.anyi.data.UserRepo.downloadLog(a.title, a.id)
                val dlId = ApkInstaller.enqueue(url, a.title)
                Toasts.s("已加入下载队列")
                var last = -1
                while (true) {
                    val st = ApkInstaller.query(dlId)
                    if (st.progress != last) { last = st.progress; }
                    if (st.status == DownloadManager.STATUS_SUCCESSFUL) { st.fileUri?.let { ApkInstaller.installFromUri(this@DetailsActivity, it) }; break }
                    if (st.status == DownloadManager.STATUS_FAILED) throw com.soft.anyi.data.Http.ApiException(-1, "下载失败")
                    delay(800)
                }
                Toasts.s("已开始下载")
            } catch (e: Exception) {
                Toasts.e(e.message ?: "下载失败")
            } finally {
                downloading = false
                render()
            }
        }
    }
}
