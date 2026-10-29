package com.soft.anyi.ui.common

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.soft.anyi.util.Toasts

/**
 * 内置浏览器（对应 UrlWebviewPage）：
 * 私有协议白名单、进度条、标题栏 + 「用浏览器打开」兜底。
 */
class WebViewActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private lateinit var bar: ProgressBar
    private var startTime = 0L

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url") ?: ""
        val title = intent.getStringExtra("title") ?: "浏览"

        val root = FrameLayout(this)
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView, req: WebResourceRequest): Boolean {
                    val scheme = req.url.scheme ?: ""
                    // 私有协议白名单（1016 教训：非白名单协议直接吞掉，避免卡顿）
                    if (scheme !in listOf("http", "https", "about")) {
                        val ok = scheme in listOf("tel", "mailto", "sms", "geo", "weixin", "mqqwpa", "alipays")
                        if (!ok) return true
                        return try {
                            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, req.url))
                            true
                        } catch (e: Exception) {
                            Toasts.e("无法打开：$scheme")
                            true
                        }
                    }
                    return false
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(v: WebView?, newProgress: Int) {
                    bar.progress = newProgress
                    bar.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                }
            }
        }
        bar = ProgressBar(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (3 * resources.displayMetrics.density).toInt()
            )
        }
        root.addView(web, FrameLayout.LayoutParams(-1, -1))
        root.addView(bar)
        setContentView(root)

        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (url.isNotBlank()) web.loadUrl(url)

        // 20s 超时提示（对应 ToolBrowserPage 的超时遮罩）
        startTime = System.currentTimeMillis()
        web.postDelayed({
            if (bar.visibility == View.VISIBLE) {
                Toasts.s("加载较慢，可点右上角菜单用浏览器打开")
            }
        }, 20_000)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menu?.add("用浏览器打开")?.setOnMenuItemClickListener {
            com.soft.anyi.util.ApkInstaller.openBrowser(web.url ?: "")
            true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }
}
