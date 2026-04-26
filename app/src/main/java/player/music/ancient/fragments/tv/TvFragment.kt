package player.music.ancient.fragments.tv

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.app.ActivityOptionsCompat
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import player.music.ancient.R
import player.music.ancient.activities.VideoPlayerActivity
import player.music.ancient.common.ATHToolbarActivity
import player.music.ancient.databinding.DialogAddTvCategoryBinding
import player.music.ancient.databinding.DialogAddTvChannelBinding
import player.music.ancient.databinding.FragmentTvBinding
import player.music.ancient.db.TvCategoryEntity
import player.music.ancient.db.TvChannelEntity
import player.music.ancient.fragments.base.AbsMainActivityFragment
import player.music.ancient.util.PreferenceUtil
import player.music.ancient.util.RadioImageStore
import player.music.ancient.util.ToolbarContentTintHelper
import com.bumptech.glide.Glide

class TvFragment : AbsMainActivityFragment(R.layout.fragment_tv) {
    private val viewModel: TvViewModel by viewModel()
    private var _binding: FragmentTvBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TvAdapter
    private var tvCategories: List<TvCategoryEntity> = emptyList()
    private var tvChannels: List<TvChannelEntity> = emptyList()
    private var tvCategoriesLoaded = false
    private var tvChannelsLoaded = false
    private var seedStage = 0
    private var pendingArtworkTarget: PendingArtworkTarget? = null

    private val artworkPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val target = pendingArtworkTarget ?: return@registerForActivityResult
            pendingArtworkTarget = null

            if (result.resultCode == Activity.RESULT_OK) {
                val pickedUri = result.data?.data ?: return@registerForActivityResult
                lifecycleScope.launch {
                    val savedImageUri = when (target.kind) {
                        ArtworkKind.CHANNEL ->
                            RadioImageStore.persistStationImage(requireContext(), pickedUri)
                        ArtworkKind.CATEGORY ->
                            RadioImageStore.persistCategoryImage(requireContext(), pickedUri)
                    }
                    if (savedImageUri == null) {
                        Toast.makeText(requireContext(), "Couldn't save the selected image", Toast.LENGTH_SHORT).show()
                    } else {
                        target.onImageReady(savedImageUri)
                    }
                }
            } else if (result.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(requireContext(), ImagePicker.getError(result.data), Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTvBinding.bind(view)
        mainActivity.setSupportActionBar(binding.appBarLayout.toolbar)
        binding.appBarLayout.title = getString(R.string.tv)

        binding.emptyState.iconView.setImageResource(R.drawable.ic_tv)
        binding.emptyState.titleView.text = "No TV Channels"
        binding.emptyState.messageView.text = "Add Christian movie and music channels to watch them here."

        adapter = TvAdapter(
            onClick = ::openChannel,
            onLongClick = ::showEditOrDeleteDialog
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.addChannelFab.setOnClickListener { showChannelBottomSheet() }

        viewModel.tvCategories.observe(viewLifecycleOwner) { categories ->
            tvCategoriesLoaded = true
            tvCategories = categories.orEmpty()
            adapter.submitCategories(tvCategories)
            maybeSeedKnownContent()
        }
        viewModel.tvChannels.observe(viewLifecycleOwner) { channels ->
            tvChannelsLoaded = true
            tvChannels = channels.orEmpty()
            maybeSeedKnownContent()
            adapter.submitList(tvChannels)
            updateEmptyState(tvChannels.isEmpty())
        }
    }

    private fun maybeSeedKnownContent() {
        if (!tvCategoriesLoaded || !tvChannelsLoaded || PreferenceUtil.tvDefaultsVersion >= CURRENT_TV_DEFAULTS_VERSION) {
            return
        }

        val legacyChannelsToDelete = tvChannels.filter { channel ->
            channel.name in LEGACY_CHANNEL_NAMES &&
                KNOWN_CHANNELS.none { it.name.equals(channel.name, ignoreCase = true) }
        }
        if (legacyChannelsToDelete.isNotEmpty()) {
            if (seedStage < 1) {
                seedStage = 1
                legacyChannelsToDelete.forEach(viewModel::deleteTvChannel)
            }
            return
        }

        val missingCategories = KNOWN_CATEGORIES.filter { name ->
            tvCategories.none { it.name.equals(name, ignoreCase = true) }
        }
        if (missingCategories.isNotEmpty()) {
            if (seedStage < 2) {
                seedStage = 2
                missingCategories.forEach { name ->
                    viewModel.insertTvCategory(TvCategoryEntity(name = name))
                }
            }
            return
        }

        val missingChannels = KNOWN_CHANNELS.filter { known ->
            val categoryId = tvCategories.firstOrNull {
                it.name.equals(known.categoryName, ignoreCase = true)
            }?.id
            categoryId != null && tvChannels.none {
                it.name.equals(known.name, ignoreCase = true) || it.url == known.url
            }
        }
        if (missingChannels.isNotEmpty()) {
            if (seedStage < 3) {
                seedStage = 3
                missingChannels.forEach { known ->
                    val categoryId = tvCategories.firstOrNull {
                        it.name.equals(known.categoryName, ignoreCase = true)
                    }?.id ?: return@forEach
                    viewModel.insertTvChannel(
                        TvChannelEntity(
                            name = known.name,
                            url = known.url,
                            imageUri = known.imageUri,
                            categoryId = categoryId
                        )
                    )
                }
            }
            return
        }

        val removableLegacyCategories = tvCategories.filter { category ->
            category.name in LEGACY_CATEGORY_NAMES &&
                category.name !in KNOWN_CATEGORIES &&
                tvChannels.none { it.categoryId == category.id }
        }
        if (removableLegacyCategories.isNotEmpty()) {
            if (seedStage < 4) {
                seedStage = 4
                removableLegacyCategories.forEach(viewModel::deleteTvCategory)
            }
            return
        }

        seedStage = 5
        PreferenceUtil.tvDefaultsVersion = CURRENT_TV_DEFAULTS_VERSION
        PreferenceUtil.tvDefaultsInitialized = true
    }

    private fun openChannel(channel: TvChannelEntity, sharedView: View) {
        lifecycleScope.launch {
            when (val launchTarget = withContext(Dispatchers.IO) { TvStreamResolver.resolve(channel.url) }) {
                is NativeTvLaunchTarget -> {
                    val transitionName = VideoPlayerActivity.transitionNameFor(channel.id)
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        requireActivity(),
                        sharedView,
                        transitionName
                    )
                    startActivity(
                        VideoPlayerActivity.intent(
                            context = requireContext(),
                            title = channel.name,
                            uri = launchTarget.streamUrl,
                            referer = launchTarget.referer,
                            origin = launchTarget.origin,
                            userAgent = launchTarget.userAgent,
                            transitionName = transitionName
                        ),
                        options.toBundle()
                    )
                }

                is ExternalTvLaunchTarget -> {
                    Toast.makeText(requireContext(), R.string.tv_opening_external, Toast.LENGTH_SHORT).show()
                    openExternalChannel(launchTarget.url)
                }
            }
        }
    }

    private fun openExternalChannel(url: String) {
        val externalIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(externalIntent)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), R.string.tv_unable_to_open_channel, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_layout_type)
        menu.removeItem(R.id.action_sort_order)
        menu.removeItem(R.id.action_radio)
        menu.add(Menu.NONE, MENU_MANAGE_CATEGORIES, Menu.NONE, "Manage Categories")
            .setIcon(R.drawable.ic_folder)
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
            MENU_MANAGE_CATEGORIES -> {
                showManageCategoriesDialog()
                true
            }

            R.id.action_settings -> {
                findNavController().navigate(R.id.settings_fragment, null, navOptions)
                true
            }

            else -> false
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(requireActivity(), binding.appBarLayout.toolbar)
    }

    private fun showChannelBottomSheet(channel: TvChannelEntity? = null) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAddTvChannelBinding.inflate(layoutInflater)
        bottomSheet.setContentView(dialogBinding.root)

        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, buildCategoryOptions())
        (dialogBinding.categoryEditText as MaterialAutoCompleteTextView).setAdapter(categoryAdapter)
        dialogBinding.categoryEditText.setOnClickListener {
            (it as? MaterialAutoCompleteTextView)?.showDropDown()
        }

        dialogBinding.nameEditText.setText(channel?.name)
        dialogBinding.urlEditText.setText(channel?.url)
        dialogBinding.imageUriEditText.setText(channel?.imageUri.takeIf(::isRemoteImage))
        dialogBinding.categoryEditText.setText(resolveCategoryLabel(channel?.categoryId), false)

        var selectedImageUri = channel?.imageUri
        updateImagePreview(dialogBinding.imagePreview, selectedImageUri, R.drawable.ic_tv)

        dialogBinding.uploadImageButton.setOnClickListener {
            launchArtworkPicker(ArtworkKind.CHANNEL) {
                selectedImageUri = it
                dialogBinding.imageUriEditText.setText("")
                updateImagePreview(dialogBinding.imagePreview, selectedImageUri, R.drawable.ic_tv)
            }
        }
        dialogBinding.removeImageButton.setOnClickListener {
            selectedImageUri = null
            dialogBinding.imageUriEditText.setText("")
            updateImagePreview(dialogBinding.imagePreview, null, R.drawable.ic_tv)
        }
        dialogBinding.imageUriEditText.doAfterTextChanged { editable ->
            val typedImage = editable?.toString()?.trim().orEmpty()
            if (typedImage.isNotBlank()) {
                selectedImageUri = typedImage
                updateImagePreview(dialogBinding.imagePreview, typedImage, R.drawable.ic_tv)
            }
        }

        dialogBinding.saveButton.text = if (channel == null) "Add Channel" else "Save Changes"
        dialogBinding.saveButton.setOnClickListener {
            val name = dialogBinding.nameEditText.text?.toString()?.trim().orEmpty()
            val url = dialogBinding.urlEditText.text?.toString()?.trim().orEmpty()
            val imageSource = dialogBinding.imageUriEditText.text?.toString()?.trim()
                .takeUnless { it.isNullOrBlank() } ?: selectedImageUri
            val categoryId = resolveCategoryId(dialogBinding.categoryEditText.text?.toString().orEmpty())

            if (name.isBlank() || url.isBlank()) {
                Toast.makeText(requireContext(), "Name and URL are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (channel == null) {
                viewModel.insertTvChannel(TvChannelEntity(name = name, url = url, imageUri = imageSource, categoryId = categoryId))
            } else {
                cleanupReplacedImage(channel.imageUri, imageSource)
                viewModel.updateTvChannel(channel.copy(name = name, url = url, imageUri = imageSource, categoryId = categoryId))
            }
            bottomSheet.dismiss()
        }
        bottomSheet.show()
        expandBottomSheet(bottomSheet)
    }

    private fun showManageCategoriesDialog() {
        val categoryNames = tvCategories.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Manage Categories")
            .setItems(categoryNames) { _, which ->
                showEditOrDeleteCategoryDialog(tvCategories[which])
            }
            .setPositiveButton("Add Category") { _, _ -> showCategoryBottomSheet() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showCategoryBottomSheet(category: TvCategoryEntity? = null) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAddTvCategoryBinding.inflate(layoutInflater)
        bottomSheet.setContentView(dialogBinding.root)

        dialogBinding.nameEditText.setText(category?.name)
        dialogBinding.imageUriEditText.setText(category?.imageUri.takeIf(::isRemoteImage))
        var selectedImageUri = category?.imageUri
        updateImagePreview(dialogBinding.imagePreview, selectedImageUri, R.drawable.ic_folder)

        dialogBinding.uploadImageButton.setOnClickListener {
            launchArtworkPicker(ArtworkKind.CATEGORY) {
                selectedImageUri = it
                dialogBinding.imageUriEditText.setText("")
                updateImagePreview(dialogBinding.imagePreview, selectedImageUri, R.drawable.ic_folder)
            }
        }
        dialogBinding.removeImageButton.setOnClickListener {
            selectedImageUri = null
            dialogBinding.imageUriEditText.setText("")
            updateImagePreview(dialogBinding.imagePreview, null, R.drawable.ic_folder)
        }
        dialogBinding.imageUriEditText.doAfterTextChanged { editable ->
            val typedImage = editable?.toString()?.trim().orEmpty()
            if (typedImage.isNotBlank()) {
                selectedImageUri = typedImage
                updateImagePreview(dialogBinding.imagePreview, typedImage, R.drawable.ic_folder)
            }
        }

        dialogBinding.saveButton.text = if (category == null) "Add Category" else "Save Changes"
        dialogBinding.saveButton.setOnClickListener {
            val name = dialogBinding.nameEditText.text?.toString()?.trim().orEmpty()
            val imageSource = dialogBinding.imageUriEditText.text?.toString()?.trim()
                .takeUnless { it.isNullOrBlank() } ?: selectedImageUri

            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Category name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (category == null) {
                viewModel.insertTvCategory(TvCategoryEntity(name = name, imageUri = imageSource))
            } else {
                cleanupReplacedImage(category.imageUri, imageSource)
                viewModel.updateTvCategory(category.copy(name = name, imageUri = imageSource))
            }
            bottomSheet.dismiss()
        }
        bottomSheet.show()
        expandBottomSheet(bottomSheet)
    }

    private fun showEditOrDeleteDialog(channel: TvChannelEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(channel.name)
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                if (which == 0) showChannelBottomSheet(channel) else showDeleteDialog(channel)
            }
            .show()
    }

    private fun showEditOrDeleteCategoryDialog(category: TvCategoryEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(category.name)
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                if (which == 0) showCategoryBottomSheet(category) else showDeleteCategoryDialog(category)
            }
            .show()
    }

    private fun showDeleteDialog(channel: TvChannelEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Channel")
            .setMessage("Delete ${channel.name}?")
            .setPositiveButton("Delete") { _, _ ->
                RadioImageStore.deleteManagedImage(requireContext(), channel.imageUri)
                viewModel.deleteTvChannel(channel)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteCategoryDialog(category: TvCategoryEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Delete ${category.name}? Channels in this category will become uncategorized.")
            .setPositiveButton("Delete") { _, _ ->
                RadioImageStore.deleteManagedImage(requireContext(), category.imageUri)
                viewModel.deleteTvCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateImagePreview(imageView: ImageView, imageUri: String?, placeholderRes: Int) {
        Glide.with(imageView.context)
            .load(imageUri)
            .placeholder(placeholderRes)
            .error(placeholderRes)
            .into(imageView)
    }

    private fun launchArtworkPicker(kind: ArtworkKind, onImageReady: (String) -> Unit) {
        pendingArtworkTarget = PendingArtworkTarget(kind, onImageReady)
        ImagePicker.with(this)
            .galleryOnly()
            .crop()
            .compress(768)
            .maxResultSize(800, 800)
            .createIntent { artworkPickerLauncher.launch(it) }
    }

    private fun resolveCategoryId(categoryName: String): Long? {
        val normalizedName = categoryName.trim()
        if (normalizedName.isBlank() || normalizedName == NO_CATEGORY_LABEL) return null
        return tvCategories.firstOrNull { it.name.equals(normalizedName, ignoreCase = true) }?.id
    }

    private fun resolveCategoryLabel(categoryId: Long?): String {
        return tvCategories.firstOrNull { it.id == categoryId }?.name ?: NO_CATEGORY_LABEL
    }

    private fun buildCategoryOptions(): List<String> = listOf(NO_CATEGORY_LABEL) + tvCategories.map { it.name }

    private fun cleanupReplacedImage(previousImageUri: String?, nextImageUri: String?) {
        if (previousImageUri != nextImageUri) {
            RadioImageStore.deleteManagedImage(requireContext(), previousImageUri)
        }
    }

    private fun isRemoteImage(imageUri: String?): Boolean {
        return imageUri?.startsWith("http://", ignoreCase = true) == true ||
            imageUri?.startsWith("https://", ignoreCase = true) == true
    }

    private fun expandBottomSheet(bottomSheet: BottomSheetDialog) {
        val sheet = bottomSheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        sheet.layoutParams = sheet.layoutParams?.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        BottomSheetBehavior.from(sheet).apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.root.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class PendingArtworkTarget(
        val kind: ArtworkKind,
        val onImageReady: (String) -> Unit
    )

    private data class KnownTvChannel(
        val name: String,
        val categoryName: String,
        val url: String,
        val imageUri: String = ""
    )

    private enum class ArtworkKind {
        CHANNEL,
        CATEGORY
    }

    companion object {
        private const val CURRENT_TV_DEFAULTS_VERSION = 2
        private const val MENU_MANAGE_CATEGORIES = 2101
        private const val NO_CATEGORY_LABEL = "No Category"

        private val KNOWN_CATEGORIES = listOf(
            "Christian Movies",
            "Christian Music"
        )

        private val LEGACY_CATEGORY_NAMES = setOf(
            "Live TV",
            "Movies & Family",
            "International"
        )

        private val LEGACY_CHANNEL_NAMES = setOf(
            "Daystar Live",
            "Daystar Canada",
            "Daystar En Vivo",
            "Reflections 24/7",
            "GOD TV US",
            "GOD TV UK",
            "GOD TV Africa",
            "GOD TV Asia",
            "Hope Channel Live",
            "Shalom World Live",
            "Christian Channel",
            "KingdomFlix",
            "PraiseFlix",
            "FaithChannel",
            "WOTG Movies",
            "Jesus Film Project"
        )

        private val KNOWN_CHANNELS = listOf(
            KnownTvChannel(
                name = "ABN Bible Movies",
                categoryName = "Christian Movies",
                url = "https://mediaserver.abnvideos.com/streams/abnbiblemovies.m3u8"
            ),
            KnownTvChannel(
                name = "Gospel Movie TV",
                categoryName = "Christian Movies",
                url = "https://stmv1.srvif.com/gospelf/gospelf/playlist.m3u8"
            ),
            KnownTvChannel(
                name = "3ABN Praise Him Music Network",
                categoryName = "Christian Music",
                url = "https://3abn.bozztv.com/3abn1/PraiseHim/smil:PraiseHim.smil/playlist.m3u8"
            ),
            KnownTvChannel(
                name = "Spirit TV",
                categoryName = "Christian Music",
                url = "https://cdnlive.myspirit.tv/LS-ATL-43240-2/index.m3u8"
            ),
            KnownTvChannel(
                name = "My Gospel TV",
                categoryName = "Christian Music",
                url = "https://streamtv.cmediahosthosting.net:3046/live/mygospeltvlive.m3u8"
            ),
            KnownTvChannel(
                name = "Trace Gospel",
                categoryName = "Christian Music",
                url = "https://channels.trace.plus/Traceprod/GOSPEL_FR/.m3u8"
            )
        )
    }
}
