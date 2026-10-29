package com.soft.anyi.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.load
import androidx.lifecycle.lifecycleScope
import com.soft.anyi.R
import com.soft.anyi.SoftApp
import com.soft.anyi.data.AppConfig
import com.soft.anyi.data.ConfigRepo
import com.soft.anyi.data.Session
import com.soft.anyi.data.VersionRepo
import com.soft.anyi.util.ApkInstaller
import com.soft.anyi.util.Prefs
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 开屏页（对应 SplashPage）：
 * 远程开屏图 + 倒计时 + 公告弹窗（每日限次）+ 维护拦截 + 版本更新。
 *
 * ★ 移植自 Flutter 版踩坑教训：
 *  - 维护判定必须在倒计时放行**之前**（321ee59 时序 bug）
 *  - 维护页「重新加载」只做重新判定，绝不能重启倒计时（b2a5042 连点进首页漏洞）
 *  - 公告按「用户id+天」计次，换号重计，跨天清零
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private var cfg: AppConfig? = null
    private var cfgSettled = false
    private var entered = false
    private var entering = false
    private var countdownJob: kotlinx.coroutines.Job? = null

    private lateinit var root: FrameLayout
    private lateinit var splashImg: ImageView
    private lateinit var btnSkip: Button
    private lateinit var logo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#FAF9FE")) }
        splashImg = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = ImageView.GONE
        }
        logo = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = FrameLayout.LayoutParams(
                (96 * resources.displayMetrics.density).toInt(),
                (96 * resources.displayMetrics.density).toInt()
            ).apply { gravity = Gravity.CENTER }
        }
        btnSkip = Button(this).apply {
            text = "跳过 3"
            textSize = 13f
            setTextColor(Color.WHITE)
            background = androidx.core.content.res.ResourcesCompat.getDrawable(
                resources, R.drawable.bg_pill_alpha, theme
            )
            alpha = 0.85f
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply {
                marginEnd = (20 * resources.displayMetrics.density).toInt()
                topMargin = (60 * resources.displayMetrics.density).toInt()
            }
        }
        val word = TextView(this).apply {
            text = "安逸软件汇"
            textSize = 15f
            setTextColor(Color.parseColor("#6B6B80"))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply { bottomMargin = (48 * resources.displayMetrics.density).toInt() }
        }
        root.addView(splashImg, FrameLayout.LayoutParams(-1, -1))
        root.addView(logo)
        root.addView(btnSkip)
        root.addView(word)
        setContentView(root)

        // 入场动画
        logo.scaleX = 0.6f; logo.scaleY = 0.6f; logo.alpha = 0f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logo, "scaleX", 0.6f, 1f),
                ObjectAnimator.ofFloat(logo, "scaleY", 0.6f, 1f),
                ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f)
            )
            duration = 450
            interpolator = DecelerateInterpolator()
            start()
        }

        btnSkip.setOnClickListener { enter() }

        lifecycleScope.launch {
            // ① 磁盘缓存配置先回（不阻塞）② 网络刷新最多等 2.5s
            cfg = ConfigRepo.cached(this@SplashActivity)
            launch {
                val c = try {
                    withContext(Dispatchers.IO) {
                        ConfigRepo.fetchCachedFirst(this@SplashActivity)
                    }
                } catch (_: Exception) { null }
                c?.let {
                    cfg = it
                    SoftApp.instance.setConfig(it)
                }
                cfgSettled = true
            }
            withContext(Dispatchers.IO) {
                val deadline = System.currentTimeMillis() + 2500
                while (!cfgSettled && System.currentTimeMillis() < deadline) delay(100)
            }
            onConfigReady()
        }
    }

    private fun onConfigReady() {
        val c = cfg

        // ★ 维护判定在一切放行逻辑之前
        if (c?.maintainEnable == true) {
            showMaintain(c)
            return
        }

        // 开屏图
        if (c?.splashEnable == true && c.splashImage.isNotBlank()) {
            splashImg.visibility = ImageView.VISIBLE
            splashImg.load(c.splashImage) {
                crossfade(true)
                error(R.mipmap.ic_launcher)
            }
            if (c.splashUrl.isNotBlank()) {
                splashImg.setOnClickListener { ApkInstaller.openBrowser(c.splashUrl) }
            }
        }

        // 版本检查（静默）
        lifecycleScope.launch {
            val cur = try {
                packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
            } catch (_: Exception) { "1.0" }
            VersionRepo.check(cur)?.let { showUpdate(it, forced = it.forced) }
        }

        // 倒计时
        val sec = c?.splashSeconds ?: 3
        countdownJob = lifecycleScope.launch {
            for (i in sec downTo 1) {
                btnSkip.text = "跳过 $i"
                delay(1000)
            }
            enter()
        }

        // 公告（每日限次）
        if (c?.noticeEnable == true && c.noticeContent.isNotBlank()) {
            showNoticeIfNeeded(c)
        }
    }

    private fun showNoticeIfNeeded(c: AppConfig) {
        if (!c.noticeForce) {
            val day = SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(Date())
            val uid = Session.user()?.id ?: 0
            val dayKey = "notice_day_$uid"
            val cntKey = "notice_cnt_$uid"
            if (Prefs.str(dayKey) != day) {
                Prefs.setStr(dayKey, day)
                Prefs.setInt(cntKey, 0)
            }
            if (c.noticeDailyLimit > 0 && Prefs.int(cntKey) >= c.noticeDailyLimit) return
            Prefs.setInt(cntKey, Prefs.int(cntKey) + 1)
        }
        AlertDialog.Builder(this)
            .setTitle(c.noticeTitle)
            .setMessage(c.noticeContent)
            .setCancelable(!c.noticeForce)
            .setPositiveButton("知道了", null)
            .setNegativeButton("查看详情") { _, _ ->
                if (c.noticeUrl.isNotBlank()) ApkInstaller.openBrowser(c.noticeUrl)
            }
            .show()
    }

    private fun showUpdate(v: VersionRepo.VersionInfo, forced: Boolean) {
        val dlg = AlertDialog.Builder(this)
            .setTitle("发现新版本 ${v.version}")
            .setMessage(v.desc.ifBlank { "更新体验更佳" })
            .setPositiveButton("立即更新") { _, _ ->
                ApkInstaller.enqueue(v.url, "安逸软件汇 ${v.version}")
                if (forced) enter()
            }
            .setNegativeButton("下次再说") { _, _ -> }
            .create()
        if (forced) dlg.setCanceledOnTouchOutside(false)
        dlg.show()
    }

    private fun showMaintain(c: AppConfig) {
        val view = layoutInflater.inflate(R.layout.view_maintain, null)
        view.findViewById<TextView>(R.id.tvMaintainText).text =
            c.maintainText.ifBlank { "系统维护中，请稍后再来" }
        val dlg = androidx.appcompat.app.AppCompatDialog(this).apply {
            setContentView(view)
            setCancelable(false)
            window?.setLayout(-1, -1)
        }
        view.findViewById<Button>(R.id.btnReload).setOnClickListener {
            lifecycleScope.launch {
                view.findViewById<Button>(R.id.btnReload).isEnabled = false
                val fresh = ConfigRepo.fetchForce(this@SplashActivity)
                view.findViewById<Button>(R.id.btnReload).isEnabled = true
                if (fresh?.maintainEnable == true) {
                    Toasts.s("仍在维护中")
                } else {
                    dlg.dismiss()
                    cfg = fresh
                    // ★ 只做重新判定，不重启任何倒计时（b2a5042 教训）
                    countdownJob?.cancel()
                    enter()
                }
            }
        }
        dlg.show()
    }

    /** 放行进主框架（三重保险：维护中绝不放行） */
    private fun enter() {
        if (entered || entering) return
        if (cfg?.maintainEnable == true) return
        entering = true
        lifecycleScope.launch {
            val deadline = System.currentTimeMillis() + 2500
            while (!cfgSettled && System.currentTimeMillis() < deadline) delay(100)
            if (cfg?.maintainEnable == true) { entering = false; return@launch }
            entered = true
            startActivity(Intent(this@SplashActivity, com.soft.anyi.ui.main.MainActivity::class.java))
            finish()
        }
    }
}
