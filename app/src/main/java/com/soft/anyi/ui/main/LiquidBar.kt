package com.soft.anyi.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.soft.anyi.R

/**
 * Liquid 风格玻璃底栏（对应 liquid26/27 模板）：
 * 选中项淡绿胶囊 + 图标浮起；长按拟真（按下凹 0.92 / 长按浮起 1.35 / 松手切换）。
 */
class LiquidBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val onTap: (Int) -> Unit = {}
) : LinearLayout(context, attrs) {

    private val items = mutableListOf<TextView>()
    private val labels = listOf("首页", "工具", "广场", "线报", "我的")
    private val icons = listOf("⌂", "⚙", "□", "⚡", "☺")
    private var selected = 0

    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8E4F8")
    }
    private val pillRect = RectF()

    init {
        orientation = HORIZONTAL
        setBackgroundColor(Color.parseColor("#F2FFFFFF"))
        elevation = 12f
        val pad = (8 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad / 2, pad, pad)

        labels.forEachIndexed { i, label ->
            val tv = TextView(context).apply {
                text = "${icons[i]}\n$label"
                gravity = Gravity.CENTER
                textSize = 11f
                setLineSpacing(0f, 0.9f)
                setTextColor(if (i == 0) Color.parseColor("#6750A4") else Color.parseColor("#8A8A9E"))
                setTypeface(typeface, if (i == 0) Typeface.BOLD else Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(tv)
            items.add(tv)
        }
    }

    fun select(pos: Int, animate: Boolean = true) {
        selected = pos
        items.forEachIndexed { i, tv ->
            val on = i == pos
            tv.setTextColor(if (on) Color.parseColor("#6750A4") else Color.parseColor("#8A8A9E"))
            tv.setTypeface(tv.typeface, if (on) Typeface.BOLD else Typeface.NORMAL)
            if (animate && on) {
                tv.animate().scaleX(1.12f).scaleY(1.12f).setDuration(180)
                    .setInterpolator(OvershootInterpolator(2f))
                    .withEndAction { tv.animate().scaleX(1f).scaleY(1f).setDuration(140).start() }
                    .start()
            } else {
                tv.scaleX = 1f; tv.scaleY = 1f
            }
        }
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val idx = indexAt(e.x)
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (idx >= 0) { items[idx].animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).start() }
            }
            MotionEvent.ACTION_CANCEL -> {
                items.forEach { it.animate().scaleX(1f).scaleY(1f).setDuration(90).start() }
            }
            MotionEvent.ACTION_UP -> {
                if (idx >= 0) {
                    select(idx)
                    onTap(idx)
                } else {
                    items.forEach { it.animate().scaleX(1f).scaleY(1f).setDuration(90).start() }
                }
            }
        }
        return true
    }

    private fun indexAt(x: Float): Int {
        val w = width / items.size.coerceAtLeast(1)
        return (x / w).toInt().takeIf { it in items.indices } ?: -1
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 选中项底部胶囊
        val w = width / items.size.coerceAtLeast(1)
        val pad = 10 * resources.displayMetrics.density
        pillRect.set(
            selected * w + pad, height - 6 * resources.displayMetrics.density,
            (selected + 1) * w - pad, height - 2 * resources.displayMetrics.density
        )
        canvas.drawRoundRect(pillRect, 8f, 8f, pillPaint)
    }
}
