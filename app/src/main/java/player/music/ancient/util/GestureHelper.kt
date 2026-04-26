package player.music.ancient.util

import android.content.Context
import android.media.AudioManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import kotlin.math.abs

class GestureHelper(
    private val context: Context,
    private val window: Window,
    private val view: View,
    private val listener: GestureListener
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private var brightness = -1.0f
    private var volume = -1

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (e1 == null) return false

            val deltaY = e1.y - e2.y
            val deltaX = e1.x - e2.x

            if (abs(deltaX) > abs(deltaY)) {
                // Horizontal scroll (maybe for seeking later)
                return false
            }

            val viewWidth = view.width
            val isLeftSide = e1.x < viewWidth / 2f

            if (isLeftSide) {
                // Adjust Brightness
                if (brightness < 0) {
                    brightness = window.attributes.screenBrightness
                    if (brightness <= 0.00f) {
                        brightness = 0.50f
                    } else if (brightness < 0.01f) {
                        brightness = 0.01f
                    }
                }
                
                var lParams = window.attributes
                lParams.screenBrightness = brightness + (deltaY / view.height)
                if (lParams.screenBrightness > 1.0f) {
                    lParams.screenBrightness = 1.0f
                } else if (lParams.screenBrightness < 0.01f) {
                    lParams.screenBrightness = 0.01f
                }
                window.attributes = lParams
                listener.onBrightnessChanged((lParams.screenBrightness * 100).toInt())
            } else {
                // Adjust Volume
                if (volume < 0) {
                    volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                }
                
                var vol = volume + (deltaY / view.height) * maxVolume
                if (vol > maxVolume) {
                    vol = maxVolume.toFloat()
                } else if (vol < 0) {
                    vol = 0f
                }
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol.toInt(), 0)
                listener.onVolumeChanged((vol / maxVolume * 100).toInt())
            }
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            brightness = -1.0f
            volume = -1
            return true
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            listener.onGestureEnd()
        }
        return gestureDetector.onTouchEvent(event)
    }

    interface GestureListener {
        fun onBrightnessChanged(percent: Int)
        fun onVolumeChanged(percent: Int)
        fun onGestureEnd()
    }
}
