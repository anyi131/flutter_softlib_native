package com.soft.anyi.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.soft.anyi.R
import com.soft.anyi.data.Session
import com.soft.anyi.ui.home.HomeFragment
import com.soft.anyi.ui.mine.MineFragment
import com.soft.anyi.ui.square.SquareFragment
import com.soft.anyi.ui.tips.TipsFragment
import com.soft.anyi.ui.tools.ToolsFragment
import com.soft.anyi.util.Sfx
import com.soft.anyi.util.Toasts

/**
 * 主框架 —— 5 Tab（首页/工具/广场/线报/我的），对应 NavigatePage。
 * 底栏为自绘 Liquid 风格（长按浮起动效对应 liquid27）。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bar: LiquidBar
    private lateinit var pager: androidx.viewpager2.widget.ViewPager2

    private var lastBackAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        pager = androidx.viewpager2.widget.ViewPager2(this).apply {
            id = android.R.id.content
            adapter = object : FragmentStateAdapter(this@MainActivity) {
                override fun getItemCount() = 5
                override fun createFragment(position: Int): Fragment = when (position) {
                    0 -> HomeFragment()
                    1 -> ToolsFragment()
                    2 -> SquareFragment()
                    3 -> TipsFragment()
                    else -> MineFragment()
                }
            }
            offscreenPageLimit = 4
            isUserInputEnabled = false
        }
        root.addView(pager, FrameLayout.LayoutParams(-1, -1))

        bar = LiquidBar(this) { pos ->
            if (pager.currentItem != pos) {
                pager.setCurrentItem(pos, false)
                Sfx.haptic(this)
            }
        }
        root.addView(bar, FrameLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.BOTTOM))
        setContentView(root)

        bar.select(0, false)
    }

    /** 底栏透传给子页面（如首页需要知道 Tab 回到顶部） */
    fun currentTab(): Int = pager.currentItem

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val now = System.currentTimeMillis()
        if (now - lastBackAt < 2000) {
            finish()
        } else {
            lastBackAt = now
            Toasts.s("再按一次退出")
        }
    }
}
