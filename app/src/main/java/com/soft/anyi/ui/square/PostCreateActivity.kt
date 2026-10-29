package com.soft.anyi.ui.square

import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.PostRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/** 发布帖子（对应 square/post_create.dart） */
class PostCreateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("发布")

        val scroll = android.widget.ScrollView(this)
        scroll.setBackgroundColor(getColor(R.color.page_bg))
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@PostCreateActivity, 16), Ui.dp(this@PostCreateActivity, 16),
                Ui.dp(this@PostCreateActivity, 16), Ui.dp(this@PostCreateActivity, 24))
        }
        scroll.addView(box, ViewGroup.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)

        val title = EditText(this).apply {
            hint = "标题"
            textSize = 16f
            setSingleLine(true)
            background = androidx.core.content.ContextCompat.getDrawable(
                this@PostCreateActivity, R.drawable.bg_input)
        }
        val content = EditText(this).apply {
            hint = "说点什么吧…"
            textSize = 15f
            minLines = 6
            gravity = android.view.Gravity.TOP
            background = androidx.core.content.ContextCompat.getDrawable(
                this@PostCreateActivity, R.drawable.bg_input)
        }
        fun lp(top: Int) = (LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)).apply { topMargin = Ui.dp(this@PostCreateActivity, top) }
        box.addView(title, lp(0))
        box.addView(content, lp(12))

        val submit = TextView(this).apply {
            text = "发 布"
            textSize = 16f
            setTextColor(androidx.core.content.ContextCompat.getColor(this@PostCreateActivity, android.R.color.white))
            background = androidx.core.content.ContextCompat.getDrawable(this@PostCreateActivity, R.drawable.bg_btn_primary)
            gravity = android.view.Gravity.CENTER
            setPadding(0, Ui.dp(this@PostCreateActivity, 12), 0, Ui.dp(this@PostCreateActivity, 12))
        }
        box.addView(submit, lp(20))

        submit.setOnClickListener {
            val t = title.text.toString().trim()
            val c = content.text.toString().trim()
            if (t.isEmpty() || c.isEmpty()) { Toasts.s("标题和内容不能为空"); return@setOnClickListener }
            submit.isEnabled = false
            lifecycleScope.launch {
                try {
                    PostRepo.create(c, emptyList(), "")
                    Toasts.s("发布成功")
                    finish()
                } catch (e: Exception) {
                    submit.isEnabled = true
                    Toasts.e("发布失败：${e.message}")
                }
            }
        }
    }
}
