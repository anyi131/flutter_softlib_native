package com.soft.anyi.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.soft.anyi.SoftApp
import com.soft.anyi.data.ApiHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 下载与安装（对应 Flutter 版 AppDownload / ApkInstaller）。
 * 用系统 DownloadManager 做大文件下载，完成后校验 + 拉起安装。
 */
object ApkInstaller {
    /** 下载 APK：返回下载任务，进度走 DownloadManager 查询 */
    fun enqueue(url: String, title: String): Long {
        val ctx = SoftApp.ctx()
        val name = (title.ifBlank { "anyi" }).replace(Regex("[\\\\/:*?\"<>|]"), "_") + ".apk"
        val req = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(title)
            setDescription("安逸软件汇 · 正在下载")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            setMimeType("application/vnd.android.package-archive")
            addRequestHeader("User-Agent", "SoftlibNative/1.0")
        }
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(req)
    }

    data class DlState(val status: Int, val progress: Int, val fileUri: Uri?)

    fun query(id: Long): DlState {
        val ctx = SoftApp.ctx()
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val q = DownloadManager.Query().setFilterById(id)
        dm.query(q).use { c ->
            if (!c.moveToFirst()) return DlState(DownloadManager.STATUS_FAILED, 0, null)
            val st = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val local = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            val progress = if (total > 0) (downloaded * 100 / total).toInt() else 0
            return DlState(st, progress, local?.let { Uri.parse(it) })
        }
    }

    /** 拉起安装（FileProvider 兼容 Android 7+） */
    fun install(ctx: Context, file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            val it = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(it)
            true
        } catch (e: Exception) {
            Toasts.e("无法安装：${e.message}")
            false
        }
    }

    fun installFromUri(ctx: Context, uri: Uri): Boolean {
        return try {
            val it = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(it)
            true
        } catch (e: Exception) {
            Toasts.e("无法安装：${e.message}")
            false
        }
    }

    /** urlType=browser 时跳系统浏览器 */
    fun openBrowser(url: String): Boolean = try {
        val ctx = SoftApp.ctx()
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        Toasts.e("打开失败：${e.message}")
        false
    }

    /** 带 Context 的重载（Fragment 里传 requireContext()） */
    fun openBrowser(ctx: Context, url: String): Boolean = try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        true
    } catch (e: Exception) {
        Toasts.e("打开失败：${e.message}")
        false
    }
}

/**
 * 图片保存到相册（对应 ImgSaver / jicun_save_album，相册目录「安逸软件汇」）。
 */
object ImgSaver {
    suspend fun saveUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val rb = okhttp3.Request.Builder().url(url).get()
            com.soft.anyi.data.Http.client.newCall(rb.build()).execute().use { r ->
                val bytes = r.body?.bytes() ?: return@withContext false
                saveBytes(bytes, "img_${System.currentTimeMillis()}.jpg")
            }
        } catch (e: Exception) {
            Toasts.e("保存失败：${e.message}")
            false
        }
    }

    suspend fun saveBytes(bytes: ByteArray, name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val ctx = SoftApp.ctx()
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "安逸软件汇"
            )
            dir.mkdirs()
            val f = File(dir, name)
            f.writeBytes(bytes)
            // 扫描进相册
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(f)
            ctx.sendBroadcast(intent)
            Toasts.s("已保存到相册")
            true
        } catch (e: Exception) {
            Toasts.e("保存失败：${e.message}")
            false
        }
    }
}
