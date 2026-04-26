package player.music.ancient.fragments.more

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import player.music.ancient.R
import player.music.ancient.common.ATHToolbarActivity
import player.music.ancient.databinding.FragmentMoreBinding
import player.music.ancient.fragments.base.AbsMainActivityFragment
import player.music.ancient.model.CategoryInfo
import player.music.ancient.util.PreferenceUtil
import player.music.ancient.util.ToolbarContentTintHelper
import player.music.ancient.activities.base.AbsSlidingMusicPanelActivity.Companion.MAX_BOTTOM_NAV_ITEMS

class MoreFragment : AbsMainActivityFragment(R.layout.fragment_more) {
    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMoreBinding.bind(view)
        mainActivity.setSupportActionBar(binding.appBarLayout.toolbar)
        binding.appBarLayout.title = getString(R.string.more)

        val overflowEntries = PreferenceUtil.libraryCategory
            .filter { it.visible }
            .drop(MAX_BOTTOM_NAV_ITEMS - 1)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = MoreAdapter(overflowEntries) { category ->
            navigateToCategory(category)
        }
    }

    private fun navigateToCategory(category: CategoryInfo) {
        findNavController().navigate(category.category.id, null, navOptions)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_layout_type)
        menu.removeItem(R.id.action_sort_order)
        menu.removeItem(R.id.action_radio)
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
}
