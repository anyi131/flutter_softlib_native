package com.soft.anyi.ui.mine

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.UserRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Sfx
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/**
 * 我的-二级页集合：签到 / 积分商城 / 卡密 / 邀请 / 下载记录 / 消息。
 * 用 mode 区分，复用一套布局骨架（列表型）。
 */
class MineSubActivity : AppCompatActivity() {

    companion object {
        const val MODE_SIGN = "sign"
        const val MODE_SHOP = "shop"       // 积分商城（兑换）
        const val MODE_CARDS = "cards"     // 卡密商店
        const val MODE_INVITE = "invite"
        const val MODE_DL = "download"
        const val MODE_MSG = "message"
    }

    private lateinit var box: LinearLayout
    private var mode = MODE_SIGN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = intent.getStringExtra("mode") ?: MODE_SIGN
        val ctx = this
        val scroll = ScrollView(ctx).apply { setBackgroundColor(ctx.getColor(R.color.page_bg)) }
        box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 40))
        }
        scroll.addView(box)
        setContentView(scroll)
        supportActionBar?.title = titleOf(mode)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        load()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun titleOf(m: String) = when (m) {
        MODE_SIGN -> "每日签到"; MODE_SHOP -> "积分商城"; MODE_CARDS -> "卡密商店"
        MODE_INVITE -> "邀请好友"; MODE_DL -> "下载记录"; else -> "消息中心"
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                when (mode) {
                    MODE_SIGN -> doSign()
                    MODE_SHOP -> renderGoods(UserRepo.exchangeGoods()) { g -> UserRepo.exchange(g.name) }
                    MODE_CARDS -> renderCards()
                    MODE_INVITE -> renderInvite()
                    MODE_DL -> renderDownloads()
                    MODE_MSG -> renderMessages()
                }
            } catch (e: Exception) {
                Toasts.e(e.message ?: "加载失败")
            }
        }
    }

    private fun doSign() {
        lifecycleScope.launch {
            try {
                val msg = UserRepo.sign()
                Sfx.play("sign")
                Toasts.s(msg)
                box.removeAllViews()
                box.addView(center("今日已签到 ✓"))
            } catch (e: Exception) { Toasts.e(e.message ?: "签到失败") }
        }
    }

    private fun center(t: String) = TextView(this).apply {
        text = t; gravity = Gravity.CENTER; textSize = 16f
        setTextColor(getColor(R.color.t1))
    }

    private fun card(title: String, sub: String, action: String? = null, onClick: (() -> Unit)? = null): View {
        val ctx = this
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ctx.getDrawable(R.drawable.bg_card)
            setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 12))
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(ctx).apply {
                    text = title; textSize = 15f; setTextColor(ctx.getColor(R.color.t1))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                })
                if (sub.isNotEmpty()) addView(TextView(ctx).apply {
                    text = sub; textSize = 12f; setTextColor(ctx.getColor(R.color.t2))
                }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(ctx, 3) })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            if (action != null) addView(TextView(ctx).apply {
                text = action
                setTextColor(ctx.getColor(R.color.brand))
                setOnClickListener { onClick?.invoke() }
            })
        }.also { v ->
            (v.layoutParams ?: LinearLayout.LayoutParams(-1, -2)).let { p ->
                p.marginTop = Ui.dp(ctx, 8); v.layoutParams = p
            }
        }
    }

    private fun renderGoods(list: List<UserRepo.Goods>, buy: suspend (UserRepo.Goods) -> String) {
        box.removeAllViews()
        box.addView(center("当前积分：${com.soft.anyi.data.Session.user()?.score ?: 0}"))
        if (list.isEmpty()) { box.addView(center("暂无可兑换商品")); return }
        list.forEach { g ->
            box.addView(card(g.name, "需要 ${g.price} 积分 · 剩余 ${g.stock}", "兑换") {
                lifecycleScope.launch {
                    try { Toasts.s(buy(g)) } catch (e: Exception) { Toasts.e(e.message ?: "兑换失败") }
                }
            })
        }
    }

    private fun renderCards() {
        lifecycleScope.launch {
            try {
                val (cards, goods) = UserRepo.cardShop()
                box.removeAllViews()
                cards.forEach { c ->
                    box.addView(card(c.name, "时长 ${c.days} 天 · ${c.code}", if (c.used) "已用" else "激活") {
                        if (!c.used) lifecycleScope.launch {
                            try { Toasts.s(UserRepo.redeem(c.code)) } catch (e: Exception) { Toasts.e(e.message ?: "失败") }
                        }
                    })
                }
                goods.forEach { g ->
                    box.addView(card(g.name, "售价 ${g.price} 积分 · 剩余 ${g.stock}", "购买") {
                        lifecycleScope.launch {
                            try { Toasts.s(UserRepo.cardBuy(g.id)) } catch (e: Exception) { Toasts.e(e.message ?: "失败") }
                        }
                    })
                }
                if (cards.isEmpty() && goods.isEmpty()) box.addView(center("商店暂未开放"))
            } catch (e: Exception) { Toasts.e(e.message ?: "加载失败") }
        }
    }

    private fun renderInvite() {
        lifecycleScope.launch {
            try {
                val s = UserRepo.inviteStats()
                box.removeAllViews()
                box.addView(center("邀请码：${s.code}"))
                box.addView(center("已邀请 ${s.invites} 人 · 获得积分 ${s.score}"))
                box.addView(card("复制邀请码", "好友注册时填写，双方都得积分", "复制") {
                    val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("invite", s.code))
                    Toasts.s("已复制")
                })
            } catch (e: Exception) { Toasts.e(e.message ?: "加载失败") }
        }
    }

    private fun renderDownloads() {
        lifecycleScope.launch {
            try {
                // 下载记录来自本地 FavStore（App 端不拉后台日志，与 Flutter 版口径一致）
                val ids = com.soft.anyi.util.FavStore.historyIds()
                box.removeAllViews()
                if (ids.isEmpty()) { box.addView(center("暂无浏览记录")); return@launch }
                ids.forEach { id ->
                    box.addView(card("软件 #$id", "点击查看详情", "打开") {
                        com.soft.anyi.ui.details.DetailsActivity.start(this@MineSubActivity, id)
                    })
                }
            } catch (e: Exception) { Toasts.e(e.message ?: "加载失败") }
        }
    }

    private fun renderMessages() {
        lifecycleScope.launch {
            try {
                val list = com.soft.anyi.data.MessageRepo.list()
                com.soft.anyi.data.MessageRepo.readAll()
                box.removeAllViews()
                if (list.isEmpty()) { box.addView(center("暂无消息")); return@launch }
                list.forEach { m ->
                    box.addView(card(
                        m.optString("title"),
                        m.optString("content"),
                        null, null
                    ))
                }
            } catch (e: Exception) { Toasts.e(e.message ?: "加载失败") }
        }
    }
}
