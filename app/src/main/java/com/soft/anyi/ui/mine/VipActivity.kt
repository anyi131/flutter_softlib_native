package com.soft.anyi.ui.mine

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.PayRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/**
 * 会员购买页（对应 vip_page）：套餐卡片 + 支付方式 + 创建订单轮询。
 */
class VipActivity : AppCompatActivity() {

    private lateinit var box: LinearLayout
    private var plans: List<PayRepo.Plan> = emptyList()
    private var selected = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val scroll = ScrollView(ctx).apply { setBackgroundColor(ctx.getColor(R.color.page_bg)) }
        box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 40))
        }
        scroll.addView(box)
        setContentView(scroll)
        supportActionBar?.title = "开通会员"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        load()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun load() {
        lifecycleScope.launch {
            try {
                val p = PayRepo.plans()
                plans = p.first
                render()
            } catch (e: Exception) { Toasts.e(e.message ?: "加载失败") }
        }
    }

    private fun render() {
        box.removeAllViews()
        val ctx = this
        box.addView(TextView(ctx).apply {
            text = "选择套餐"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ctx.getColor(R.color.t1))
        })
        plans.forEachIndexed { i, p ->
            val sel = i == selected
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = ctx.getDrawable(if (sel) R.drawable.bg_pill_on else R.drawable.bg_card)
                setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 14), Ui.dp(ctx, 16), Ui.dp(ctx, 14))
                addView(TextView(ctx).apply {
                    text = p.name; textSize = 16f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(0xFFFFFFFF.toInt())
                })
                addView(TextView(ctx).apply {
                    text = "¥${p.price} · ${p.days} 天"
                    textSize = 13f; setTextColor(0xCCFFFFFF.toInt())
                }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(ctx, 4) })
                setOnClickListener { selected = i; render() }
            }
            box.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 10) })
        }
        box.addView(TextView(ctx).apply {
            text = "立即支付 ¥${plans.getOrNull(selected)?.price ?: ""}"
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundResource(R.drawable.bg_pill_on)
            setPadding(0, Ui.dp(ctx, 13), 0, Ui.dp(ctx, 13))
            textSize = 16f
            setOnClickListener { pay() }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 20) })
    }

    private fun pay() {
        val p = plans.getOrNull(selected) ?: return
        lifecycleScope.launch {
            try {
                val order = PayRepo.create(p.id, "wxpay")
                val url = order.optString("pay_url", order.optString("url"))
                if (url.isEmpty()) { Toasts.e("未获取到支付链接"); return@launch }
                com.soft.anyi.util.ApkInstaller.openBrowser(url)
                // 轮询订单状态（最多 60 次 × 3s）
                var ok = false
                for (i in 1..60) {
                    kotlinx.coroutines.delay(3000)
                    val orderNo = order.optString("order_no", order.optString("orderid"))
                    if (PayRepo.paid(orderNo)) { ok = true; break }

                }
                if (ok) { Toasts.s("支付成功，会员已开通"); finish() }
                else Toasts.e("支付超时或未完成")
            } catch (e: Exception) { Toasts.e(e.message ?: "支付失败") }
        }
    }
}
