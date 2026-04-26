package player.music.ancient.service

import android.content.Context
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import player.music.ancient.util.PreferenceUtil

class AudioEffectManager(private val context: Context) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    fun onAudioSessionId(audioSessionId: Int) {
        if (audioSessionId == 0) return

        release()

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = PreferenceUtil.equalizerEnabled
                applySettings()
            }

            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = PreferenceUtil.equalizerEnabled
                if (strengthSupported) {
                    setStrength(PreferenceUtil.bassBoostStrength.toShort())
                }
            }

            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = PreferenceUtil.equalizerEnabled
                if (strengthSupported) {
                    setStrength(PreferenceUtil.virtualizerStrength.toShort())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun Equalizer.applySettings() {
        try {
            val preset = PreferenceUtil.equalizerPreset
            if (preset != -1 && preset < numberOfPresets) {
                usePreset(preset.toShort())
            } else {
                val bands = PreferenceUtil.equalizerBands
                if (bands.isNotEmpty()) {
                    val bandLevels = bands.split(":").map { it.toShort() }
                    for (i in bandLevels.indices) {
                        if (i < numberOfBands) {
                            setBandLevel(i.toShort(), bandLevels[i])
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEnabled(enabled: Boolean) {
        PreferenceUtil.equalizerEnabled = enabled
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
            saveBands()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveBands() {
        equalizer?.let {
            val bands = StringBuilder()
            for (i in 0 until it.numberOfBands) {
                bands.append(it.getBandLevel(i.toShort()))
                if (i < it.numberOfBands - 1) {
                    bands.append(":")
                }
            }
            PreferenceUtil.equalizerBands = bands.toString()
            PreferenceUtil.equalizerPreset = -1
        }
    }

    fun usePreset(preset: Short) {
        try {
            equalizer?.usePreset(preset)
            PreferenceUtil.equalizerPreset = preset.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBassBoostStrength(strength: Short) {
        try {
            PreferenceUtil.bassBoostStrength = strength.toInt()
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(strength)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVirtualizerStrength(strength: Short) {
        try {
            PreferenceUtil.virtualizerStrength = strength.toInt()
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(strength)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        equalizer?.release()
        equalizer = null
        bassBoost?.release()
        bassBoost = null
        virtualizer?.release()
        virtualizer = null
    }
}
