package com.soft.anyi.ui.fav

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.ui.common.AppCard
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.FavStore
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/** 浏览历史（对应 fav/history.dart） */
class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("浏览历史")

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(getColor(R.color.page_bg))
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@HistoryActivity, 12), Ui.dp(this@HistoryActivity, 12),
                Ui.dp(this@HistoryActivity, 12), Ui.dp(this@HistoryActivity, 24))
        }
        scroll.addView(box, ViewGroup.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)

        lifecycleScope.launch {
            try {
                val ids = FavStore.historyIds().reversed()
                if (ids.isEmpty()) {
                    box.addView(TextView(this@HistoryActivity).apply {
                        text = "暂无浏览记录"
                        textSize = 14f; setTextColor(getColor(R.color.t2))
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, Ui.dp(this@HistoryActivity, 100), 0, 0)
                    })
                } else ids.forEach { id ->
                    try { box.addView(AppCard.build(this@HistoryActivity, com.soft.anyi.data.SoftRepo.detail(id))) } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Toasts.e("加载失败：${e.message}")
            }
        }
    }
}
