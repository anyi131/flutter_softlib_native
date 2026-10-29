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
import com.soft.anyi.data.Session
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/**
 * 登录页（对应 login_page）：账号/密码 + 注册入口。
 */
class LoginActivity : AppCompatActivity() {
    private lateinit var inputUser: EditText
    private lateinit var inputPass: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.page_bg))
            setPadding(Ui.dp(ctx, 24), Ui.dp(ctx, 80), Ui.dp(ctx, 24), Ui.dp(ctx, 24))
        }

        root.addView(TextView(ctx).apply {
            text = "登录"
            textSize = 26f
            setTextColor(getColor(R.color.t1))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(ctx).apply {
            text = "欢迎回来，请登录您的账号"
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
        listOf(inputUser, inputPass).forEach {
            root.addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 16) })
        }

        val btnLogin = TextView(ctx).apply {
            text = "登 录"
            gravity = Gravity.CENTER
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = getDrawable(R.drawable.bg_btn_primary)
            setPadding(0, Ui.dp(ctx, 14), 0, Ui.dp(ctx, 14))
            setOnClickListener {
                val u = inputUser.text.toString().trim()
                val p = inputPass.text.toString()
                if (u.isEmpty() || p.isEmpty()) { Toasts.s("请输入账号和密码"); return@setOnClickListener }
                it.isEnabled = false
                lifecycleScope.launch {
                    try {
                        UserRepo.login(u, p)
                        Toasts.s("登录成功")
                        setResult(RESULT_OK)
                        finish()
                    } catch (e: Exception) {
                        Toasts.e(e.message ?: "登录失败")
                        it.isEnabled = true
                    }
                }
            }
        }
        root.addView(btnLogin, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 28) })

        root.addView(TextView(ctx).apply {
            text = "还没有账号？立即注册"
            textSize = 14f
            setTextColor(getColor(R.color.brand))
            gravity = Gravity.CENTER
            setPadding(0, Ui.dp(ctx, 18), 0, 0)
            setOnClickListener {
                startActivity(android.content.Intent(ctx, RegisterActivity::class.java))
            }
        }, LinearLayout.LayoutParams(-1, -2))

        setContentView(android.widget.ScrollView(ctx).apply { addView(root) })
    }
}
