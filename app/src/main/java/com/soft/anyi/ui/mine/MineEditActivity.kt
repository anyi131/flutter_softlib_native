package com.soft.anyi.ui.mine

import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.Session
import com.soft.anyi.data.UserRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/** 资料编辑（昵称 / 头像链接 / 密码修改）——对应 Flutter profile_edit */
class MineEditActivity : AppCompatActivity() {
    private lateinit var nick: EditText

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ctx.getColor(R.color.page_bg))
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 16))
        }
        setContentView(root)

        // 顶栏
        root.addView(TextView(ctx).apply {
            text = "编辑资料"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ctx.getColor(R.color.t1))
        }, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = Ui.dp(ctx, 14) })

        val u = Session.user()
        nick = EditText(ctx).apply {
            hint = "昵称"
            setText(u?.nickname ?: "")
            singleLine = true
            background = ctx.getDrawable(R.drawable.bg_card)
            setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 10), Ui.dp(ctx, 12), Ui.dp(ctx, 10))
        }
        root.addView(nick, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = Ui.dp(ctx, 10) })

        root.addView(Ui.pill(ctx, "保存") {
            val name = nick.text.toString().trim()
            if (name.isEmpty()) { Toasts.s("昵称不能为空"); return@pill }
            lifecycleScope.launch {
                try {
                    UserRepo.updateProfile(nickname = name)
                    Toasts.s("已保存")
                    finish()
                } catch (e: Exception) { Toasts.e(e.message ?: "保存失败") }
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = Ui.dp(ctx, 6) })
    }
}
