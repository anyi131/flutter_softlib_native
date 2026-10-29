package com.soft.anyi.ui.task

import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.TaskRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/** 任务中心：输入口令领奖励（对应 task/task.dart） */
class TaskActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("任务中心")

        val scroll = android.widget.ScrollView(this)
        scroll.setBackgroundColor(getColor(R.color.page_bg))
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@TaskActivity, 16), Ui.dp(this@TaskActivity, 24),
                Ui.dp(this@TaskActivity, 16), Ui.dp(this@TaskActivity, 24))
        }
        scroll.addView(box, ViewGroup.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)

        box.addView(TextView(this).apply {
            text = "输入口令领取奖励"
            textSize = 16f; setTextColor(getColor(R.color.t1))
        })
        val input = EditText(this).apply {
            hint = "请输入口令"
            textSize = 15f
            background = androidx.core.content.ContextCompat.getDrawable(this@TaskActivity, R.drawable.bg_input)
        }
        box.addView(input, (LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)).apply {
            topMargin = Ui.dp(this@TaskActivity, 12)
        })

        val go = TextView(this).apply {
            text = "领 取"
            textSize = 16f
            setTextColor(androidx.core.content.ContextCompat.getColor(this@TaskActivity, android.R.color.white))
            background = androidx.core.content.ContextCompat.getDrawable(this@TaskActivity, R.drawable.bg_btn_primary)
            gravity = android.view.Gravity.CENTER
            setPadding(0, Ui.dp(this@TaskActivity, 12), 0, Ui.dp(this@TaskActivity, 12))
        }
        box.addView(go, (LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)).apply {
            topMargin = Ui.dp(this@TaskActivity, 16)
        })

        go.setOnClickListener {
            val code = input.text.toString().trim()
            if (code.isEmpty()) { Toasts.s("请输入口令"); return@setOnClickListener }
            go.isEnabled = false
            lifecycleScope.launch {
                try {
                    val msg = TaskRepo.claim(code)
                    Toasts.s(msg)
                } catch (e: Exception) {
                    Toasts.e("领取失败：${e.message}")
                } finally { go.isEnabled = true }
            }
        }
    }
}
