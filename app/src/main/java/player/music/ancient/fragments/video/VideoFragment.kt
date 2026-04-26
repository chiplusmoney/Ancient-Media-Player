package player.music.ancient.fragments.video

import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import player.music.ancient.R
import player.music.ancient.activities.VideoPlayerActivity
import player.music.ancient.common.ATHToolbarActivity
import player.music.ancient.databinding.FragmentVideoBinding
import player.music.ancient.fragments.base.AbsMainActivityFragment
import player.music.ancient.model.LocalVideoItem
import player.music.ancient.util.ToolbarContentTintHelper

class VideoFragment : AbsMainActivityFragment(R.layout.fragment_video) {
    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: VideoAdapter
    private var allVideos: List<LocalVideoItem> = emptyList()
    private var selectedFolder: String? = null

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVideoBinding.bind(view)
        mainActivity.setSupportActionBar(binding.appBarLayout.toolbar)
        binding.appBarLayout.title = getString(R.string.videos)

        binding.emptyState.iconView.setImageResource(R.drawable.ic_video)
        binding.emptyState.titleView.text = "No Videos Found"
        binding.emptyState.messageView.text = "Device videos you can play will appear here."

        adapter = VideoAdapter { item, sharedView ->
            val transitionName = VideoPlayerActivity.transitionNameFor(item.id)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(),
                sharedView,
                transitionName
            )
            startActivity(
                VideoPlayerActivity.intent(
                    context = requireContext(),
                    title = item.title,
                    uri = item.uri,
                    transitionName = transitionName
                ),
                options.toBundle()
            )
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        loadVideos()
    }

    private fun loadVideos() {
        lifecycleScope.launch {
            val videos = withContext(Dispatchers.IO) {
                val items = mutableListOf<LocalVideoItem>()
                val projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.TITLE,
                    MediaStore.Video.Media.DURATION,
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Video.Media.DATE_ADDED
                )
                requireContext().contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${MediaStore.Video.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val folderColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn).orEmpty()
                        val duration = cursor.getLong(durationColumn)
                        val folder = cursor.getString(folderColumn).orEmpty().ifBlank { "Unknown Folder" }
                        val dateAdded = cursor.getLong(dateAddedColumn)
                        val uri = "${MediaStore.Video.Media.EXTERNAL_CONTENT_URI}/$id"
                        items += LocalVideoItem(id, title, uri, folder, duration, dateAdded)
                    }
                }
                items
            }
            allVideos = videos
            updateList()
        }
    }

    private fun updateList() {
        val filteredVideos = selectedFolder?.let { folder ->
            allVideos.filter { it.folderName == folder }
        } ?: allVideos
        adapter.submitList(filteredVideos)
        binding.emptyState.root.visibility = if (filteredVideos.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (filteredVideos.isEmpty()) View.GONE else View.VISIBLE
        binding.appBarLayout.toolbar.subtitle = selectedFolder
    }

    private fun showFolderPicker() {
        val folders = allVideos.map { it.folderName }.distinct().sorted()
        val options = listOf("All Videos") + folders
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Folder")
            .setItems(options.toTypedArray()) { _, which ->
                selectedFolder = options[which].takeUnless { it == "All Videos" }
                updateList()
            }
            .show()
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_layout_type)
        menu.removeItem(R.id.action_sort_order)
        menu.removeItem(R.id.action_radio)
        menu.add(Menu.NONE, MENU_CHOOSE_FOLDER, Menu.NONE, "Choose Folder")
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
            MENU_CHOOSE_FOLDER -> {
                showFolderPicker()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MENU_CHOOSE_FOLDER = 2201
    }
}
