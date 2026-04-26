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
package player.music.ancient.fragments.other

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import player.music.ancient.R
import player.music.ancient.databinding.FragmentMiniPlayerBinding
import player.music.ancient.extensions.accentColor
import player.music.ancient.extensions.show
import player.music.ancient.extensions.textColorPrimary
import player.music.ancient.extensions.textColorSecondary
import player.music.ancient.fragments.base.AbsMusicServiceFragment
import player.music.ancient.glide.AncientGlideExtension
import player.music.ancient.glide.AncientGlideExtension.songCoverOptions
import player.music.ancient.helper.MusicPlayerRemote
import player.music.ancient.helper.MusicProgressViewUpdateHelper
import player.music.ancient.helper.PlayPauseButtonOnClickHandler
import player.music.ancient.util.ArtistSeparator
import player.music.ancient.util.PreferenceUtil
import player.music.ancient.util.AncientUtil
import com.bumptech.glide.Glide
import kotlin.math.abs

open class MiniPlayerFragment : AbsMusicServiceFragment(R.layout.fragment_mini_player),
    MusicProgressViewUpdateHelper.Callback, View.OnClickListener {

    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper

    private var dX = 0f
    private var dY = 0f
    private var initialX = 0f
    private var initialY = 0f
    private val CLICK_DRAG_TOLERANCE = 10f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this)
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMiniPlayerBinding.bind(view)
        
        // Restore position
        val savedX = PreferenceUtil.miniPlayerX
        val savedY = PreferenceUtil.miniPlayerY
        if (savedX != -1f && savedY != -1f) {
            view.x = savedX
            view.y = savedY
        }

        view.setOnTouchListener(object : View.OnTouchListener {
            private val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    if (abs(velocityX) > abs(velocityY)) {
                        if (velocityX < 0) {
                            MusicPlayerRemote.playNextSong()
                            return true
                        } else if (velocityX > 0) {
                            MusicPlayerRemote.back()
                            return true
                        }
                    }
                    return false
                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    (requireActivity() as? player.music.ancient.activities.base.AbsSlidingMusicPanelActivity)?.expandPanel()
                    return true
                }
            })

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (gestureDetector.onTouchEvent(event)) return true

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = v.x - event.rawX
                        dY = v.y - event.rawY
                        initialX = event.rawX
                        initialY = event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val newX = event.rawX + dX
                        val newY = event.rawY + dY
                        
                        // Constrain within parent bounds
                        val parent = v.parent as? View
                        if (parent != null) {
                            v.x = newX.coerceIn(0f, (parent.width - v.width).toFloat())
                            v.y = newY.coerceIn(0f, (parent.height - v.height).toFloat())
                        } else {
                            v.x = newX
                            v.y = newY
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        val distance = Math.sqrt(
                            Math.pow((event.rawX - initialX).toDouble(), 2.0) +
                                    Math.pow((event.rawY - initialY).toDouble(), 2.0)
                        )
                        if (distance > CLICK_DRAG_TOLERANCE) {
                            PreferenceUtil.miniPlayerX = v.x
                            PreferenceUtil.miniPlayerY = v.y
                        } else {
                            v.performClick()
                        }
                    }
                }
                return true
            }
        })
        setUpMiniPlayer()
        setUpButtons()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.actionNext -> MusicPlayerRemote.playNextSong()
            R.id.actionPrevious -> MusicPlayerRemote.back()
            else -> (requireActivity() as? player.music.ancient.activities.base.AbsSlidingMusicPanelActivity)?.expandPanel()
        }
    }

    fun setUpButtons() {
        if (AncientUtil.isTablet) {
            binding.actionNext.show()
            binding.actionPrevious.show()
        } else {
            binding.actionNext.isVisible = PreferenceUtil.isExtraControls
            binding.actionPrevious.isVisible = PreferenceUtil.isExtraControls
        }
        binding.actionNext.setOnClickListener(this)
        binding.actionPrevious.setOnClickListener(this)
    }

    private fun setUpMiniPlayer() {
        setUpPlayPauseButton()
        binding.progressBar.accentColor()
    }

    private fun setUpPlayPauseButton() {
        binding.miniPlayerPlayPauseButton.setOnClickListener(PlayPauseButtonOnClickHandler())
    }

    private fun updateSongTitle() {
        val song = MusicPlayerRemote.currentSong

        val builder = SpannableStringBuilder()

        val title = song.title.toSpannable()
        title.setSpan(ForegroundColorSpan(textColorPrimary()), 0, title.length, 0)

        // Format artist names using ArtistSeparator
        val formattedArtistName = ArtistSeparator.split(song.artistName).joinToString(", ")
        val text = formattedArtistName.toSpannable()
        text.setSpan(ForegroundColorSpan(textColorSecondary()), 0, text.length, 0)

        builder.append(title).append(" • ").append(text)

        binding.miniPlayerTitle.isSelected = true
        binding.miniPlayerTitle.text = builder
    }

    private fun updateSongCover() {
        val song = MusicPlayerRemote.currentSong
        Glide.with(requireContext())
            .load(AncientGlideExtension.getSongModel(song))
            .transition(AncientGlideExtension.getDefaultTransition())
            .songCoverOptions(song)
            .into(binding.image)
    }

    override fun onServiceConnected() {
        updateSongTitle()
        updateSongCover()
        updatePlayPauseDrawableState()
    }

    override fun onPlayingMetaChanged() {
        updateSongTitle()
        updateSongCover()
    }

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState()
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        binding.progressBar.max = total
        binding.progressBar.progress = progress
    }

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
    }

    protected fun updatePlayPauseDrawableState() {
        if (MusicPlayerRemote.isPlaying) {
            binding.miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            binding.miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_play_arrow)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}