package com.soft.anyi.ui.details

import android.app.Activity
import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import io.coil_image.CoilImage
import com.soft.anyi.data.ApiHost

/**
 * 全屏图片查看器（对应 _previewGallery）：黑底 + ViewPager 滑动 + 页码。
 * ★ 全屏查看器不套任何玻璃壳（Flutter 版 1027 教训）。
 */
object PhotoViewer {

    fun show(ctx: Context, urls: List<String>, initial: Int = 0) {
        if (urls.isEmpty()) return
        val activity = ctx as Activity

        val root = android.widget.FrameLayout(ctx).apply { setBackgroundColor(0xFF000000.toInt()) }
        val vp = ViewPager2(ctx).apply {
            adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<VH>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ImageView(parent.context))
                override fun getItemCount() = urls.size
                override fun onBindViewHolder(h: VH, position: Int) {
                    CoilImage.load(h.iv.context, ApiHost.proxyImg(urls[position]), h.iv)
                    h.iv.setOnClickListener { (h.iv.context as Activity).finish() }
                }
            }
            setCurrentItem(initial, false)
        }
        root.addView(vp, ViewGroup.LayoutParams(-1, -1))
        val page = TextView(ctx).apply {
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 24)
        }
        root.addView(page, android.widget.FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        fun upd() { page.text = "${vp.currentItem + 1}/${urls.size}" }
        vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(p: Int) { upd() }
        })
        upd()

        activity.addContentView(
            root,
            ViewGroup.LayoutParams(-1, -1)
        )
    }

    private class VH(val iv: ImageView) : androidx.recyclerview.widget.RecyclerView.ViewHolder(
        LinearLayout(iv.context).apply {
            addView(iv, LinearLayout.LayoutParams(-1, -1))
        }
    )
}
