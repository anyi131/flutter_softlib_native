package com.soft.anyi.ui.mine

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.soft.anyi.R
import com.soft.anyi.data.ApiHost
import com.soft.anyi.data.Session
import com.soft.anyi.data.UserRepo
import com.soft.anyi.ui.auth.LoginActivity
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/**
 * 我的页（对应 MinePage）：
 * 头像卡（昵称/VIP/积分/余额）+ 功能列表（编辑资料/消息/收藏/历史/邀请/会员/卡密/关于/退出）。
 */
class MineFragment : Fragment() {

    private var unread = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(R.color.page_bg))
        }
        val scroll = androidx.core.widget.NestedScrollView(ctx).apply {
            addView(root, ViewGroup.LayoutParams(-1, -2))
        }
        val host = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(R.color.page_bg))
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        }

        buildUi(ctx, root)
        refresh()
        return host
    }

    private var contentRoot: LinearLayout? = null

    private fun buildUi(ctx: android.content.Context, root: LinearLayout) {
        contentRoot = root
        render()
    }

    private fun refresh() {
        if (!Session.isLoggedIn()) { render(); return }
        lifecycleScope.launch {
            try {
                val u = UserRepo.profile()
                Session.updateUser(u)
            } catch (_: Exception) {
            }
            unread = try { com.soft.anyi.data.MessageRepo.unread() } catch (_: Exception) { 0 }
            render()
        }
    }

    @Suppress("DEPRECATION")
    private fun render() {
        val root = contentRoot ?: return
        val ctx = root.context
        root.removeAllViews()
        val u = Session.user()

        // ── 头像卡 ──
        root.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ctx.getDrawable(R.drawable.bg_card)
            elevation = 3f
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 16))
            val m = Ui.dp(ctx, 16)
            (layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(m, m, m, 0)
            addView(android.widget.ImageView(ctx).apply {
                setImageResource(R.mipmap.ic_launcher)
                background = ctx.getDrawable(R.drawable.bg_icon)
                clipToOutline = true
                layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 56), Ui.dp(ctx, 56))
                u?.avatar?.takeIf { it.startsWith("http") }?.let { io.coil_image.CoilImage.load(ctx, it, this) }
            })
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = Ui.dp(ctx, 12) }
                addView(TextView(ctx).apply {
                    text = u?.nickname?.ifBlank { u?.username } ?: "点击登录"
                    textSize = 18f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(ctx.getColor(R.color.t1))
                })
                addView(TextView(ctx).apply {
                    text = if (u != null)
                        (if (u.isVip) "VIP · ${u.vipExpire}" else "普通用户") +
                            " · 积分 ${u.score} · 余额 ¥${u.money}"
                    else "登录后享受更多功能"
                    textSize = 12f
                    setTextColor(ctx.getColor(R.color.t2))
                })
            })
            setOnClickListener {
                if (u == null) startActivity(Intent(ctx, LoginActivity::class.java))
                else startActivity(Intent(ctx, MineEditActivity::class.java))
            }
        })

        // ── 功能列表 ──
        val items = mutableListOf<Triple<String, String, () -> Unit>>()
        if (u != null) {
            items.add(Triple("✉️", if (unread > 0) "消息 ($unread)" else "消息") {
                startActivity(Intent(ctx, com.soft.anyi.ui.message.MessageActivity::class.java))
            })
            items.add(Triple("⭐", "我的收藏") {
                startActivity(Intent(ctx, com.soft.anyi.ui.fav.FavoritesActivity::class.java))
            })
            items.add(Triple("🕐", "浏览历史") {
                startActivity(Intent(ctx, com.soft.anyi.ui.fav.HistoryActivity::class.java))
            })
            items.add(Triple("🎁", "邀请有礼") {
                startActivity(Intent(ctx, InviteActivity::class.java))
            })
            items.add(Triple("👑", if (u.isVip) "会员中心" else "开通会员") {
                startActivity(Intent(ctx, VipActivity::class.java))
            })
            items.add(Triple("💳", "卡密商店") {
                startActivity(Intent(ctx, CardShopActivity::class.java))
            })
            items.add(Triple("💰", "余额充值") {
                startActivity(Intent(ctx, RechargeActivity::class.java))
            })
            items.add(Triple("📅", "每日签到") { sign() })
            items.add(Triple("🧾", "关于软件") {
                startActivity(Intent(ctx, AboutActivity::class.java))
            })
            items.add(Triple("🚪", "退出登录") {
                Session.logout()
                Toasts.s("已退出")
                render()
            })
        } else {
            items.add(Triple("🔑", "登录 / 注册") {
                startActivity(Intent(ctx, LoginActivity::class.java))
            })
            items.add(Triple("🧾", "关于软件") {
                startActivity(Intent(ctx, AboutActivity::class.java))
            })
        }
        items.forEach { (icon, label, action) ->
            root.addView(row(ctx, icon, label, action))
        }
        root.setPadding(0, 0, 0, Ui.dp(ctx, 72))
    }

    private fun row(ctx: android.content.Context, icon: String, label: String, action: () -> Unit): View =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ctx.getDrawable(R.drawable.bg_card)
            elevation = 2f
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 14), Ui.dp(ctx, 16), Ui.dp(ctx, 14))
            val lp = ViewGroup.MarginLayoutParams(-1, -2)
            lp.setMargins(Ui.dp(ctx, 12), Ui.dp(ctx, 6), Ui.dp(ctx, 12), 0)
            layoutParams = lp
            addView(TextView(ctx).apply { text = icon; textSize = 18f })
            addView(TextView(ctx).apply {
                text = label
                textSize = 15f
                setTextColor(ctx.getColor(R.color.t1))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = Ui.dp(ctx, 12) }
            })
            addView(TextView(ctx).apply { text = "›"; textSize = 16f; setTextColor(ctx.getColor(R.color.t2)) })
            setOnClickListener { action() }
        }

    private fun sign() {
        if (!Session.requireLogin()) return
        lifecycleScope.launch {
            try {
                val msg = UserRepo.sign()
                Toasts.s(msg)
                refresh()
            } catch (e: Exception) {
                Toasts.e(e.message ?: "签到失败")
            }
        }
    }
}
