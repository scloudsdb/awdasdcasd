package com.shadownightdev.universaliptv.ui.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.shadownightdev.universaliptv.R
import com.shadownightdev.universaliptv.databinding.ActivityPlayerBinding
import com.shadownightdev.universaliptv.util.LocaleHelper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHANNEL_NAME = "channel_name"
        const val EXTRA_CHANNEL_URL = "channel_url"
    }

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var channelUrl: String = ""
    private var channelName: String = ""
    private var isFullscreen = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "Channel"
        channelUrl = intent.getStringExtra(EXTRA_CHANNEL_URL) ?: ""

        binding.tvChannelName.text = channelName
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { playStream() }

        initPlayer()
        playStream()
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        binding.playerView.setKeepScreenOn(true)

        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> showLoading()
                    Player.STATE_READY -> showPlayer()
                    Player.STATE_ENDED -> showError(getString(R.string.stream_error))
                    Player.STATE_IDLE -> { /* Do nothing */ }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                showError(error.localizedMessage ?: getString(R.string.stream_error))
            }
        })
    }

    private fun playStream() {
        if (channelUrl.isEmpty()) {
            showError(getString(R.string.stream_error))
            return
        }

        showLoading()

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(httpClient)

        val mediaSource: MediaSource = if (channelUrl.contains(".m3u8", ignoreCase = true) ||
            channelUrl.contains("hls", ignoreCase = true)
        ) {
            HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(channelUrl))
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(channelUrl))
        }

        player?.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
        binding.errorOverlay.visibility = View.GONE
    }

    private fun showPlayer() {
        binding.loadingOverlay.visibility = View.GONE
        binding.errorOverlay.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.loadingOverlay.visibility = View.GONE
        binding.errorOverlay.visibility = View.VISIBLE
        binding.errorText.text = message
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
