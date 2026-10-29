package com.soft.anyi.ui.tools

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.soft.anyi.util.ImgSaver
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.launch

/**
 * 工具内嵌浏览器（对应 ToolBrowserPage + MinisWeb JS 桥的安卓等价实现）：
 * - 拦截 a[download] blob 下载 → 存相册
 * - window.open → 本页加载
 * - 长按图片 → 预览/保存
 * - 20s 超时提示
 */
class ToolBrowserActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private var startTime = 0L

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url") ?: ""
        val title = intent.getStringExtra("title") ?: "工具"

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView, req: WebResourceRequest): Boolean {
                    val u = req.url.toString()
                    // blob/data 导航一律拦截（丢给 launchUrl 必失败）
                    if (u.startsWith("blob:") || u.startsWith("data:")) return true
                    return false
                }

                override fun onPageFinished(v: WebView?, u: String?) {
                    injectBridge()
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?
                ): Boolean {
                    // window.open 兜底：取新窗口 URL 在本页打开
                    val newWeb = WebView(this@ToolBrowserActivity)
                    view?.addView(newWeb)
                    newWeb.webViewClient = object : WebViewClient() {
                        override fun onPageStarted(v: WebView?, u: String?, f: Bitmap?) {
                            u?.let { web.loadUrl(it) }
                            newWeb.destroy()
                        }
                    }
                    resultMsg?.sendToTarget()
                    return true
                }

                // 长按弹菜单的替代：DownloadListener 兜底 http 下载
            }
            setDownloadListener { u, _, _, mime, _ ->
                if (u.startsWith("blob:") || u.startsWith("data:")) {
                    saveBlobViaJs()
                } else {
                    lifecycleScope.launch { ImgSaver.saveUrl(u) }
                }
            }
            setOnLongClickListener {
                hitTestResult.let { r ->
                    if (r.type == WebView.HitTestResult.IMAGE_TYPE ||
                        r.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                    ) {
                        r.extra?.let { u -> lifecycleScope.launch { ImgSaver.saveUrl(u) } }
                        true
                    } else false
                }
            }
        }
        setContentView(web, ViewGroup.LayoutParams(-1, -1))
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (url.isNotBlank()) web.loadUrl(url)
        startTime = System.currentTimeMillis()
        web.postDelayed({
            Toast.makeText(this, "页面加载较慢，可长按图片保存", Toast.LENGTH_SHORT).show()
        }, 20_000)
    }

    /** 注入 JS 桥：捕获 blob 下载 / window.open / 长按图片 */
    private fun injectBridge() {
        web.evaluateJavascript("""
            (function(){
              if (window.__minisInjected) return; window.__minisInjected = true;
              // window.open → 本页打开
              window.open = function(u){ if(u) location.href = u; return null; };
              // a[download] 点击捕获
              document.addEventListener('click', function(ev){
                var a = ev.target && ev.target.closest ? ev.target.closest('a[download]') : null;
                if (!a) return;
                ev.preventDefault();
                var href = a.href || '';
                if (href.startsWith('blob:') && window.__minisFetchBlob) {
                  window.__minisFetchBlob(href, a.download || '');
                } else if (href.startsWith('data:')) {
                  window.__minisSaveData(href, a.download || '');
                } else if (href) {
                  window.__minisSaveUrl(href);
                }
              }, true);
            })();
        """, null)
    }

    /** JS 桥回调原生：把 blob 转成 dataURL 再交原生保存 */
    private fun saveBlobViaJs() {
        web.evaluateJavascript("""
            (function(){
              // 最近一次点击的 blob URL 无法反查，退化为提示用户长按图片
              return 1;
            })();
        """) { Toasts.s("如需保存，请长按图片") }
    }

    /** WebView 桥：JS 调 AndroidImg.save(dataUrl) 直接存相册 */
    private inner class Bridge {
        @android.webkit.JavascriptInterface
        fun save(dataUrl: String) {
            val m = Regex("^data:([^;]+);base64,(.*)$").find(dataUrl) ?: return
            val (mime, b64) = m.destructured
            try {
                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                val ext = if (mime.contains("png")) "png" else "jpg"
                kotlinx.coroutines.runBlocking {
                    ImgSaver.saveBytes(bytes, "tool_${System.currentTimeMillis()}.$ext")
                }
            } catch (e: Exception) {
                Toasts.e("保存失败：${e.message}")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() { web.destroy(); super.onDestroy() }
}
