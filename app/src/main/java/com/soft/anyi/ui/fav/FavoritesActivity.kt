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
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/** 我的收藏（对应 fav/favorites.dart） */
class FavoritesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("我的收藏")

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(getColor(R.color.page_bg))
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 24)
        }
        scroll.addView(box, ViewGroup.LayoutParams(-1, -2))
        setContentView(scroll)

        lifecycleScope.launch {
            try {
                val ids = com.soft.anyi.util.FavStore.favIds()
                if (ids.isEmpty()) {
                    box.addView(empty())
                } else ids.forEach { id ->
                    try { box.addView(AppCard.build(this@FavoritesActivity, com.soft.anyi.data.SoftRepo.detail(id))) } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Toasts.e("加载失败：${e.message}")
                box.addView(empty())
            }
        }
    }

    private fun empty(): TextView = TextView(this).apply {
        text = "还没有收藏，去首页看看吧"
        textSize = 14f
        setTextColor(getColor(R.color.t3))
        gravity = android.view.Gravity.CENTER
        setPadding(0, 120, 0, 0)
    }
}
