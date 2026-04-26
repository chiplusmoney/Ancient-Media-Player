package player.music.ancient.activities

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.transition.ChangeBounds
import android.transition.ChangeImageTransform
import android.transition.Fade
import android.transition.TransitionSet
import android.util.Rational
import android.view.MenuItem
import android.view.Window
import androidx.activity.addCallback
import androidx.annotation.OptIn
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.bumptech.glide.Glide
import player.music.ancient.R
import player.music.ancient.activities.base.AbsThemeActivity
import player.music.ancient.databinding.ActivityVideoPlayerBinding
import player.music.ancient.util.GestureHelper
import player.music.ancient.util.PreferenceUtil

class VideoPlayerActivity : AbsThemeActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var gestureHelper: GestureHelper? = null
    private var mediaUri: Uri? = null
    private var launchTransitionName: String? = null
    private var videoAspectRatio = Rational(16, 9)
    private var isStreamingSource = false
    private var isInteractiveScreen = true
    private var isScreenReceiverRegistered = false
    private var suppressPictureInPictureOnPause = false
    private lateinit var defaultTrackSelectionParameters: DefaultTrackSelector.Parameters

    private val hideFeedbackRunnable = Runnable {
        if (!::binding.isInitialized) return@Runnable
        binding.feedbackOverlay.animate()
            .alpha(0f)
            .setDuration(140L)
            .withEndAction {
                binding.feedbackOverlay.isVisible = false
            }
            .start()
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isInteractiveScreen = false
                    applyPlaybackBatteryPolicy()
                }

                Intent.ACTION_SCREEN_ON -> {
                    isInteractiveScreen = true
                    applyPlaybackBatteryPolicy()
                }
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            fadeOutPoster()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                videoAspectRatio = Rational(videoSize.width, videoSize.height)
                updatePictureInPictureParams()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            applyPlaybackBatteryPolicy()
            updatePictureInPictureParams()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        configureTransitions()
        supportPostponeEnterTransition()

        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.posterView.doOnPreDraw { supportStartPostponedEnterTransition() }

        mediaUri = intent.getStringExtra(EXTRA_URI)?.let(Uri::parse)
        val resolvedMediaUri = mediaUri ?: run {
            finish()
            return
        }

        launchTransitionName = intent.getStringExtra(EXTRA_TRANSITION_NAME)?.takeIf { it.isNotBlank() }
        launchTransitionName?.let { ViewCompat.setTransitionName(binding.posterView, it) }

        val referer = intent.getStringExtra(EXTRA_REFERER)
        val origin = intent.getStringExtra(EXTRA_ORIGIN)
        val userAgent = intent.getStringExtra(EXTRA_USER_AGENT)?.takeIf { it.isNotBlank() }
        isStreamingSource =
            resolvedMediaUri.scheme.equals("http", true) || resolvedMediaUri.scheme.equals("https", true)
        val isStreaming = isStreamingSource
        isInteractiveScreen = getSystemService(PowerManager::class.java)?.isInteractive ?: true

        if (isStreaming) {
            binding.posterView.isVisible = false
        } else {
            binding.posterView.isVisible = true
            Glide.with(this)
                .load(resolvedMediaUri)
                .centerCrop()
                .into(binding.posterView)
        }

        trackSelector = DefaultTrackSelector(this).apply {
            defaultTrackSelectionParameters = parameters
            parameters = buildTrackSelectionParameters(disableVideo = false)
        }

        val requestHeaders = buildMap<String, String> {
            referer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
            origin?.takeIf { it.isNotBlank() }?.let { put("Origin", it) }
        }
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(userAgent ?: DEFAULT_USER_AGENT)
            .setDefaultRequestProperties(requestHeaders)
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector!!)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .also { exoPlayer ->
                exoPlayer.addListener(playerListener)
                exoPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true
                )
                exoPlayer.setHandleAudioBecomingNoisy(true)
                binding.playerView.player = exoPlayer
                exoPlayer.setMediaItem(MediaItem.fromUri(resolvedMediaUri))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }

        applyPlaybackBatteryPolicy()
        updatePictureInPictureParams()

        onBackPressedDispatcher.addCallback(this) {
            finishWithTransition()
        }

        setupGestures()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        gestureHelper = GestureHelper(
            this,
            window,
            binding.playerView,
            object : GestureHelper.GestureListener {
                override fun onBrightnessChanged(percent: Int) {
                    showFeedbackOverlay(R.drawable.ic_brightness, percent)
                }

                override fun onVolumeChanged(percent: Int) {
                    showFeedbackOverlay(R.drawable.ic_volume_up, percent)
                }

                override fun onGestureEnd() {
                    binding.feedbackOverlay.removeCallbacks(hideFeedbackRunnable)
                    binding.feedbackOverlay.postDelayed(hideFeedbackRunnable, FEEDBACK_HIDE_DELAY_MS)
                }
            }
        )

        binding.playerView.setOnTouchListener { _, event ->
            gestureHelper?.onTouchEvent(event) ?: false
        }
    }

    private fun configureTransitions() {
        window.sharedElementEnterTransition = buildSharedElementTransition()
        window.sharedElementReturnTransition = buildSharedElementTransition()
        window.enterTransition = Fade().apply { duration = WINDOW_FADE_DURATION_MS }
        window.returnTransition = Fade().apply { duration = WINDOW_FADE_DURATION_MS }
    }

    private fun buildSharedElementTransition(): TransitionSet {
        return TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(ChangeImageTransform())
            .apply {
                duration = SHARED_ELEMENT_DURATION_MS
                interpolator = FastOutSlowInInterpolator()
            }
    }

    private fun showFeedbackOverlay(iconRes: Int, percent: Int) {
        binding.feedbackOverlay.removeCallbacks(hideFeedbackRunnable)
        binding.feedbackIcon.setImageResource(iconRes)
        binding.feedbackText.text = getString(R.string.video_player_feedback_percent, percent)
        if (!binding.feedbackOverlay.isVisible) {
            binding.feedbackOverlay.alpha = 0f
            binding.feedbackOverlay.isVisible = true
        }
        binding.feedbackOverlay.animate().alpha(1f).setDuration(120L).start()
        binding.feedbackOverlay.postDelayed(hideFeedbackRunnable, FEEDBACK_HIDE_DELAY_MS)
    }

    private fun fadeOutPoster() {
        if (!binding.posterView.isVisible) return
        binding.posterView.animate()
            .alpha(0f)
            .setDuration(180L)
            .withEndAction {
                binding.posterView.isVisible = false
            }
            .start()
    }

    private fun finishWithTransition() {
        suppressPictureInPictureOnPause = true
        binding.feedbackOverlay.removeCallbacks(hideFeedbackRunnable)
        if (isInPictureInPictureModeCompat()) {
            finish()
            return
        }
        if (launchTransitionName != null && binding.posterView.alpha < 1f) {
            binding.posterView.alpha = 1f
            binding.posterView.isVisible = true
        }
        supportFinishAfterTransition()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            enterPictureInPictureIfPossible()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!::binding.isInitialized) return

        binding.feedbackOverlay.removeCallbacks(hideFeedbackRunnable)
        binding.feedbackOverlay.isVisible = false
        binding.playerView.useController = !isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            binding.playerView.hideController()
        }
        applyPlaybackBatteryPolicy()
    }

    override fun onResume() {
        super.onResume()
        suppressPictureInPictureOnPause = false
        if (::binding.isInitialized) {
            binding.playerView.onResume()
            binding.playerView.useController = !isInPictureInPictureModeCompat()
        }
        isInteractiveScreen = getSystemService(PowerManager::class.java)?.isInteractive ?: true
        applyPlaybackBatteryPolicy()
        updatePictureInPictureParams()
    }

    override fun onPause() {
        if (::binding.isInitialized) {
            binding.feedbackOverlay.removeCallbacks(hideFeedbackRunnable)
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !suppressPictureInPictureOnPause &&
                isInteractiveScreen
            ) {
                enterPictureInPictureIfPossible()
            }
            binding.playerView.onPause()
        }
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        registerScreenReceiverIfNeeded()
    }

    override fun onStop() {
        if (::binding.isInitialized) {
            binding.feedbackOverlay.removeCallbacks(hideFeedbackRunnable)
        }
        unregisterScreenReceiverIfNeeded()
        super.onStop()
    }

    override fun onDestroy() {
        unregisterScreenReceiverIfNeeded()
        player?.removeListener(playerListener)
        player?.release()
        player = null
        trackSelector = null
        if (::binding.isInitialized) {
            binding.playerView.player = null
        }
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finishWithTransition()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun enterPictureInPictureIfPossible() {
        if (!supportsPictureInPicture()) return
        if (isInPictureInPictureModeCompat()) return
        val activePlayer = player ?: return
        if (activePlayer.playbackState == Player.STATE_IDLE || activePlayer.playbackState == Player.STATE_ENDED) {
            return
        }
        runCatching {
            enterPictureInPictureMode(buildPictureInPictureParams())
        }
    }

    private fun buildTrackSelectionParameters(disableVideo: Boolean): DefaultTrackSelector.Parameters {
        return defaultTrackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, disableVideo)
            .apply {
                if (isStreamingSource && PreferenceUtil.batterySaverMode) {
                    setForceLowestBitrate(true)
                    setMaxVideoSizeSd()
                    setMaxVideoBitrate(BATTERY_SAVER_MAX_VIDEO_BITRATE)
                }
            }
            .build()
    }

    private fun applyPlaybackBatteryPolicy() {
        val selector = trackSelector ?: return
        val activePlayer = player ?: return
        val disableVideo = !isInteractiveScreen &&
            !isInPictureInPictureModeCompat() &&
            activePlayer.playbackState != Player.STATE_IDLE &&
            activePlayer.playbackState != Player.STATE_ENDED
        selector.parameters = buildTrackSelectionParameters(disableVideo)
    }

    private fun registerScreenReceiverIfNeeded() {
        if (isScreenReceiverRegistered) return
        registerReceiver(
            screenStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
        )
        isScreenReceiverRegistered = true
    }

    private fun unregisterScreenReceiverIfNeeded() {
        if (!isScreenReceiverRegistered) return
        unregisterReceiver(screenStateReceiver)
        isScreenReceiverRegistered = false
    }

    private fun updatePictureInPictureParams() {
        if (!supportsPictureInPicture()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setPictureInPictureParams(buildPictureInPictureParams())
        }
    }

    private fun buildPictureInPictureParams(): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(videoAspectRatio)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(player?.isPlaying == true)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun supportsPictureInPicture(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    private fun isInPictureInPictureModeCompat(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode
    }

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_URI = "extra_uri"
        private const val EXTRA_REFERER = "extra_referer"
        private const val EXTRA_ORIGIN = "extra_origin"
        private const val EXTRA_USER_AGENT = "extra_user_agent"
        private const val EXTRA_TRANSITION_NAME = "extra_transition_name"
        private const val SHARED_ELEMENT_DURATION_MS = 320L
        private const val WINDOW_FADE_DURATION_MS = 180L
        private const val FEEDBACK_HIDE_DELAY_MS = 280L
        private const val BATTERY_SAVER_MAX_VIDEO_BITRATE = 900_000
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        fun transitionNameFor(videoId: Long): String = "video_poster_$videoId"

        fun intent(
            context: Context,
            title: String,
            uri: String,
            referer: String? = null,
            origin: String? = null,
            userAgent: String? = null,
            transitionName: String? = null,
        ): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_URI, uri)
                referer?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_REFERER, it) }
                origin?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_ORIGIN, it) }
                userAgent?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_USER_AGENT, it) }
                transitionName?.takeIf { it.isNotBlank() }?.let {
                    putExtra(EXTRA_TRANSITION_NAME, it)
                }
            }
        }
    }
}
