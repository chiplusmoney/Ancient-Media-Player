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
package player.music.ancient.fragments.player.color

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import player.music.ancient.util.ToolbarContentTintHelper
import player.music.ancient.R
import player.music.ancient.databinding.FragmentColorPlayerBinding
import player.music.ancient.extensions.colorControlNormal
import player.music.ancient.extensions.drawAboveSystemBars
import player.music.ancient.extensions.whichFragment
import player.music.ancient.fragments.base.AbsPlayerFragment
import player.music.ancient.fragments.player.PlayerAlbumCoverFragment
import player.music.ancient.helper.MusicPlayerRemote
import player.music.ancient.model.Song
import player.music.ancient.util.color.MediaNotificationProcessor

class ColorFragment : AbsPlayerFragment(R.layout.fragment_color_player) {

    private var lastColor: Int = 0
    private var navigationColor: Int = 0
    private lateinit var playbackControlsFragment: ColorPlaybackControlsFragment
    private var valueAnimator: ValueAnimator? = null
    private var _binding: FragmentColorPlayerBinding? = null
    private val binding get() = _binding!!

    override fun playerToolbar(): Toolbar {
        return binding.playerToolbar
    }

    override val paletteColor: Int
        get() = navigationColor

    override fun onColorChanged(color: MediaNotificationProcessor) {
        libraryViewModel.updateColor(color.backgroundColor)
        lastColor = color.secondaryTextColor
        playbackControlsFragment.setColor(color)
        navigationColor = color.backgroundColor

        binding.colorGradientBackground.setBackgroundColor(color.backgroundColor)
        val animator =
            playbackControlsFragment.createRevealAnimator(binding.colorGradientBackground)
        animator.doOnEnd {
            _binding?.root?.setBackgroundColor(color.backgroundColor)
        }
        animator.start()
        binding.playerToolbar.post {
            ToolbarContentTintHelper.colorizeToolbar(
                binding.playerToolbar,
                color.secondaryTextColor,
                requireActivity()
            )
        }
    }

    override fun onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.currentSong)
    }

    override fun onShow() {
        playbackControlsFragment.show()
    }

    override fun onHide() {
        playbackControlsFragment.hide()
    }

    override fun toolbarIconColor(): Int {
        return lastColor
    }

    override fun toggleFavorite(song: Song) {
        super.toggleFavorite(song)
        if (song.id == MusicPlayerRemote.currentSong.id) {
            updateIsFavorite()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (valueAnimator != null) {
            valueAnimator!!.cancel()
            valueAnimator = null
        }
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentColorPlayerBinding.bind(view)
        setUpSubFragments()
        setUpPlayerToolbar()
        val playerAlbumCoverFragment: PlayerAlbumCoverFragment =
            whichFragment(R.id.playerAlbumCoverFragment)
        playerAlbumCoverFragment.setCallbacks(this)
        playerToolbar().drawAboveSystemBars()
    }

    private fun setUpSubFragments() {
        playbackControlsFragment = whichFragment(R.id.playbackControlsFragment)
    }

    private fun setUpPlayerToolbar() {
        binding.playerToolbar.apply {
            inflateMenu(R.menu.menu_player)
            setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
            setOnMenuItemClickListener(this@ColorFragment)
            ToolbarContentTintHelper.colorizeToolbar(
                this,
                colorControlNormal(),
                requireActivity()
            )
        }
    }

    companion object {
        fun newInstance(): ColorFragment {
            return ColorFragment()
        }
    }
}
