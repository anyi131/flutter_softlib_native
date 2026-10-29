package com.soft.anyi.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.soft.anyi.R
import com.soft.anyi.data.SfxConfig
import com.soft.anyi.SoftApp

/**
 * 音效开关（对应 Flutter 版 SoundUtil）。
 * 服务端 ui_config.sfx 为权威（applyRemote），本地只做立即生效缓存。
 */
object Sfx {
    private var sp: android.content.SharedPreferences? = null
    private var pool: SoundPool? = null
    private val ids = HashMap<Int, Int>() // resId -> soundId

    // 本地开关（master 关 = 全静音）
    private var master = true
    private var remote = SfxConfig()

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("sfx", Context.MODE_PRIVATE)
        master = sp?.getBoolean("master", true) ?: true
        pool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }

    /** 服务端配置到达后调用（ui_config.sfx） */
    fun applyRemote(cfg: SfxConfig) {
        remote = cfg
    }

    fun setMaster(on: Boolean) {
        master = on
        sp?.edit()?.putBoolean("master", on)?.apply()
    }

    fun masterOn(): Boolean = master

    /** kind: download/sign/message/update */
    fun play(kind: String) {
        if (!master) return
        val on = when (kind) {
            "download" -> remote.download
            "sign" -> remote.sign
            "message" -> remote.message
            "update" -> remote.update
            else -> true
        }
        if (!on) return
        val res = when (kind) {
            "download" -> R.raw.download
            "sign" -> R.raw.sign
            "message" -> R.raw.message
            "update" -> R.raw.update
            else -> return
        }
        val p = pool ?: return
        val sid = ids[res] ?: p.load(SoftApp.ctx(), res, 1).also { ids[res] = it }
        p.play(sid, 1f, 1f, 1, 0, 1f)
    }

    fun haptic(ctx: Context) {
        val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(10, 80))
        } else @Suppress("DEPRECATION") {
            v.vibrate(10)
        }
    }
}
