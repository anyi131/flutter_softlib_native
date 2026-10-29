package com.soft.anyi.ui.tools

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * 播放页（对应 PlayerNext）：
 * video → 全屏视频；audio → 后台播放（大封面 + 控制）。
 */
class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra("mode") ?: "video"
        val url = intent.getStringExtra("url") ?: ""
        val title = intent.getStringExtra("title") ?: "播放"

        val ctx = this
        val root = FrameLayout(ctx)
        val pv = PlayerView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            useController = true
        }
        root.addView(pv)
        setContentView(root)
        supportActionBar?.title = title
        supportActionBar?.hide()

        player = ExoPlayer.Builder(ctx).build().apply {
            pv.player = this
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
