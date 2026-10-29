package com.soft.anyi.ui.common

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.soft.anyi.R
import com.soft.anyi.data.ApiHost
import com.soft.anyi.data.AppItem
import com.soft.anyi.data.Session
import io.coil_image.CoilImage

/** 软件卡片（首页/搜索/收藏复用，对应 widgets/app_card.dart） */
object AppCard {
    fun build(ctx: Context, a: AppItem): View {
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.bg_card)
            setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 12), Ui.dp(ctx, 12), Ui.dp(ctx, 12))
        }
        val icon = ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        card.addView(icon, LinearLayout.LayoutParams(Ui.dp(ctx, 52), Ui.dp(ctx, 52)))
        CoilImage.load(ctx, a.icon, icon)

        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(ctx).apply {
            text = a.title
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ctx.getColor(R.color.t1))
            maxLines = 1
        })
        col.addView(TextView(ctx).apply {
            text = a.description.ifBlank { "暂无简介" }
            textSize = 12f
            setTextColor(ctx.getColor(R.color.t2))
            maxLines = 1
        }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = Ui.dp(ctx, 3) })

        val mid = LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginStart = Ui.dp(ctx, 12)
        }
        card.addView(col, mid)

        // 价格角标
        card.addView(TextView(ctx).apply {
            text = a.priceTag
            textSize = 11f
            setTextColor(Color.WHITE)
            background = ctx.getDrawable(
                if (a.priceTag == "免费") R.drawable.bg_pill_off else R.drawable.bg_pill_on
            )
            setPadding(Ui.dp(ctx, 8), Ui.dp(ctx, 3), Ui.dp(ctx, 8), Ui.dp(ctx, 3))
        })

        card.setOnClickListener { AppAction.open(ctx, a) }
        return card
    }
}
