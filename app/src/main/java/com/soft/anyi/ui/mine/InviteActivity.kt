package com.soft.anyi.ui.mine

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.Session
import com.soft.anyi.data.UserRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/** 邀请返利：我的邀请码 + 复制分享（对应 mine/invite.dart） */
class InviteActivity : AppCompatActivity() {
    private lateinit var ctx: android.content.Context
    private lateinit var codeBox: LinearLayout
    private lateinit var listBox: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = this
        setTitle("邀请返利")

        val root = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.page_bg))
            setPadding(Ui.dp(this@InviteActivity, 12), Ui.dp(this@InviteActivity, 12), Ui.dp(this@InviteActivity, 12), Ui.dp(this@InviteActivity, 24))
        }
        root.addView(box)
        setContentView(root)

        // 邀请码卡片
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.bg_card)
            setPadding(Ui.dp(this@InviteActivity, 16), Ui.dp(this@InviteActivity, 16), Ui.dp(this@InviteActivity, 16), Ui.dp(this@InviteActivity, 16))
        }
        card.addView(TextView(this).apply {
            text = "我的邀请码"
            textSize = 15f; setTextColor(getColor(R.color.t1)); setTypeface(typeface, Typeface.BOLD)
        })
        codeBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        card.addView(codeBox, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@InviteActivity, 8) })
        box.addView(card)

        // 邀请记录
        box.addView(Ui.sectionTitle(this, "邀请记录"), LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@InviteActivity, 16) })
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(listBox)

        load()
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val d = UserRepo.inviteStats().let { st ->
                    org.json.JSONObject().put("invites", st.invites).put("score", st.score).put("invite_code", st.code)
                }
                render(d)
            } catch (e: Exception) {
                Toasts.e(e.message ?: "加载失败")
            }
        }
    }

    private fun render(d: org.json.JSONObject) {
        codeBox.removeAllViews()
        val code = d.optString("code", d.optString("invite_code", "—"))
        codeBox.addView(TextView(this).apply {
            text = code; textSize = 24f; setTextColor(getColor(R.color.brand)); setTypeface(typeface, Typeface.BOLD)
        })
        codeBox.addView(TextView(this).apply {
            text = "点此复制，好友注册后双方得奖励"
            textSize = 12f; setTextColor(getColor(R.color.t2))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@InviteActivity, 4) })
        codeBox.setOnClickListener {
            val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("code", code))
            Toasts.s("已复制")
        }

        listBox.removeAllViews()
        val arr = d.optJSONArray("records") ?: d.optJSONArray("list")
        if (arr == null || arr.length() == 0) {
            listBox.addView(Ui.emptyView(this, "还没有邀请记录"))
            return
        }
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            listBox.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = getDrawable(R.drawable.bg_card)
                setPadding(Ui.dp(this@InviteActivity, 14), Ui.dp(this@InviteActivity, 12), Ui.dp(this@InviteActivity, 14), Ui.dp(this@InviteActivity, 12))
                addView(TextView(this@InviteActivity).apply {
                    text = o.optString("nickname", o.optString("username", "新用户"))
                    textSize = 14f; setTextColor(getColor(R.color.t1))
                })
                addView(TextView(this@InviteActivity).apply {
                    text = o.optString("createtime", o.optString("time", ""))
                    textSize = 11f; setTextColor(getColor(R.color.t2))
                }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(this@InviteActivity, 4) })
            }, LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = Ui.dp(this@InviteActivity, 8)
            })
        }
    }
}
