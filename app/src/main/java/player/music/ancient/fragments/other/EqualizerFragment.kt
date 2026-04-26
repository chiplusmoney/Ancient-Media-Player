package player.music.ancient.fragments.other

import android.media.audiofx.Equalizer
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.slider.Slider
import player.music.ancient.R
import player.music.ancient.databinding.FragmentEqualizerBinding
import player.music.ancient.databinding.ItemEqualizerBandBinding
import player.music.ancient.extensions.*
import player.music.ancient.fragments.base.AbsMusicServiceFragment
import player.music.ancient.helper.MusicPlayerRemote
import player.music.ancient.util.PreferenceUtil

class EqualizerFragment : AbsMusicServiceFragment(R.layout.fragment_equalizer) {
    private var _binding: FragmentEqualizerBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEqualizerBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.toolbar.setTitleTextColor(accentColor())
        binding.toolbar.navigationIcon?.setTint(accentColor())

        setupEqualizer()
    }

    private fun setupEqualizer() {
        val manager = MusicPlayerRemote.audioEffectManager ?: return

        binding.equalizerSwitch.isChecked = PreferenceUtil.equalizerEnabled
        binding.equalizerSwitch.thumbTintList = accentColor().colorStateList
        binding.equalizerSwitch.trackTintList = accentColor().addAlpha(0.5f).colorStateList
        binding.equalizerSwitch.setOnCheckedChangeListener { _, isChecked ->
            manager.setEnabled(isChecked)
            updateUIState(isChecked)
        }

        setupPresets()
        setupBands()
        setupBassBoost()
        setupVirtualizer()

        updateUIState(PreferenceUtil.equalizerEnabled)
    }

    private fun setupPresets() {
        val manager = MusicPlayerRemote.audioEffectManager ?: return
        val audioSessionId = MusicPlayerRemote.audioSessionId
        if (audioSessionId == -1) return

        try {
            val equalizer = Equalizer(0, audioSessionId)
            val presets = mutableListOf<String>()
            for (i in 0 until equalizer.numberOfPresets.toInt()) {
                presets.add(equalizer.getPresetName(i.toShort()))
            }
            equalizer.release()

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, presets)
            binding.presetSpinner.setAdapter(adapter)

            val currentPreset = PreferenceUtil.equalizerPreset
            if (currentPreset != -1 && currentPreset < presets.size) {
                binding.presetSpinner.setText(presets[currentPreset], false)
            }

            binding.presetSpinner.setOnItemClickListener { _, _, position, _ ->
                manager.usePreset(position.toShort())
                // Refresh bands after preset change
                binding.bandsContainer.removeAllViews()
                setupBands()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupBands() {
        val manager = MusicPlayerRemote.audioEffectManager ?: return
        val audioSessionId = MusicPlayerRemote.audioSessionId
        if (audioSessionId == -1) return

        try {
            val equalizer = Equalizer(0, audioSessionId)
            val minLevel = equalizer.bandLevelRange[0]
            val maxLevel = equalizer.bandLevelRange[1]
            val numBands = equalizer.numberOfBands

            for (i in 0 until numBands.toInt()) {
                val band = i.toShort()
                val bandBinding = ItemEqualizerBandBinding.inflate(layoutInflater, binding.bandsContainer, true)
                
                val freq = equalizer.getCenterFreq(band) / 1000
                val freqText = if (freq < 1000) "${freq} Hz" else "${freq / 1000} kHz"
                bandBinding.bandFrequency.text = freqText
                
                bandBinding.bandSlider.valueFrom = minLevel.toFloat()
                bandBinding.bandSlider.valueTo = maxLevel.toFloat()
                bandBinding.bandSlider.value = equalizer.getBandLevel(band).toFloat()

                bandBinding.bandSlider.addOnChangeListener { _, value, fromUser ->
                    if (fromUser) {
                        manager.setBandLevel(band, value.toInt().toShort())
                        binding.presetSpinner.setText("", false)
                    }
                }
            }
            equalizer.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupBassBoost() {
        val manager = MusicPlayerRemote.audioEffectManager ?: return
        binding.bassBoostSlider.value = PreferenceUtil.bassBoostStrength.toFloat()
        binding.bassBoostSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                manager.setBassBoostStrength(value.toInt().toShort())
            }
        }
    }

    private fun setupVirtualizer() {
        val manager = MusicPlayerRemote.audioEffectManager ?: return
        binding.virtualizerSlider.value = PreferenceUtil.virtualizerStrength.toFloat()
        binding.virtualizerSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                manager.setVirtualizerStrength(value.toInt().toShort())
            }
        }
    }

    private fun updateUIState(enabled: Boolean) {
        binding.presetSpinner.isEnabled = enabled
        binding.bassBoostSlider.isEnabled = enabled
        binding.virtualizerSlider.isEnabled = enabled
        for (i in 0 until binding.bandsContainer.childCount) {
            val child = binding.bandsContainer.getChildAt(i)
            child.isEnabled = enabled
            if (child is android.view.ViewGroup) {
                for (j in 0 until child.childCount) {
                    child.getChildAt(j).isEnabled = enabled
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        setupEqualizer()
    }
}
