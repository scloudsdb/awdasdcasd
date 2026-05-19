package com.univiptv.orionhub.site.ui.player

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.bumptech.glide.Glide
import com.univiptv.orionhub.site.R
import com.univiptv.orionhub.site.databinding.ActivityPlayerBinding
import com.univiptv.orionhub.site.util.LocaleHelper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHANNEL_NAME = "channel_name"
        const val EXTRA_CHANNEL_URL = "channel_url"
        const val EXTRA_IS_DRM = "is_drm"
        const val EXTRA_DRM_KEY_ID = "drm_key_id"
        const val EXTRA_DRM_KEY = "drm_key"
        const val EXTRA_DRM_TYPE = "drm_type"
        const val EXTRA_IMAGE_URL = "image_url"
        const val EXTRA_USER_AGENT = "user_agent"
        const val EXTRA_REFERRER = "referrer"
        const val EXTRA_DRM_LICENSE_URL = "drm_license_url"
    }

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var channelUrl = ""
    private var channelName = ""
    private var isDrm = false
    private var drmKeyId = ""
    private var drmKey = ""
    private var drmType = ""
    private var drmLicenseUrl = ""
    private var imageUrl: String? = null
    private var userAgent: String? = null
    private var referrer: String? = null

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
        isDrm = intent.getBooleanExtra(EXTRA_IS_DRM, false)
        drmKeyId = intent.getStringExtra(EXTRA_DRM_KEY_ID) ?: ""
        drmKey = intent.getStringExtra(EXTRA_DRM_KEY) ?: ""
        drmType = intent.getStringExtra(EXTRA_DRM_TYPE) ?: ""
        drmLicenseUrl = intent.getStringExtra(EXTRA_DRM_LICENSE_URL) ?: ""
        imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
        userAgent = intent.getStringExtra(EXTRA_USER_AGENT)
        referrer = intent.getStringExtra(EXTRA_REFERRER)

        binding.tvChannelName.text = channelName
        binding.btnBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { playStream() }

        imageUrl?.let { url ->
            if (url.isNotEmpty()) {
                binding.ivChannelLogo.visibility = View.VISIBLE
                Glide.with(this).load(url).circleCrop().into(binding.ivChannelLogo)
            }
        }

        initPlayer()
        playStream()
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this)
            .build().apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
        binding.playerView.player = player
        binding.playerView.setKeepScreenOn(true)
        binding.playerView.useController = true

        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> showLoading()
                    Player.STATE_READY -> showPlayer()
                    Player.STATE_ENDED -> showError(getString(R.string.stream_error))
                    Player.STATE_IDLE -> { }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                showError(error.localizedMessage ?: getString(R.string.stream_error))
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                if (videoSize.width > 0 && videoSize.height > 0) {
                    showPlayer()
                }
            }

            override fun onRenderedFirstFrame() {
                showPlayer()
            }
        })
    }

    private fun playStream() {
        if (channelUrl.isEmpty()) {
            showError(getString(R.string.stream_error))
            return
        }
        showLoading()

        player?.stop()
        player?.clearMediaItems()

        val effectiveUserAgent = userAgent ?: "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        val httpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)

        if (!referrer.isNullOrEmpty()) {
            httpClientBuilder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Referer", referrer!!)
                    .header("User-Agent", effectiveUserAgent)
                    .build()
                chain.proceed(request)
            }
        }

        val httpClient = httpClientBuilder.build()
        val dataSourceFactory = OkHttpDataSource.Factory(httpClient)
            .setUserAgent(effectiveUserAgent)

        val mediaSource: MediaSource = if (isDrm && drmKeyId.isNotEmpty() && drmKey.isNotEmpty()) {
            buildDrmSource(dataSourceFactory)
        } else if (channelUrl.contains(".mpd", ignoreCase = true) || channelUrl.contains("/dash/", ignoreCase = true)) {
            DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(channelUrl))
        } else if (channelUrl.contains(".m3u8", ignoreCase = true) || channelUrl.contains("/hls/", ignoreCase = true)) {
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
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

    private fun buildDrmSource(dataSourceFactory: OkHttpDataSource.Factory): MediaSource {
        val cleanKeyId = drmKeyId.replace("-", "").lowercase()
        val cleanKey = drmKey.replace("-", "").lowercase()

        val initData = buildClearKeyJsonResponse(cleanKeyId, cleanKey)
        val drmCallback = LocalMediaDrmCallback(initData)

        val drmSessionManager = DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, androidx.media3.exoplayer.drm.FrameworkMediaDrm.DEFAULT_PROVIDER)
            .build(drmCallback)

        return DashMediaSource.Factory(dataSourceFactory)
            .setDrmSessionManagerProvider { drmSessionManager }
            .createMediaSource(MediaItem.fromUri(channelUrl))
    }

    private fun buildClearKeyJsonResponse(keyId: String, key: String): ByteArray {
        val keyIdB64 = hexToBase64Url(keyId)
        val keyB64 = hexToBase64Url(key)
        val json = """{"keys":[{"kty":"oct","k":"$keyB64","kid":"$keyIdB64"}],"type":"temporary"}"""
        return json.toByteArray(Charsets.UTF_8)
    }

    private fun hexToBase64Url(hex: String): String {
        val bytes = hexToBytes(hex)
        return android.util.Base64.encodeToString(bytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
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

    override fun onPause() { super.onPause(); player?.pause() }
    override fun onResume() { super.onResume(); player?.play() }
    override fun onDestroy() { super.onDestroy(); player?.release(); player = null }
}
