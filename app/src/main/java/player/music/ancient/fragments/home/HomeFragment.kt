/*
 * Copyright (c) 2020 Future Eternal.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package player.music.ancient.fragments.home

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import player.music.ancient.*
import player.music.ancient.adapter.HomeAdapter
import player.music.ancient.common.ATHToolbarActivity
import player.music.ancient.databinding.FragmentHomeBinding
import player.music.ancient.dialogs.CreatePlaylistDialog
import player.music.ancient.dialogs.ImportPlaylistDialog
import player.music.ancient.extensions.accentColor
import player.music.ancient.extensions.dip
import player.music.ancient.extensions.elevatedAccentColor
import player.music.ancient.extensions.setUpMediaRouteButton
import player.music.ancient.fragments.ReloadType
import player.music.ancient.fragments.base.AbsMainActivityFragment
import player.music.ancient.glide.AncientGlideExtension
import player.music.ancient.glide.AncientGlideExtension.profileBannerOptions
import player.music.ancient.glide.AncientGlideExtension.songCoverOptions
import player.music.ancient.glide.AncientGlideExtension.userProfileOptions
import player.music.ancient.helper.MusicPlayerRemote
import player.music.ancient.interfaces.IScrollHelper
import player.music.ancient.model.Song
import player.music.ancient.util.ColorUtil
import player.music.ancient.util.PreferenceUtil
import player.music.ancient.util.PreferenceUtil.userName
import player.music.ancient.util.ToolbarContentTintHelper

class HomeFragment :
    AbsMainActivityFragment(R.layout.fragment_home), IScrollHelper {

    private var _binding: HomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val homeBinding = FragmentHomeBinding.bind(view)
        _binding = HomeBinding(homeBinding)
        mainActivity.setSupportActionBar(binding.toolbar)
        mainActivity.supportActionBar?.title = null
        setupListeners()
        binding.titleWelcome.text = String.format("%s", userName)
        updateHomeActionsVisibility()

        enterTransition = MaterialFadeThrough().addTarget(binding.contentContainer)
        reenterTransition = MaterialFadeThrough().addTarget(binding.contentContainer)

        checkForMargins()

        val homeAdapter = HomeAdapter(mainActivity)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(mainActivity)
            adapter = homeAdapter
        }
        libraryViewModel.getSuggestions().observe(viewLifecycleOwner) {
            loadSuggestions(it)
        }
        libraryViewModel.getHome().observe(viewLifecycleOwner) {
            homeAdapter.swapData(it)
        }

        loadProfile()
        setupTitle()
        colorButtons()
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        view.doOnLayout { adjustPlaylistButtons() }
    }

    private fun adjustPlaylistButtons() {
        val buttons = listOf(
            binding.history,
            binding.lastAdded,
            binding.topPlayed,
            binding.actionShuffle,
            binding.chooseFolder,
            binding.actionRadio,
            binding.actionTv,
            binding.actionVideos,
            binding.actionYoutube
        ).filter { it.isVisible }
        if (buttons.isEmpty()) return
        buttons.maxOf { it.lineCount }.let { maxLineCount ->
            buttons.forEach { button ->
                button.setLines(maxLineCount)
            }
        }
    }

    private fun setupListeners() {
        binding.bannerImage?.setOnClickListener {
            findNavController().navigate(
                R.id.user_info_fragment, null, null, FragmentNavigatorExtras(
                    binding.userImage to "user_image"
                )
            )
            reenterTransition = null
        }

        binding.lastAdded.setOnClickListener {
            findNavController().navigate(
                R.id.detailListFragment,
                bundleOf(EXTRA_PLAYLIST_TYPE to LAST_ADDED_PLAYLIST)
            )
            setSharedAxisYTransitions()
        }

        binding.topPlayed.setOnClickListener {
            findNavController().navigate(
                R.id.detailListFragment,
                bundleOf(EXTRA_PLAYLIST_TYPE to TOP_PLAYED_PLAYLIST)
            )
            setSharedAxisYTransitions()
        }

        binding.actionShuffle.setOnClickListener {
            libraryViewModel.shuffleSongs()
        }

        binding.chooseFolder.setOnClickListener {
            findNavController().navigate(R.id.action_folder, null, navOptions)
            setSharedAxisYTransitions()
        }

        binding.actionRadio.setOnClickListener {
            findNavController().navigate(R.id.action_radio, null, navOptions)
            setSharedAxisYTransitions()
        }

        binding.actionTv.setOnClickListener {
            findNavController().navigate(R.id.action_tv, null, navOptions)
            setSharedAxisYTransitions()
        }

        binding.actionVideos.setOnClickListener {
            findNavController().navigate(R.id.action_video, null, navOptions)
            setSharedAxisYTransitions()
        }

        binding.actionYoutube.setOnClickListener {
            findNavController().navigate(R.id.action_youtube, null, navOptions)
            setSharedAxisYTransitions()
        }

        binding.history.setOnClickListener {
            findNavController().navigate(
                R.id.detailListFragment,
                bundleOf(EXTRA_PLAYLIST_TYPE to HISTORY_PLAYLIST)
            )
            setSharedAxisYTransitions()
        }

        binding.userImage.setOnClickListener {
            findNavController().navigate(
                R.id.user_info_fragment, null, null, FragmentNavigatorExtras(
                    binding.userImage to "user_image"
                )
            )
        }

        binding.suggestions.refreshButton.setOnClickListener {
            libraryViewModel.forceReload(ReloadType.Suggestions)
        }
    }

    private fun setupTitle() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigate(R.id.action_search, null, navOptions)
        }
        val hexColor = String.format("#%06X", 0xFFFFFF and accentColor())
        val appName = "Ancient <font color=$hexColor>Media</font>".parseAsHtml()
        binding.appBarLayout.title = appName
    }

    private fun loadProfile() {
        binding.bannerImage?.let {
            Glide.with(requireContext())
                .load(AncientGlideExtension.getBannerModel())
                .profileBannerOptions(AncientGlideExtension.getBannerModel())
                .into(it)
        }
        Glide.with(requireActivity())
            .load(AncientGlideExtension.getUserModel())
            .userProfileOptions(AncientGlideExtension.getUserModel(), requireContext())
            .into(binding.userImage)
    }

    private fun colorButtons() {
        binding.history.elevatedAccentColor()
        binding.lastAdded.elevatedAccentColor()
        binding.topPlayed.elevatedAccentColor()
        binding.actionShuffle.elevatedAccentColor()
        binding.chooseFolder.elevatedAccentColor()
        binding.actionRadio.elevatedAccentColor()
        binding.actionTv.elevatedAccentColor()
        binding.actionVideos.elevatedAccentColor()
        binding.actionYoutube.elevatedAccentColor()
    }

    private fun checkForMargins() {
        if (mainActivity.isBottomNavVisible) {
            binding.recyclerView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = dip(R.dimen.bottom_nav_height)
            }
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_layout_type)
        menu.removeItem(R.id.action_sort_order)
        menu.findItem(R.id.action_settings).setShowAsAction(SHOW_AS_ACTION_IF_ROOM)
        menu.add(Menu.NONE, MENU_MANAGE_HOME, Menu.NONE, "Manage Home")
            .setIcon(R.drawable.ic_more_horiz)
            .setShowAsAction(SHOW_AS_ACTION_IF_ROOM)
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(
            requireContext(),
            binding.toolbar,
            menu,
            ATHToolbarActivity.getToolbarBackgroundColor(binding.toolbar)
        )
        requireContext().setUpMediaRouteButton(menu)
    }

    override fun scrollToTop() {
        binding.container.scrollTo(0, 0)
        binding.appBarLayout.setExpanded(true)
    }

    fun setSharedAxisXTransitions() {
        exitTransition =
            MaterialSharedAxis(MaterialSharedAxis.X, true).addTarget(CoordinatorLayout::class.java)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    private fun setSharedAxisYTransitions() {
        exitTransition =
            MaterialSharedAxis(MaterialSharedAxis.Y, true).addTarget(CoordinatorLayout::class.java)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    private fun loadSuggestions(songs: List<Song>) {
        if (!PreferenceUtil.homeSuggestions || songs.isEmpty()) {
            binding.suggestions.root.isVisible = false
            return
        }
        val images = listOf(
            binding.suggestions.image1,
            binding.suggestions.image2,
            binding.suggestions.image3,
            binding.suggestions.image4,
            binding.suggestions.image5,
            binding.suggestions.image6,
            binding.suggestions.image7,
            binding.suggestions.image8
        )
        val color = accentColor()
        binding.suggestions.message.apply {
            setTextColor(color)
            setOnClickListener {
                it.isClickable = false
                it.postDelayed({ it.isClickable = true }, 500)
                MusicPlayerRemote.playNext(songs.subList(0, 8))
                if (!MusicPlayerRemote.isPlaying) {
                    MusicPlayerRemote.playNextSong()
                }
            }
        }
        binding.suggestions.card6.setCardBackgroundColor(ColorUtil.withAlpha(color, 0.12f))
        images.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                it.isClickable = false
                it.postDelayed({ it.isClickable = true }, 500)
                MusicPlayerRemote.playNext(songs[index])
                if (!MusicPlayerRemote.isPlaying) {
                    MusicPlayerRemote.playNextSong()
                }
            }
            Glide.with(this)
                .load(AncientGlideExtension.getSongModel(songs[index]))
                .songCoverOptions(songs[index])
                .into(imageView)
        }
    }

    private fun updateHomeActionsVisibility() {
        val shortcuts = PreferenceUtil.homeShortcuts
        val actions = homeActionViews()
        actions.forEach { action ->
            action.second.isVisible = action.first in shortcuts
        }
        binding.homeActions.isVisible = shortcuts.isNotEmpty()
        binding.root.post { adjustPlaylistButtons() }
    }

    private fun showManageHomeDialog() {
        val actions = homeActionViews()
        val labels = actions.map { action ->
            (action.second as? com.google.android.material.button.MaterialButton)?.text?.toString()
                .orEmpty()
        }.toTypedArray()
        val keys = actions.map { it.first }
        val selectedShortcuts = PreferenceUtil.homeShortcuts.toMutableSet()
        val checkedItems = keys.map { key -> key in selectedShortcuts }.toBooleanArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Manage Home")
            .setMultiChoiceItems(labels, checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    selectedShortcuts.add(keys[which])
                } else {
                    selectedShortcuts.remove(keys[which])
                }
            }
            .setPositiveButton("Save") { _, _ ->
                if (selectedShortcuts.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Choose at least one home shortcut",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                PreferenceUtil.homeShortcuts = selectedShortcuts
                updateHomeActionsVisibility()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun homeActionViews(): List<Pair<String, View>> = listOf(
        "history" to binding.history,
        "last_added" to binding.lastAdded,
        "top_played" to binding.topPlayed,
        "shuffle" to binding.actionShuffle,
        "folder" to binding.chooseFolder,
        "radio" to binding.actionRadio,
        "tv" to binding.actionTv,
        "videos" to binding.actionVideos,
        "youtube" to binding.actionYoutube
    )

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_MANAGE_HOME -> {
                showManageHomeDialog()
                return true
            }

            R.id.action_settings -> findNavController().navigate(
                R.id.settings_fragment,
                null,
                navOptions
            )

            R.id.action_import_playlist -> ImportPlaylistDialog().show(
                childFragmentManager,
                "ImportPlaylist"
            )

            R.id.action_add_to_playlist -> CreatePlaylistDialog.create(emptyList()).show(
                childFragmentManager,
                "ShowCreatePlaylistDialog"
            )

            R.id.action_radio -> findNavController().navigate(
                R.id.action_radio,
                null,
                navOptions
            )
        }
        return false
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(requireActivity(), binding.toolbar)
    }

    override fun onResume() {
        super.onResume()
        checkForMargins()
        libraryViewModel.forceReload(ReloadType.HomeSections)
        exitTransition = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MENU_MANAGE_HOME = 2301
        const val TAG: String = "BannerHomeFragment"

        @JvmStatic
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }
}
