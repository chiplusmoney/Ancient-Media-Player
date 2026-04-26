package player.music.ancient.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import androidx.core.net.toUri
import player.music.ancient.R
import player.music.ancient.extensions.showToast
import player.music.ancient.extensions.uri
import player.music.ancient.model.Song
import player.music.ancient.service.playback.Playback
import player.music.ancient.util.logE

class StreamMediaPlayerPlayback(context: Context) : AudioManagerPlayback(context),
    Playback,
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnErrorListener {

    private val player = MediaPlayer()
    override var callbacks: Playback.PlaybackCallbacks? = null
    private var initialized = false
    private var playWhenReady = false

    init {
        player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        player.setOnCompletionListener(this)
        player.setOnErrorListener(this)
    }

    override val isInitialized: Boolean
        get() = initialized

    override val isPlaying: Boolean
        get() = initialized && (playWhenReady || player.isPlaying)

    override val audioSessionId: Int
        get() = player.audioSessionId

    override fun setDataSource(
        song: Song,
        force: Boolean,
        completion: (success: Boolean) -> Unit,
    ) {
        initialized = false
        playWhenReady = false
        try {
            player.reset()
            setDataSource(song.uri)
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player.setOnPreparedListener {
                initialized = true
                player.setOnPreparedListener(null)
                callbacks?.onPlayStateChanged()
                completion(true)
            }
            player.prepareAsync()
        } catch (error: Exception) {
            initialized = false
            completion(false)
            logE(error)
        }
    }

    override fun setNextDataSource(path: Uri?) = Unit

    override fun start(): Boolean {
        super.start()
        return runCatching {
            player.start()
            playWhenReady = true
            callbacks?.onPlayStateChanged()
            true
        }.getOrDefault(false)
    }

    override fun stop() {
        super.stop()
        runCatching { player.stop() }
        initialized = false
        playWhenReady = false
        callbacks?.onPlayStateChanged()
    }

    override fun release() {
        stop()
        player.release()
    }

    override fun pause(): Boolean {
        super.pause()
        return runCatching {
            if (player.isPlaying) {
                player.pause()
            }
            playWhenReady = false
            callbacks?.onPlayStateChanged()
            true
        }.getOrDefault(false)
    }

    override fun duration(): Int {
        return if (!initialized) {
            -1
        } else {
            runCatching { player.duration }.getOrDefault(-1)
        }
    }

    override fun position(): Int {
        return if (!initialized) {
            -1
        } else {
            runCatching { player.currentPosition }.getOrDefault(-1)
        }
    }

    override fun seek(whereto: Int, force: Boolean): Int {
        return runCatching {
            player.seekTo(whereto)
            whereto
        }.getOrDefault(-1)
    }

    override fun setVolume(vol: Float): Boolean {
        return runCatching {
            player.setVolume(vol, vol)
            true
        }.getOrDefault(false)
    }

    override fun setAudioSessionId(sessionId: Int): Boolean {
        return runCatching {
            player.audioSessionId = sessionId
            true
        }.getOrDefault(false)
    }

    override fun setCrossFadeDuration(duration: Int) = Unit

    override fun setPlaybackSpeedPitch(speed: Float, pitch: Float) {
        // Some stream hosts behave poorly when playback params are pushed into MediaPlayer.
        // We keep the live-radio path conservative and battery-light instead.
    }

    override fun onCompletion(mp: MediaPlayer?) {
        playWhenReady = false
        callbacks?.onPlayStateChanged()
        callbacks?.onTrackEnded()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        initialized = false
        playWhenReady = false
        callbacks?.onPlayStateChanged()
        context.showToast(R.string.unplayable_file)
        logE("StreamMediaPlayerPlayback error: $what/$extra")
        return false
    }

    private fun setDataSource(uri: Uri) {
        if (uri.scheme == "content") {
            player.setDataSource(context, uri)
            return
        }
        player.setDataSource(context, uri, STREAM_HEADERS)
    }

    companion object {
        private val STREAM_HEADERS = hashMapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
            "Icy-MetaData" to "1",
            "Accept" to "*/*",
            "Accept-Encoding" to "identity",
            "Connection" to "keep-alive",
        )
    }
}
