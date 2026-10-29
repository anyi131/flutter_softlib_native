package com.soft.anyi.ui.mine

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.ApiHost
import com.soft.anyi.data.UserRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/**
 * 登录 / 注册 / 找回密码 三合一页（对应 login_page + register_page）。
 * Tab 切换：登录 → 注册 → 找回。
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var inputAccount: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputCode: EditText
    private lateinit var inputInvite: EditText
    private lateinit var inputEmail: EditText
    private lateinit var rowCode: View
    private lateinit var rowInvite: View
    private lateinit var rowEmail: View
    private lateinit var tvTitle: TextView
    private lateinit var btnMain: TextView
    private lateinit var btnSendCode: TextView
    private lateinit var tabLogin: TextView
    private lateinit var tabRegister: TextView
    private lateinit var tabReset: TextView
    private var mode = 0 // 0 登录 1 注册 2 找回

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val pad = Ui.dp(ctx, 16)

        fun field(hint: String): EditText = EditText(ctx).apply {
            this.hint = hint
            singleLine = true
            setBackgroundResource(R.drawable.bg_card)
            setPadding(pad, Ui.dp(ctx, 12), pad, Ui.dp(ctx, 12))
        }

        inputAccount = field("邮箱 / 用户名")
        inputPassword = field("密码")
        inputPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        inputCode = field("邮箱验证码")
        inputInvite = field("邀请码（选填）")
        inputEmail = field("注册邮箱")

        rowCode = row(inputCode)
        rowInvite = row(inputInvite)
        rowEmail = row(inputEmail)

        tvTitle = TextView(ctx).apply {
            text = "登录"
            textSize = 26f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ctx.getColor(R.color.t1))
        }
        btnMain = TextView(ctx).apply {
            text = "登 录"
            gravity = ViewGravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundResource(R.drawable.bg_pill_on)
            setPadding(0, Ui.dp(ctx, 13), 0, Ui.dp(ctx, 13))
            textSize = 16f
            setOnClickListener { submit() }
        }
        btnSendCode = TextView(ctx).apply {
            text = "发送验证码"
            gravity = ViewGravity.CENTER
            setTextColor(ctx.getColor(R.color.brand))
            setOnClickListener { sendCode() }
        }

        tabLogin = tab("登录", 0)
        tabRegister = tab("注册", 1)
        tabReset = tab("找回密码", 2)

        val root = ScrollView(ctx).apply {
            setBackgroundColor(ctx.getColor(R.color.page_bg))
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, Ui.dp(ctx, 28), pad, pad)
                addView(tvTitle)
                addView(tabs())
                addView(inputAccount, lp().apply { topMargin = Ui.dp(ctx, 20) })
                addView(inputPassword, lp().apply { topMargin = Ui.dp(ctx, 10) })
                addView(rowEmail, lp().apply { topMargin = Ui.dp(ctx, 10) })
                addView(rowCode, lp().apply { topMargin = Ui.dp(ctx, 10) })
                addView(btnSendCode, lp().apply { topMargin = Ui.dp(ctx, 4) })
                addView(rowInvite, lp().apply { topMargin = Ui.dp(ctx, 10) })
                addView(btnMain, lp().apply { topMargin = Ui.dp(ctx, 24) })
                addView(TextView(ctx).apply {
                    text = "登录即代表同意《软件分享汇》用户协议；注册即送新人积分。"
                    textSize = 12f
                    setTextColor(ctx.getColor(R.color.t2))
                    gravity = ViewGravity.CENTER
                }, lp().apply { topMargin = Ui.dp(ctx, 18) })
            })
        }
        setContentView(root)
        setMode(0)
        supportActionBar?.title = "账号"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun row(v: View) = v

    private fun lp() = LinearLayout.LayoutParams(-1, -2)

    private fun tab(text: String, m: Int): TextView = TextView(this).apply {
        this.text = text
        gravity = ViewGravity.CENTER
        textSize = 14f
        setPadding(Ui.dp(this@LoginActivity, 14), Ui.dp(this@LoginActivity, 8), Ui.dp(this@LoginActivity, 14), Ui.dp(this@LoginActivity, 8))
        tag = m
        setOnClickListener { setMode(m) }
    }

    private fun tabs(): View {
        val ctx = this
        val rowLl = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(tabLogin, tabRegister, tabReset).forEach {
            rowLl.addView(it, LinearLayout.LayoutParams(0, -2, 1f))
        }
        return rowLl
    }

    private fun setMode(m: Int) {
        mode = m
        val on = getColor(R.color.brand)
        val off = getColor(R.color.t2)
        tabLogin.setTextColor(if (m == 0) on else off)
        tabRegister.setTextColor(if (m == 1) on else off)
        tabReset.setTextColor(if (m == 2) on else off)
        rowEmail.visibility = if (m == 1) View.VISIBLE else View.GONE
        rowCode.visibility = if (m >= 1) View.VISIBLE else View.GONE
        btnSendCode.visibility = if (m >= 1) View.VISIBLE else View.GONE
        rowInvite.visibility = if (m == 1) View.VISIBLE else View.GONE
        tvTitle.text = when (m) { 0 -> "登录"; 1 -> "注册"; else -> "找回密码" }
        btnMain.text = when (m) { 0 -> "登 录"; 1 -> "注 册"; else -> "重置密码" }
    }

    private fun sendCode() {
        val email = (if (mode == 1) inputEmail else inputAccount).text.toString().trim()
        if (email.isEmpty()) { Toasts.e("请输入邮箱"); return }
        lifecycleScope.launch {
            try {
                UserRepo.sendCode(email, if (mode == 1) "register" else "reset")
                Toasts.s("验证码已发送，请查收邮箱")
            } catch (e: Exception) { Toasts.e(e.message ?: "发送失败") }
        }
    }

    private fun submit() {
        val acc = inputAccount.text.toString().trim()
        val pwd = inputPassword.text.toString().trim()
        val code = inputCode.text.toString().trim()
        if (acc.isEmpty() || pwd.isEmpty()) { Toasts.e("请填写完整"); return }
        btnMain.isEnabled = false
        lifecycleScope.launch {
            try {
                when (mode) {
                    0 -> { UserRepo.login(acc, pwd); Toasts.s("登录成功") }
                    1 -> {
                        UserRepo.register(
                            email = if (inputEmail.text.isNotBlank()) inputEmail.text.toString().trim() else acc,
                            code = code, password = pwd,
                            nickname = "", qq = "", invite = inputInvite.text.toString().trim()
                        )
                        Toasts.s("注册成功")
                    }
                    else -> { UserRepo.resetPassword(acc, code, pwd); Toasts.s("密码已重置，请登录"); setMode(0); return@launch }
                }
                setResult(RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toasts.e(e.message ?: "操作失败")
            } finally {
                btnMain.isEnabled = true
            }
        }
    }
}

private object ViewGravity {
    const val CENTER = android.view.Gravity.CENTER
}
