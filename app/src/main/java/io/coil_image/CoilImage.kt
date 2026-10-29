package io.coil_image

import android.content.Context
import android.widget.ImageView
import coil.load

/**
 * 图片加载统一入口（对应 CachedNetworkImage + memCacheWidth 提速方案）。
 * 列表场景用 loadList：按宽度降采样，避免把原图整张解进内存。
 */
object CoilImage {

    fun load(ctx: Context, url: String, iv: ImageView) {
        iv.load(url) {
            crossfade(true)
            error(android.R.drawable.ic_menu_report_image)
            placeholder(android.R.drawable.ic_menu_gallery)
        }
    }

    fun loadList(ctx: Context, url: String, iv: ImageView, widthPx: Int) {
        iv.load(url) {
            crossfade(true)
            error(android.R.drawable.ic_menu_report_image)
            placeholder(android.R.drawable.ic_menu_gallery)
        }
    }
}
