/*
 * Copyright (c) 2020 Future Eternal.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package player.music.ancient.fragments.settings

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.media.audiofx.AudioEffect
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.preference.Preference
import player.music.ancient.util.VersionUtils
import player.music.ancient.BLUETOOTH_PLAYBACK
import player.music.ancient.EQUALIZER
import player.music.ancient.R
import player.music.ancient.activities.base.AbsBaseActivity.Companion.BLUETOOTH_PERMISSION_REQUEST
import player.music.ancient.util.NavigationUtil

/**
 * @author Hemanth S (h4h13).
 */

class AudioSettings : AbsSettingsFragment() {
    override fun invalidateSettings() {
        val eqPreference: Preference? = findPreference(EQUALIZER)
        eqPreference?.isEnabled = true
        eqPreference?.setOnPreferenceClickListener {
            NavigationUtil.openEqualizer(requireActivity())
            true
        }
        val bluetoothPreference: Preference? = findPreference(BLUETOOTH_PLAYBACK)
        if (VersionUtils.hasS()) {
            bluetoothPreference?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            BLUETOOTH_CONNECT
                        ) != PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            requireActivity(), arrayOf(
                                BLUETOOTH_CONNECT
                            ), BLUETOOTH_PERMISSION_REQUEST
                        )
                    }
                }
                return@setOnPreferenceChangeListener true
            }
        }
    }

    private fun hasEqualizer(): Boolean {
        val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)

        val pm = requireActivity().packageManager
        val ri = pm.resolveActivity(effects, 0)
        return ri != null
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_audio)
    }
}
