package com.soft.anyi.ui.auth

import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.UserRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.QQAuthCache
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/**
 * 注册页（对应 register_page）：账号/密码/确认密码 + 可选邀请码。
 */
class RegisterActivity : AppCompatActivity() {
    private lateinit var inputUser: EditText
    private lateinit var inputPass: EditText
    private lateinit var inputPass2: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.page_bg))
            setPadding(Ui.dp(ctx, 24), Ui.dp(ctx, 80), Ui.dp(ctx, 24), Ui.dp(ctx, 24))
        }

        root.addView(TextView(ctx).apply {
            text = "注册"
            textSize = 26f
            setTextColor(getColor(R.color.t1))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(ctx).apply {
            text = "创建一个新账号"
            textSize = 13f
            setTextColor(getColor(R.color.t2))
        }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(ctx, 6) })

        fun field(hint: String): EditText = EditText(ctx).apply {
            this.hint = hint
            textSize = 15f
            setSingleLine()
            background = getDrawable(R.drawable.bg_input)
            setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 12))
        }

        inputUser = field("账号 / 邮箱")
        inputPass = field("密码").apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        inputPass2 = field("确认密码").apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        listOf(inputUser, inputPass, inputPass2).forEach {
            root.addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 16) })
        }

        val btn = TextView(ctx).apply {
            text = "注 册"
            gravity = Gravity.CENTER
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = getDrawable(R.drawable.bg_btn_primary)
            setPadding(0, Ui.dp(ctx, 14), 0, Ui.dp(ctx, 14))
            setOnClickListener {
                val u = inputUser.text.toString().trim()
                val p = inputPass.text.toString()
                val p2 = inputPass2.text.toString()
                if (u.isEmpty() || p.isEmpty()) { Toasts.s("请填写完整"); return@setOnClickListener }
                if (p != p2) { Toasts.s("两次密码不一致"); return@setOnClickListener }
                it.isEnabled = false
                lifecycleScope.launch {
                    try {
                        UserRepo.register(u, "", p,
                            bindOpenid = QQAuthCache.pendingOpenid,
                            bindToken = QQAuthCache.pendingToken)
                        QQAuthCache.clearPending()
                        Toasts.s("注册成功，请登录")
                        finish()
                    } catch (e: Exception) {
                        Toasts.e(e.message ?: "注册失败")
                        it.isEnabled = true
                    }
                }
            }
        }
        root.addView(btn, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 28) })

        setContentView(android.widget.ScrollView(ctx).apply { addView(root) })
    }
}
