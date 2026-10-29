package com.soft.anyi

import android.app.Application
import android.content.Context
import com.soft.anyi.data.AppConfig
import com.soft.anyi.data.ConfigRepo
import com.soft.anyi.data.Session
import com.soft.anyi.util.DeviceInfo
import com.soft.anyi.util.QQAuthCache
import com.soft.anyi.util.Sfx
import com.soft.anyi.util.Toasts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口 —— 全局单例持有：配置仓库 / 会话 / 设备信息 / 音效
 * 对应 Flutter 版 main.dart 的启动初始化序列（错误捕获 + 预热）。
 */
class SoftApp : Application() {

    companion object {
        lateinit var instance: SoftApp
            private set
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        fun ctx(): Context = instance
    }

    var config: AppConfig? = null
        private set

    /** 供启动链写入最新配置（SplashActivity / 维护页刷新） */
    fun setConfig(c: AppConfig) { config = c }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 线程级异常兜底（对应 runZonedGuarded）：不上报崩溃，避免网络抖动刷屏
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Toasts.e("发生错误：${e?.message ?: "未知"}")
        }

        // 设备信息头（X-Device-Model / X-Os-Version）预热
        DeviceInfo.init(this)

        // 会话恢复（对应 UserService.restore()）
        Session.init(this)

        // 音效偏好（对应 SoundUtil.init()）
        QQAuthCache.init(this)
        Sfx.init(this)

        // 远程配置拉取 + 缓存（后台可关）—— 对应 SoftService.fetchConfig
        appScope.launch {
            val cfg = ConfigRepo.fetchCachedFirst(this@SoftApp)
            config = cfg
            cfg?.let { Sfx.applyRemote(it.sfx) }
        }
    }
}
