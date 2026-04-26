package player.music.ancient.fragments.youtube

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import player.music.ancient.R
import player.music.ancient.common.ATHToolbarActivity
import player.music.ancient.databinding.DialogAddYoutubeChannelBinding
import player.music.ancient.databinding.FragmentYoutubeBinding
import player.music.ancient.db.YoutubeChannelEntity
import player.music.ancient.fragments.base.AbsMainActivityFragment
import player.music.ancient.util.PreferenceUtil
import player.music.ancient.util.RadioImageStore
import player.music.ancient.util.ToolbarContentTintHelper

class YoutubeFragment : AbsMainActivityFragment(R.layout.fragment_youtube) {
    private val viewModel: YoutubeViewModel by viewModel()
    private var _binding: FragmentYoutubeBinding? = null
    private val binding get() = _binding!!

    private var pendingImageCallback: ((String) -> Unit)? = null
    private var seedStarted = false

    private val artworkPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = pendingImageCallback ?: return@registerForActivityResult
            pendingImageCallback = null
            if (result.resultCode == Activity.RESULT_OK) {
                val pickedUri = result.data?.data ?: return@registerForActivityResult
                lifecycleScope.launch {
                    val savedImageUri =
                        RadioImageStore.persistStationImage(requireContext(), pickedUri)
                    if (savedImageUri == null) {
                        Toast.makeText(
                            requireContext(),
                            "Couldn't save the selected image",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        callback(savedImageUri)
                    }
                }
            } else if (result.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(
                    requireContext(),
                    ImagePicker.getError(result.data),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private lateinit var feedAdapter: YoutubeFeedAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentYoutubeBinding.bind(view)
        mainActivity.setSupportActionBar(binding.appBarLayout.toolbar)
        binding.appBarLayout.title = getString(R.string.youtube)

        feedAdapter = YoutubeFeedAdapter(
            onVideoClick = { video ->
                openVideo(video.videoId)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = feedAdapter
        binding.addChannelFab.setOnClickListener { showChannelBottomSheet() }
        binding.stateActionButton.setOnClickListener(::handleStateActionClick)

        viewModel.youtubeChannels.observe(viewLifecycleOwner) { channels ->
            val currentChannels = channels.orEmpty()
            val isWaitingForSeed = currentChannels.isEmpty() && !PreferenceUtil.youtubeDefaultsInitialized
            syncKnownChannels(currentChannels)

            if (isWaitingForSeed) {
                renderUiState(YoutubeFeedUiState(isInitialLoading = true))
                return@observe
            }

            viewModel.onChannelsChanged(currentChannels)
        }

        viewModel.uiState.observe(viewLifecycleOwner, ::renderUiState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.messages.collect { placeholder ->
                    val stateText = resolveStateText(placeholder)
                    val snackbar = Snackbar.make(
                        binding.root,
                        stateText.message,
                        Snackbar.LENGTH_LONG
                    )
                    if (placeholder.action == YoutubePlaceholderAction.RETRY) {
                        snackbar.setAction(R.string.youtube_retry_action) {
                            viewModel.refreshFeed(force = true)
                        }
                    }
                    snackbar.show()
                }
            }
        }
    }

    private fun renderUiState(state: YoutubeFeedUiState) {
        if (_binding == null) return

        feedAdapter.submitList(state.videos)

        val hasContent = state.videos.isNotEmpty()
        val placeholder = state.placeholder
        val stateText = placeholder?.let(::resolveStateText)
        val showState = !hasContent && (state.isInitialLoading || placeholder != null)

        binding.recyclerView.isVisible = hasContent
        binding.stateContainer.isVisible = showState
        binding.stateProgress.isVisible = state.isInitialLoading && !hasContent
        binding.refreshProgress.isVisible = state.isRefreshing

        binding.stateIcon.isVisible = placeholder != null
        binding.stateTitle.text = if (state.isInitialLoading && !hasContent) {
            getString(R.string.youtube_loading_title)
        } else {
            stateText?.title.orEmpty()
        }
        binding.stateMessage.text = if (state.isInitialLoading && !hasContent) {
            getString(R.string.youtube_loading_message)
        } else {
            stateText?.message.orEmpty()
        }
        binding.stateActionButton.isVisible =
            placeholder?.action != null && placeholder.action != YoutubePlaceholderAction.NONE
        binding.stateActionButton.text = when (placeholder?.action) {
            YoutubePlaceholderAction.ADD_CHANNEL -> getString(R.string.youtube_add_channel_action)
            YoutubePlaceholderAction.RETRY -> getString(R.string.youtube_retry_action)
            YoutubePlaceholderAction.NONE,
            null -> ""
        }

        binding.appBarLayout.toolbar.menu.findItem(MENU_REFRESH_FEED)?.isEnabled =
            !state.isInitialLoading && !state.isRefreshing
    }

    private fun resolveStateText(placeholder: YoutubePlaceholderState): StateText {
        return when (placeholder.kind) {
            YoutubePlaceholderKind.NO_CHANNELS -> StateText(
                title = getString(R.string.youtube_no_channels_title),
                message = getString(R.string.youtube_no_channels_message)
            )

            YoutubePlaceholderKind.EMPTY_FEED -> StateText(
                title = getString(R.string.youtube_feed_empty_title),
                message = getString(R.string.youtube_feed_empty_message)
            )

            YoutubePlaceholderKind.NETWORK_ERROR -> StateText(
                title = getString(R.string.youtube_feed_network_title),
                message = getString(R.string.youtube_feed_network_message)
            )

            YoutubePlaceholderKind.QUOTA_EXCEEDED -> StateText(
                title = getString(R.string.youtube_feed_quota_title),
                message = getString(R.string.youtube_feed_quota_message)
            )

            YoutubePlaceholderKind.CONFIGURATION_ERROR -> StateText(
                title = getString(R.string.youtube_feed_configuration_title),
                message = getString(R.string.youtube_feed_configuration_message)
            )

            YoutubePlaceholderKind.CHANNEL_RESOLUTION_ERROR -> StateText(
                title = getString(R.string.youtube_feed_resolution_title),
                message = getString(R.string.youtube_feed_resolution_message)
            )

            YoutubePlaceholderKind.SERVICE_ERROR -> StateText(
                title = getString(R.string.youtube_feed_service_title),
                message = getString(
                    R.string.youtube_feed_service_message,
                    placeholder.detail ?: "unknown"
                )
            )

            YoutubePlaceholderKind.UNKNOWN_ERROR -> StateText(
                title = getString(R.string.youtube_feed_unknown_title),
                message = getString(R.string.youtube_feed_unknown_message)
            )
        }
    }

    private fun handleStateActionClick(@Suppress("UNUSED_PARAMETER") view: View) {
        when (viewModel.uiState.value?.placeholder?.action) {
            YoutubePlaceholderAction.ADD_CHANNEL -> showChannelBottomSheet()
            YoutubePlaceholderAction.RETRY -> viewModel.refreshFeed(force = true)
            YoutubePlaceholderAction.NONE,
            null -> Unit
        }
    }

    private fun openVideo(videoId: String) {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
        try {
            startActivity(appIntent)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/watch?v=$videoId")
            )
            try {
                startActivity(webIntent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(
                    requireContext(),
                    "No app to open YouTube link",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun syncKnownChannels(channels: List<YoutubeChannelEntity>) {
        if (PreferenceUtil.youtubeDefaultsInitialized) return

        val missingChannels = KNOWN_CHANNELS.filter { known ->
            channels.none { it.name.equals(known.name, ignoreCase = true) || it.url == known.url }
        }
        if (missingChannels.isEmpty()) {
            PreferenceUtil.youtubeDefaultsInitialized = true
            return
        }

        if (seedStarted) return

        seedStarted = true
        missingChannels.forEach { known ->
            viewModel.insertYoutubeChannel(known)
        }
        PreferenceUtil.youtubeDefaultsInitialized = true
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_layout_type)
        menu.removeItem(R.id.action_sort_order)
        menu.removeItem(R.id.action_radio)
        menu.add(Menu.NONE, MENU_REFRESH_FEED, Menu.NONE, R.string.youtube_refresh_action)
            .setIcon(R.drawable.ic_refresh)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        ToolbarContentTintHelper.handleOnCreateOptionsMenu(
            requireContext(),
            binding.appBarLayout.toolbar,
            menu,
            ATHToolbarActivity.getToolbarBackgroundColor(binding.appBarLayout.toolbar)
        )
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                findNavController().navigate(R.id.settings_fragment, null, navOptions)
                true
            }

            MENU_REFRESH_FEED -> {
                viewModel.refreshFeed(force = true)
                true
            }

            else -> false
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(
            requireActivity(),
            binding.appBarLayout.toolbar
        )
    }

    private fun showChannelBottomSheet(channel: YoutubeChannelEntity? = null) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAddYoutubeChannelBinding.inflate(layoutInflater)
        bottomSheet.setContentView(dialogBinding.root)

        dialogBinding.nameEditText.setText(channel?.name)
        dialogBinding.urlEditText.setText(channel?.url)
        dialogBinding.imageUriEditText.setText(channel?.imageUri.takeIf(::isRemoteImage))

        var selectedImageUri = channel?.imageUri
        updateImagePreview(dialogBinding.imagePreview, selectedImageUri, R.drawable.ic_youtube)

        dialogBinding.uploadImageButton.setOnClickListener {
            launchArtworkPicker {
                selectedImageUri = it
                dialogBinding.imageUriEditText.setText("")
                updateImagePreview(dialogBinding.imagePreview, selectedImageUri, R.drawable.ic_youtube)
            }
        }
        dialogBinding.removeImageButton.setOnClickListener {
            selectedImageUri = null
            dialogBinding.imageUriEditText.setText("")
            updateImagePreview(dialogBinding.imagePreview, null, R.drawable.ic_youtube)
        }
        dialogBinding.imageUriEditText.doAfterTextChanged { editable ->
            val typedImage = editable?.toString()?.trim().orEmpty()
            if (typedImage.isNotBlank()) {
                selectedImageUri = typedImage
                updateImagePreview(dialogBinding.imagePreview, typedImage, R.drawable.ic_youtube)
            }
        }

        dialogBinding.saveButton.text = if (channel == null) "Add Channel" else "Save Changes"
        dialogBinding.saveButton.setOnClickListener {
            val name = dialogBinding.nameEditText.text?.toString()?.trim().orEmpty()
            val url = dialogBinding.urlEditText.text?.toString()?.trim().orEmpty()
            val imageSource = dialogBinding.imageUriEditText.text?.toString()?.trim()
                .takeUnless { it.isNullOrBlank() } ?: selectedImageUri

            if (name.isBlank() || url.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Name and channel URL are required",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (channel == null) {
                viewModel.insertYoutubeChannel(
                    YoutubeChannelEntity(name = name, url = url, imageUri = imageSource)
                )
            } else {
                cleanupReplacedImage(channel.imageUri, imageSource)
                viewModel.updateYoutubeChannel(
                    channel.copy(name = name, url = url, imageUri = imageSource)
                )
            }
            bottomSheet.dismiss()
        }
        bottomSheet.show()
    }

    private fun showEditOrDeleteDialog(channel: YoutubeChannelEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(channel.name)
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                if (which == 0) showChannelBottomSheet(channel) else showDeleteDialog(channel)
            }
            .show()
    }

    private fun showDeleteDialog(channel: YoutubeChannelEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Channel")
            .setMessage("Delete ${channel.name}?")
            .setPositiveButton("Delete") { _, _ ->
                RadioImageStore.deleteManagedImage(requireContext(), channel.imageUri)
                viewModel.deleteYoutubeChannel(channel)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openChannel(channel: YoutubeChannelEntity) {
        val channelUri = Uri.parse(channel.url)
        val appIntent = Intent(Intent.ACTION_VIEW, channelUri)
            .setPackage(YOUTUBE_PACKAGE)

        try {
            startActivity(appIntent)
            return
        } catch (_: ActivityNotFoundException) {
        }

        val browserIntent = Intent(Intent.ACTION_VIEW, channelUri)
        try {
            startActivity(browserIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                "Couldn't open this YouTube channel",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateImagePreview(imageView: ImageView, imageUri: String?, placeholderRes: Int) {
        Glide.with(imageView.context)
            .load(imageUri)
            .placeholder(placeholderRes)
            .error(placeholderRes)
            .into(imageView)
    }

    private fun launchArtworkPicker(onImageReady: (String) -> Unit) {
        pendingImageCallback = onImageReady
        ImagePicker.with(this)
            .galleryOnly()
            .crop()
            .compress(768)
            .maxResultSize(800, 800)
            .createIntent { artworkPickerLauncher.launch(it) }
    }

    private fun cleanupReplacedImage(previousImageUri: String?, nextImageUri: String?) {
        if (previousImageUri != nextImageUri) {
            RadioImageStore.deleteManagedImage(requireContext(), previousImageUri)
        }
    }

    private fun isRemoteImage(imageUri: String?): Boolean {
        return imageUri?.startsWith("http://", ignoreCase = true) == true ||
            imageUri?.startsWith("https://", ignoreCase = true) == true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val MENU_REFRESH_FEED = 2301

        private val KNOWN_CHANNELS = listOf(
            YoutubeChannelEntity(name = "GOD TV", url = "https://www.youtube.com/godtv"),
            YoutubeChannelEntity(
                name = "Daystar Television",
                url = "https://www.youtube.com/user/DaystarTV"
            ),
            YoutubeChannelEntity(name = "TBN", url = "https://www.youtube.com/tbn"),
            YoutubeChannelEntity(name = "Shalom World", url = "https://www.youtube.com/shalomworld"),
            YoutubeChannelEntity(
                name = "Jesus Film Project",
                url = "https://www.youtube.com/user/jesusfilm"
            ),
            YoutubeChannelEntity(
                name = "The Chosen",
                url = "https://www.youtube.com/channel/UCBXOFnNTULFaAnj24PAeblg"
            ),
            YoutubeChannelEntity(
                name = "Hope Channel",
                url = "https://www.youtube.com/results?search_query=Hope+Channel+official"
            )
        )
    }

    private data class StateText(
        val title: String,
        val message: String
    )
}
