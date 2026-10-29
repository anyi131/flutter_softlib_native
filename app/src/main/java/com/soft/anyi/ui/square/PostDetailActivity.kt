package com.soft.anyi.ui.square

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.data.PostRepo
import com.soft.anyi.ui.common.Ui
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch
import org.json.JSONObject

/** 帖子详情（对应 square/post_detail.dart） */
class PostDetailActivity : AppCompatActivity() {
    private var postId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postId = intent.getIntExtra("post_id", 0)
        setTitle("帖子详情")

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(getColor(R.color.page_bg))
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(this@PostDetailActivity, 16), Ui.dp(this@PostDetailActivity, 16),
                Ui.dp(this@PostDetailActivity, 16), Ui.dp(this@PostDetailActivity, 24))
        }
        scroll.addView(box, ViewGroup.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)

        lifecycleScope.launch {
            try {
                val p = PostRepo.detail(postId)
                render(box, p)
            } catch (e: Exception) {
                Toasts.e("加载失败：${e.message}")
            }
        }
    }

    private fun render(box: LinearLayout, p: JSONObject) {
        val ctx = this
        fun tv(text: String, size: Float, color: Int, top: Int): TextView =
            TextView(ctx).apply {
                this.text = text; textSize = size; setTextColor(getColor(color))
                LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .let { it.topMargin = Ui.dp(ctx, top); layoutParams = it }
            }
        box.addView(tv(p.optString("title"), 20f, R.color.t1, 0))
        box.addView(tv("${p.optString("nickname", "匿名")} · ${p.optString("time", "")}", 12f, R.color.t3, 8))
        box.addView(tv(p.optString("content"), 15f, R.color.t2, 14))
        val like = p.optInt("likes", 0)
        box.addView(tv("👍 $like", 14f, R.color.t2, 18))
    }
}
