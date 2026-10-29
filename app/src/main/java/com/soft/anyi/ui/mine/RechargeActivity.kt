package com.soft.anyi.ui.mine

import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.PayRepo
import com.soft.anyi.data.Session
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/** 余额充值（对应 mine/recharge.dart：固定档位 + 支付下单） */
class RechargeActivity : AppCompatActivity() {
    private lateinit var ctx: android.content.Context
    private var picked = 0
    private val plans = listOf(6, 18, 30, 68, 128, 328)
    private lateinit var grid: android.widget.GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = this
        setTitle("余额充值")

        val root = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.page_bg))
            setPadding(Ui.dp(this@RechargeActivity, 12), Ui.dp(this@RechargeActivity, 12), Ui.dp(this@RechargeActivity, 12), Ui.dp(this@RechargeActivity, 24))
        }
        root.addView(box)
        setContentView(root)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.bg_card)
            setPadding(Ui.dp(this@RechargeActivity, 16), Ui.dp(this@RechargeActivity, 16), Ui.dp(this@RechargeActivity, 16), Ui.dp(this@RechargeActivity, 16))
        }
        card.addView(TextView(this).apply {
            text = "选择充值金额"
            textSize = 15f; setTextColor(getColor(R.color.t1)); setTypeface(typeface, Typeface.BOLD)
        })
        grid = android.widget.GridLayout(this).apply {
            columnCount = 3
            useDefaultMargins = true
        }
        card.addView(grid, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@RechargeActivity, 10) })

        val btn = TextView(this).apply {
            text = "立即充值"; gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE); textSize = 15f; setTypeface(typeface, Typeface.BOLD)
            background = getDrawable(R.drawable.bg_btn_primary)
            setPadding(0, Ui.dp(this@RechargeActivity, 12), 0, Ui.dp(this@RechargeActivity, 12))
            setOnClickListener { pay() }
        }
        card.addView(btn, LinearLayout.LayoutParams(-1, Ui.dp(this@RechargeActivity, 44)).apply { topMargin = Ui.dp(this@RechargeActivity, 12) })
        box.addView(card)

        renderPlans()
    }

    private fun renderPlans() {
        grid.removeAllViews()
        plans.forEach { p ->
            val on = p == picked
            val tv = TextView(this).apply {
                text = "¥$p"
                gravity = android.view.Gravity.CENTER
                textSize = 16f; setTypeface(typeface, Typeface.BOLD)
                setTextColor(getColor(if (on) android.R.color.white else R.color.t1))
                background = getDrawable(if (on) R.drawable.bg_pill_on else R.drawable.bg_card)
                setPadding(0, Ui.dp(this@RechargeActivity, 16), 0, Ui.dp(this@RechargeActivity, 16))
                setOnClickListener {
                    picked = p; renderPlans()
                }
            }
            val lp = android.widget.GridLayout.LayoutParams(
                android.widget.GridLayout.spec(GridLayout.UNDEFINED, 1f),
                android.widget.GridLayout.spec(GridLayout.UNDEFINED)
            )
            lp.width = 0; lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            tv.layoutParams = lp
            grid.addView(tv)
        }
    }

    private fun pay() {
        if (picked <= 0) { Toasts.s("请选择金额"); return }
        lifecycleScope.launch {
            try {
                val order = PayRepo.recharge(picked.toString(), "wxpay")
                if (order.payUrl.isNotEmpty()) {
                    com.soft.anyi.util.ApkInstaller.openBrowser(order.payUrl)
                } else {
                    Toasts.s("下单失败")
                }
            } catch (e: Exception) {
                Toasts.e(e.message ?: "下单失败")
            }
        }
    }
}
