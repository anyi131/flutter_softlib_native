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
import com.soft.anyi.data.UserRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/** 卡密商店：兑换会员/余额（对应 mine/cards.dart 的 card_buy / my_cards） */
class CardShopActivity : AppCompatActivity() {
    private lateinit var ctx: android.content.Context
    private lateinit var input: EditText
    private lateinit var listBox: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = this
        setTitle("卡密兑换")

        val root = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.page_bg))
            setPadding(Ui.dp(this@CardShopActivity, 12), Ui.dp(this@CardShopActivity, 12), Ui.dp(this@CardShopActivity, 12), Ui.dp(this@CardShopActivity, 24))
        }
        root.addView(box)
        setContentView(root)

        // 兑换卡片
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.bg_card)
            setPadding(Ui.dp(this@CardShopActivity, 16), Ui.dp(this@CardShopActivity, 16), Ui.dp(this@CardShopActivity, 16), Ui.dp(this@CardShopActivity, 16))
        }
        card.addView(TextView(this).apply {
            text = "输入卡密兑换"
            textSize = 15f; setTextColor(getColor(R.color.t1)); setTypeface(typeface, Typeface.BOLD)
        })
        input = EditText(this).apply {
            hint = "请输入卡密"; textSize = 14f
            setSingleLine(true)
            background = getDrawable(R.drawable.bg_input)
            setPadding(Ui.dp(this@CardShopActivity, 12), Ui.dp(this@CardShopActivity, 10), Ui.dp(this@CardShopActivity, 12), Ui.dp(this@CardShopActivity, 10))
        }
        card.addView(input, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@CardShopActivity, 10) })
        val btn = TextView(this).apply {
            text = "立即兑换"; gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE); textSize = 15f; setTypeface(typeface, Typeface.BOLD)
            background = getDrawable(R.drawable.bg_btn_primary)
            setPadding(0, Ui.dp(this@CardShopActivity, 12), 0, Ui.dp(this@CardShopActivity, 12))
            setOnClickListener { buy() }
        }
        card.addView(btn, LinearLayout.LayoutParams(-1, Ui.dp(this@CardShopActivity, 44)).apply { topMargin = Ui.dp(this@CardShopActivity, 12) })
        box.addView(card)

        // 我的卡密记录
        box.addView(Ui.sectionTitle(this, "兑换记录"), LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@CardShopActivity, 16) })
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(listBox)
        loadMine()
    }

    private fun buy() {
        val code = input.text.toString().trim()
        if (code.isEmpty()) { Toasts.s("请输入卡密"); return }
        lifecycleScope.launch {
            try {
                val msg = UserRepo.cardBuy(code)
                Toasts.s(msg)
                input.setText("")
                loadMine()
            } catch (e: Exception) {
                Toasts.e(e.message ?: "兑换失败")
            }
        }
    }

    private fun loadMine() {
        lifecycleScope.launch {
            try {
                val arr = UserRepo.myCards()
                render(arr)
            } catch (_: Exception) {
                render(emptyList())
            }
        }
    }

    private fun render(list: List<org.json.JSONObject>) {
        listBox.removeAllViews()
        if (list.isEmpty()) {
            listBox.addView(Ui.emptyView(this, "暂无兑换记录"))
            return
        }
        for (o in list) {
            listBox.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = getDrawable(R.drawable.bg_card)
                setPadding(Ui.dp(this@CardShopActivity, 14), Ui.dp(this@CardShopActivity, 12), Ui.dp(this@CardShopActivity, 14), Ui.dp(this@CardShopActivity, 12))
                addView(TextView(this@CardShopActivity).apply {
                    text = o.optString("title", o.optString("name", "卡密"))
                    textSize = 14f; setTextColor(getColor(R.color.t1)); setTypeface(typeface, Typeface.BOLD)
                })
                addView(TextView(this@CardShopActivity).apply {
                    text = listOf(o.optString("createtime", ""), o.optString("status_text", "")).filter { it.isNotEmpty() }.joinToString(" · ")
                    textSize = 11f; setTextColor(getColor(R.color.t3))
                }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@CardShopActivity, 4) })
            }, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = Ui.dp(this@CardShopActivity, 8)
            })
        }
    }
}
