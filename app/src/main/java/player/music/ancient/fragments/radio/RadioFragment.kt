package player.music.ancient.fragments.radio

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.koin.androidx.viewmodel.ext.android.viewModel
import player.music.ancient.R
import player.music.ancient.common.ATHToolbarActivity
import player.music.ancient.databinding.DialogAddRadioCategoryBinding
import player.music.ancient.databinding.DialogAddRadioStationBinding
import player.music.ancient.databinding.FragmentRadioBinding
import player.music.ancient.db.RadioCategoryEntity
import player.music.ancient.db.RadioStationEntity
import player.music.ancient.extensions.openUrl
import player.music.ancient.fragments.base.AbsMainActivityFragment
import player.music.ancient.helper.MusicPlayerRemote
import player.music.ancient.model.Song
import player.music.ancient.util.PreferenceUtil
import player.music.ancient.util.RadioImageStore
import player.music.ancient.util.RadioArtworkLoader
import player.music.ancient.util.ToolbarContentTintHelper
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.util.Locale

class RadioFragment : AbsMainActivityFragment(R.layout.fragment_radio) {

    private val viewModel: RadioViewModel by viewModel()
    private var _binding: FragmentRadioBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RadioAdapter

    private var radioCategories: List<RadioCategoryEntity> = emptyList()
    private var pendingArtworkTarget: PendingArtworkTarget? = null

    private var dX = 0f
    private var dY = 0f
    private var initialX = 0f
    private var initialY = 0f
    private var isDashboardExpanded = true

    private val dashboardTransition by lazy {
        TransitionSet()
            .addTransition(AutoTransition())
            .addTransition(Fade())
            .apply {
                duration = DASHBOARD_ANIMATION_DURATION_MS
                interpolator = FastOutSlowInInterpolator()
            }
    }

    private val artworkPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val target = pendingArtworkTarget ?: return@registerForActivityResult
            pendingArtworkTarget = null

            if (result.resultCode == Activity.RESULT_OK) {
                val pickedUri = result.data?.data
                if (pickedUri == null) {
                    Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }

                lifecycleScope.launch {
                    val savedImageUri = withContext(Dispatchers.IO) {
                        when (target.kind) {
                            ArtworkKind.STATION ->
                                RadioImageStore.persistStationImage(requireContext(), pickedUri)
                            ArtworkKind.CATEGORY ->
                                RadioImageStore.persistCategoryImage(requireContext(), pickedUri)
                        }
                    }

                    if (savedImageUri == null) {
                        Toast.makeText(
                            requireContext(),
                            "Couldn't save the selected image",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        target.onImageReady(savedImageUri)
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRadioBinding.bind(view)
        mainActivity.setSupportActionBar(binding.appBarLayout.toolbar)
        binding.appBarLayout.title = "Radio"
        binding.appBarLayout.toolbar.setNavigationOnClickListener {
            binding.appBarLayout.toolbar.menu.findItem(R.id.action_search)?.expandActionView()
        }

        adapter = RadioAdapter(
            onClick = ::playStation,
            onLongClick = ::showEditOrDeleteDialog
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        isDashboardExpanded =
            savedInstanceState?.getBoolean(KEY_DASHBOARD_EXPANDED) ?: true
        binding.radioHubDashboard.setOnClickListener { toggleDashboard() }
        binding.dashboardToggleButton.setOnClickListener { toggleDashboard() }
        applyDashboardExpandedState(animate = false)

        viewModel.radioCategories.observe(viewLifecycleOwner) { categories ->
            radioCategories = categories.orEmpty()
            adapter.submitCategories(radioCategories)
            preloadCategoryArtwork(radioCategories)
        }

        viewModel.radioStations.observe(viewLifecycleOwner) { stations ->
            syncKnownStations(stations.orEmpty())
            adapter.submitList(stations)
            updateEmptyState(stations.isNullOrEmpty())
            preloadStationArtwork(stations.orEmpty())
        }

        setupFab()
        updateLiveIndicator()
    }

    private fun toggleDashboard() {
        isDashboardExpanded = !isDashboardExpanded
        applyDashboardExpandedState(animate = true)
    }

    private fun applyDashboardExpandedState(animate: Boolean) {
        if (_binding == null) return

        if (animate) {
            TransitionManager.beginDelayedTransition(binding.radioHubDashboard, dashboardTransition)
        }

        binding.hubExpandedContent.isVisible = isDashboardExpanded
        binding.hubBackgroundImage.updateLayoutParams {
            height = if (isDashboardExpanded) {
                resources.getDimensionPixelSize(R.dimen.radio_hub_expanded_height)
            } else {
                resources.getDimensionPixelSize(R.dimen.radio_hub_collapsed_height)
            }
        }

        val targetRotation = if (isDashboardExpanded) 0f else 180f
        if (animate) {
            binding.dashboardToggleButton.animate()
                .rotation(targetRotation)
                .setDuration(DASHBOARD_ANIMATION_DURATION_MS)
                .start()
        } else {
            binding.dashboardToggleButton.rotation = targetRotation
        }
    }

    private fun setupFab() {
        binding.addStationFab.visibility = if (PreferenceUtil.showRadioFab) View.VISIBLE else View.GONE
        binding.addStationFab.setOnClickListener {
            showStationBottomSheet()
        }

        val savedX = PreferenceUtil.radioFabX
        val savedY = PreferenceUtil.radioFabY
        binding.addStationFab.post {
            if (savedX != -1f && savedY != -1f) {
                clampFabPosition(savedX, savedY, persist = false)
            } else {
                clampFabPosition(
                    binding.addStationFab.x,
                    binding.addStationFab.y,
                    persist = false
                )
            }
        }

        binding.addStationFab.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    initialX = event.rawX
                    initialY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    clampFabPosition(event.rawX + dX, event.rawY + dY)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val distance = Math.sqrt(
                        Math.pow((event.rawX - initialX).toDouble(), 2.0) +
                            Math.pow((event.rawY - initialY).toDouble(), 2.0)
                    )
                    if (distance < CLICK_DRAG_TOLERANCE) {
                        view.performClick()
                    } else {
                        clampFabPosition(view.x, view.y)
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun clampFabPosition(targetX: Float, targetY: Float, persist: Boolean = true) {
        val root = binding.root
        val fab = binding.addStationFab
        if (root.width == 0 || root.height == 0 || fab.width == 0 || fab.height == 0) return

        val minY = binding.appBarLayout.bottom.toFloat()
        val maxX = (root.width - fab.width).coerceAtLeast(0).toFloat()
        val maxY = (root.height - fab.height).coerceAtLeast(minY.toInt()).toFloat()
        val clampedX = targetX.coerceIn(0f, maxX)
        val clampedY = targetY.coerceIn(minY, maxY)

        fab.x = clampedX
        fab.y = clampedY

        if (persist) {
            PreferenceUtil.radioFabX = clampedX
            PreferenceUtil.radioFabY = clampedY
        }
    }

    private fun playStation(station: RadioStationEntity) {
        lifecycleScope.launch {
            when (val launchTarget = withContext(Dispatchers.IO) { resolveLaunchTarget(station) }) {
                null -> {
                    Toast.makeText(
                        requireContext(),
                        "Couldn't find a working stream for ${station.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is NativeRadioLaunchTarget -> {
                    updateStationIfNeeded(station, launchTarget.station)
                    openStation(launchTarget.station)
                }

                is ExternalRadioLaunchTarget -> {
                    launchTarget.station?.let { updateStationIfNeeded(station, it) }
                    MusicPlayerRemote.pauseSong()
                    adapter.setCurrentPlayingUri(null)
                    Toast.makeText(
                        requireContext(),
                        "Opening ${station.name} externally because its live stream is unavailable in-app right now",
                        Toast.LENGTH_LONG
                    ).show()
                    runCatching { openUrl(launchTarget.url) }
                        .onFailure {
                            Toast.makeText(
                                requireContext(),
                                "Couldn't open ${station.name} externally",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
        }
    }

    private fun updateStationIfNeeded(
        originalStation: RadioStationEntity,
        resolvedStation: RadioStationEntity
    ) {
        if (resolvedStation.uri != originalStation.uri ||
            resolvedStation.name != originalStation.name ||
            resolvedStation.imageUri != originalStation.imageUri
        ) {
            viewModel.updateRadioStation(
                originalStation.copy(
                    name = resolvedStation.name,
                    uri = resolvedStation.uri,
                    imageUri = resolvedStation.imageUri
                )
            )
            Toast.makeText(
                requireContext(),
                "Updated ${originalStation.name} to a working stream",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun resolveLaunchTarget(station: RadioStationEntity): RadioLaunchTarget? {
        val playableStation = resolveStationForPlayback(station)
            ?.copy(id = station.id, categoryId = station.categoryId)
        val externalFallbackUrl = externalFallbackUrlFor(playableStation ?: station)

        if (playableStation == null) {
            return externalFallbackUrl?.let { ExternalRadioLaunchTarget(it, station) }
        }

        if (externalFallbackUrl != null && !isStreamResponsive(playableStation.uri)) {
            return ExternalRadioLaunchTarget(externalFallbackUrl, playableStation)
        }

        return NativeRadioLaunchTarget(playableStation)
    }

    private fun externalFallbackUrlFor(station: RadioStationEntity): String? {
        return findKnownStation(station)?.externalUrl
    }

    private fun findKnownStation(station: RadioStationEntity): KnownRadioStation? {
        return KNOWN_STATIONS.firstOrNull { matchesKnownStation(station, it) }
    }

    private fun isStreamResponsive(url: String, depth: Int = 0): Boolean {
        if (depth > 2 || url.isBlank()) return false

        val connection = (URL(url).openConnection() as? HttpURLConnection) ?: return false
        return runCatching {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.setRequestProperty("User-Agent", "AncientMediaPlayer/1.0")
            connection.setRequestProperty("Icy-MetaData", "1")
            connection.setRequestProperty("Accept", "*/*")

            if (url.endsWith(".m3u8", ignoreCase = true)) {
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val playlistBody = connection.inputStream.bufferedReader().use { it.readText() }
                    val nextUrl = playlistBody.lineSequence()
                        .map { it.trim() }
                        .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                        ?.let { resolveUrl(url, it) }

                    nextUrl == null || isStreamResponsive(nextUrl, depth + 1)
                } else {
                    false
                }
            } else {
                connection.requestMethod = "GET"
                connection.setRequestProperty("Range", "bytes=0-1")
                val responseCode = connection.responseCode
                responseCode in 200..299 || responseCode == HttpURLConnection.HTTP_PARTIAL
            }
        }.getOrDefault(false)
            .also { connection.disconnect() }
    }

    private fun resolveUrl(baseUrl: String, targetUrl: String): String {
        return runCatching { URL(URL(baseUrl), targetUrl).toString() }
            .getOrDefault(targetUrl)
    }

    private fun syncKnownStations(stations: List<RadioStationEntity>) {
        for (knownStation in KNOWN_STATIONS) {
            val existingStation = stations.firstOrNull { matchesKnownStation(it, knownStation) }
            when {
                existingStation == null -> {
                    viewModel.insertRadioStation(
                        RadioStationEntity(
                            name = knownStation.name,
                            uri = knownStation.uri,
                            imageUri = knownStation.imageUri
                        )
                    )
                }

                needsStationUpdate(existingStation, knownStation) -> {
                    viewModel.updateRadioStation(
                        existingStation.copy(
                            name = knownStation.name,
                            uri = knownStation.uri,
                            imageUri = knownStation.imageUri
                        )
                    )
                }
            }
        }
    }

    private fun openStation(station: RadioStationEntity) {
        val song = Song(
            id = station.id,
            title = station.name,
            artistName = "Radio Stream",
            albumName = "Radio",
            duration = -1,
            trackNumber = 0,
            year = 0,
            data = station.uri,
            albumId = -1,
            artistId = -1,
            dateModified = System.currentTimeMillis(),
            composer = "",
            albumArtist = "",
            isRadio = true
        )
        MusicPlayerRemote.openQueue(arrayListOf(song), 0, true)
        adapter.setCurrentPlayingUri(station.uri)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        menu.removeItem(R.id.action_grid_size)
        menu.removeItem(R.id.action_layout_type)
        menu.removeItem(R.id.action_sort_order)
        menu.removeItem(R.id.action_radio)

        val searchItem = menu.add(Menu.NONE, R.id.action_search, Menu.NONE, "Search Online")
        searchItem.setIcon(R.drawable.ic_search)
        searchItem.setShowAsAction(
            MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
        )
        val searchView = SearchView(requireContext()).apply {
            queryHint = "Search online stations..."
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (!query.isNullOrBlank()) {
                        searchOnlineStations(query)
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean = false
            })
        }
        searchItem.actionView = searchView

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

    private fun searchOnlineStations(query: String) {
        lifecycleScope.launch {
            try {
                val stations = withContext(Dispatchers.IO) {
                    fetchStations(query)
                }
                if (stations.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "No working stations found",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showSearchResultsDialog(stations)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Search failed: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun fetchStations(
        query: String,
        exactMatch: Boolean = false,
        limit: Int = 20
    ): List<RadioStationEntity> {
        val encodedQuery = URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
        val queryKey = if (exactMatch) "nameExact" else "name"
        val url = URL(
            "https://de1.api.radio-browser.info/json/stations/search?" +
                "$queryKey=$encodedQuery&hidebroken=true&order=votes&reverse=true&limit=$limit"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("User-Agent", "AncientMusicPlayer/1.0")
            connectTimeout = 5000
            readTimeout = 5000
        }
        return connection.inputStream.bufferedReader().use { reader ->
            val jsonArray = JSONArray(reader.readText())
            buildList {
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val resolvedUrl = obj.optString("url_resolved").trim()
                    if (resolvedUrl.isBlank()) continue
                    add(
                        RadioStationEntity(
                            name = obj.getString("name"),
                            uri = resolvedUrl,
                            imageUri = obj.optString("favicon", "")
                        )
                    )
                }
            }.distinctBy { it.uri }
        }
    }

    private fun resolveStationForPlayback(station: RadioStationEntity): RadioStationEntity? {
        knownReplacementFor(station)?.let { return it }

        if (station.uri.isNotBlank()) {
            return station
        }

        fetchStations(station.name, exactMatch = true, limit = 10)
            .firstOrNull { it.uri != station.uri }
            ?.let { return it }

        for (query in buildFallbackQueries(station.name)) {
            fetchStations(query, limit = 10)
                .firstOrNull()
                ?.let { return it }
        }

        return null
    }

    private fun knownReplacementFor(station: RadioStationEntity): RadioStationEntity? {
        val knownStation = KNOWN_STATIONS.firstOrNull { matchesKnownStation(station, it) } ?: return null
        return RadioStationEntity(
            id = station.id,
            name = knownStation.name,
            uri = knownStation.uri,
            imageUri = knownStation.imageUri,
            categoryId = station.categoryId
        )
    }

    private fun buildFallbackQueries(stationName: String): List<String> {
        val ignoredWords = setOf("radio", "fm", "am", "live", "station")
        val words = stationName.split(Regex("[^A-Za-z0-9]+"))
            .map { it.trim() }
            .filter { it.length > 2 }
            .filterNot { it.lowercase() in ignoredWords }

        val fallbackQueries = mutableListOf<String>()
        if ("gospel" in words.map { it.lowercase() }) {
            fallbackQueries += "gospel"
        }
        if (words.size >= 2) {
            fallbackQueries += words.take(2).joinToString(" ")
        }
        fallbackQueries += words.sortedByDescending { it.length }
        return fallbackQueries.distinct()
    }

    private fun showSearchResultsDialog(results: List<RadioStationEntity>) {
        val names = results.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Search Results")
            .setItems(names) { _, which ->
                val selected = results[which]
                viewModel.insertRadioStation(selected)
                Toast.makeText(
                    requireContext(),
                    "Added ${selected.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
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
        ToolbarContentTintHelper.handleOnPrepareOptionsMenu(
            requireActivity(),
            binding.appBarLayout.toolbar
        )
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateLiveIndicator()
    }

    override fun onPlayStateChanged() {
        super.onPlayStateChanged()
        updateLiveIndicator()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateLiveIndicator()
    }

    private fun updateLiveIndicator() {
        if (_binding == null) return
        val currentSong = MusicPlayerRemote.currentSong
        val isRadioPlaying = MusicPlayerRemote.isPlaying && currentSong.isRadio

        if (isRadioPlaying) {
            adapter.setCurrentPlayingUri(currentSong.data)
            binding.radioHubDashboard.visibility = View.VISIBLE
            binding.hubStationName.text = currentSong.title
            binding.hubLiveIndicator.visibility = View.VISIBLE

            val artworkUri = viewModel.radioStations.value?.find { it.uri == currentSong.data }?.imageUri
            val heroWidth = if (PreferenceUtil.batterySaverMode) 512 else 1024
            val heroHeight = if (PreferenceUtil.batterySaverMode) 288 else 576
            RadioArtworkLoader.preload(requireContext(), artworkUri, heroWidth, heroHeight)
            updateImagePreview(binding.hubStationArt, artworkUri, R.drawable.ic_radio)
            updateImagePreview(binding.hubBackgroundImage, artworkUri, R.drawable.default_audio_art)
        } else {
            adapter.setCurrentPlayingUri(null)
            binding.hubLiveIndicator.visibility = View.GONE
            binding.hubStationName.text = getString(R.string.radio_offline)
            updateImagePreview(binding.hubStationArt, null, R.drawable.ic_radio)
            updateImagePreview(binding.hubBackgroundImage, null, R.drawable.default_audio_art)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (_binding == null) return
        binding.emptyState.root.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.radioHubDashboard.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun preloadCategoryArtwork(categories: List<RadioCategoryEntity>) {
        val preloadCount = if (PreferenceUtil.batterySaverMode) 2 else 4
        categories.take(preloadCount).forEach { category ->
            RadioArtworkLoader.preload(requireContext(), category.imageUri, 120, 120)
        }
    }

    private fun preloadStationArtwork(stations: List<RadioStationEntity>) {
        val preloadCount = if (PreferenceUtil.batterySaverMode) 3 else 8
        val preloadSize = if (PreferenceUtil.batterySaverMode) 256 else 512
        stations.take(preloadCount).forEach { station ->
            RadioArtworkLoader.preload(
                requireContext(),
                station.imageUri,
                preloadSize,
                preloadSize
            )
        }
    }

    private fun showStationBottomSheet(station: RadioStationEntity? = null) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAddRadioStationBinding.inflate(layoutInflater)
        bottomSheet.setContentView(dialogBinding.root)

        val categoryNames = buildCategoryOptions()
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            categoryNames
        )
        (dialogBinding.categoryEditText as MaterialAutoCompleteTextView).setAdapter(categoryAdapter)
        dialogBinding.categoryEditText.setOnClickListener {
            (it as? MaterialAutoCompleteTextView)?.showDropDown()
        }

        dialogBinding.nameEditText.setText(station?.name)
        dialogBinding.urlEditText.setText(station?.uri)
        dialogBinding.imageUriEditText.setText(station?.imageUri.takeIf(::isRemoteImage))
        dialogBinding.categoryEditText.setText(resolveCategoryLabel(station?.categoryId), false)

        var selectedImageUri = station?.imageUri
        updateImagePreview(dialogBinding.imagePreview, selectedImageUri, R.drawable.ic_radio)

        dialogBinding.uploadImageButton.setOnClickListener {
            launchArtworkPicker(ArtworkKind.STATION) { imageUri ->
                selectedImageUri = imageUri
                dialogBinding.imageUriEditText.setText("")
                updateImagePreview(dialogBinding.imagePreview, selectedImageUri, R.drawable.ic_radio)
            }
        }
        dialogBinding.removeImageButton.setOnClickListener {
            selectedImageUri = null
            dialogBinding.imageUriEditText.setText("")
            updateImagePreview(dialogBinding.imagePreview, null, R.drawable.ic_radio)
        }
        dialogBinding.imageUriEditText.doAfterTextChanged { editable ->
            val typedImage = editable?.toString()?.trim().orEmpty()
            if (typedImage.isNotBlank()) {
                selectedImageUri = typedImage
                updateImagePreview(dialogBinding.imagePreview, typedImage, R.drawable.ic_radio)
            }
        }

        dialogBinding.saveButton.text = if (station == null) "Add Station" else "Save Changes"
        dialogBinding.saveButton.setOnClickListener {
            val name = dialogBinding.nameEditText.text?.toString()?.trim().orEmpty()
            val url = dialogBinding.urlEditText.text?.toString()?.trim().orEmpty()
            val imageSource = dialogBinding.imageUriEditText.text?.toString()?.trim()
                .takeUnless { it.isNullOrBlank() } ?: selectedImageUri
            val selectedCategory = dialogBinding.categoryEditText.text?.toString().orEmpty()
            val categoryId = resolveCategoryId(selectedCategory)

            if (name.isBlank() || url.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Name and URL are required",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (selectedCategory.isNotBlank() &&
                selectedCategory != NO_CATEGORY_LABEL &&
                categoryId == null
            ) {
                Toast.makeText(
                    requireContext(),
                    "Choose an existing category or use Manage Categories first",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (station == null) {
                viewModel.insertRadioStation(
                    RadioStationEntity(
                        name = name,
                        uri = url,
                        imageUri = imageSource,
                        categoryId = categoryId
                    )
                )
            } else {
                cleanupReplacedImage(station.imageUri, imageSource)
                viewModel.updateRadioStation(
                    station.copy(
                        name = name,
                        uri = url,
                        imageUri = imageSource,
                        categoryId = categoryId
                    )
                )
            }
            bottomSheet.dismiss()
        }
        bottomSheet.show()
        expandBottomSheet(bottomSheet)
    }

    private fun showManageCategoriesDialog() {
        val categoryNames = radioCategories.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Manage Categories")
            .setItems(categoryNames) { _, which ->
                showEditOrDeleteCategoryDialog(radioCategories[which])
            }
            .setPositiveButton("Add Category") { _, _ ->
                showCategoryBottomSheet()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showCategoryBottomSheet(category: RadioCategoryEntity? = null) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val dialogBinding = DialogAddRadioCategoryBinding.inflate(layoutInflater)
        bottomSheet.setContentView(dialogBinding.root)

        dialogBinding.nameEditText.setText(category?.name)
        dialogBinding.imageUriEditText.setText(category?.imageUri.takeIf(::isRemoteImage))

        var selectedImageUri = category?.imageUri
        updateImagePreview(dialogBinding.imagePreview, selectedImageUri, R.drawable.ic_folder)

        dialogBinding.uploadImageButton.setOnClickListener {
            launchArtworkPicker(ArtworkKind.CATEGORY) { imageUri ->
                selectedImageUri = imageUri
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
                Toast.makeText(
                    requireContext(),
                    "Category name is required",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (category == null) {
                viewModel.insertRadioCategory(
                    RadioCategoryEntity(
                        name = name,
                        imageUri = imageSource
                    )
                )
            } else {
                cleanupReplacedImage(category.imageUri, imageSource)
                viewModel.updateRadioCategory(
                    category.copy(
                        name = name,
                        imageUri = imageSource
                    )
                )
            }
            bottomSheet.dismiss()
        }
        bottomSheet.show()
        expandBottomSheet(bottomSheet)
    }

    private fun expandBottomSheet(bottomSheet: BottomSheetDialog) {
        val sheet =
            bottomSheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return
        sheet.layoutParams = sheet.layoutParams?.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        BottomSheetBehavior.from(sheet).apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun showEditOrDeleteDialog(station: RadioStationEntity) {
        val options = arrayOf("Edit", "Delete")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(station.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showStationBottomSheet(station)
                    1 -> showDeleteDialog(station)
                }
            }
            .show()
    }

    private fun showEditOrDeleteCategoryDialog(category: RadioCategoryEntity) {
        val options = arrayOf("Edit", "Delete")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(category.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCategoryBottomSheet(category)
                    1 -> showDeleteCategoryDialog(category)
                }
            }
            .show()
    }

    private fun showDeleteDialog(station: RadioStationEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Station")
            .setMessage("Are you sure you want to delete ${station.name}?")
            .setPositiveButton("Delete") { _, _ ->
                RadioImageStore.deleteManagedImage(requireContext(), station.imageUri)
                viewModel.deleteRadioStation(station)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteCategoryDialog(category: RadioCategoryEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Delete ${category.name}? Stations in this category will become uncategorized.")
            .setPositiveButton("Delete") { _, _ ->
                RadioImageStore.deleteManagedImage(requireContext(), category.imageUri)
                viewModel.deleteRadioCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateImagePreview(imageView: ImageView, imageUri: String?, placeholderRes: Int) {
        RadioArtworkLoader.load(imageView, imageUri, placeholderRes)
    }

    private fun launchArtworkPicker(kind: ArtworkKind, onImageReady: (String) -> Unit) {
        pendingArtworkTarget = PendingArtworkTarget(kind, onImageReady)
        ImagePicker.with(this)
            .galleryOnly()
            .crop()
            .compress(768)
            .maxResultSize(800, 800)
            .createIntent { intent ->
                artworkPickerLauncher.launch(intent)
            }
    }

    private fun resolveCategoryId(categoryName: String): Long? {
        val normalizedName = categoryName.trim()
        if (normalizedName.isBlank() || normalizedName == NO_CATEGORY_LABEL) {
            return null
        }
        return radioCategories.firstOrNull {
            it.name.equals(normalizedName, ignoreCase = true)
        }?.id
    }

    private fun resolveCategoryLabel(categoryId: Long?): String {
        return radioCategories.firstOrNull { it.id == categoryId }?.name ?: NO_CATEGORY_LABEL
    }

    private fun buildCategoryOptions(): List<String> {
        return listOf(NO_CATEGORY_LABEL) + radioCategories.map { it.name }
    }

    private fun cleanupReplacedImage(previousImageUri: String?, nextImageUri: String?) {
        if (previousImageUri == nextImageUri) return
        RadioImageStore.deleteManagedImage(requireContext(), previousImageUri)
    }

    private fun isRemoteImage(imageUri: String?): Boolean {
        return imageUri?.startsWith("http://", ignoreCase = true) == true ||
            imageUri?.startsWith("https://", ignoreCase = true) == true
    }

    private fun matchesKnownStation(
        station: RadioStationEntity,
        knownStation: KnownRadioStation
    ): Boolean {
        val normalizedName = normalizeStationText(station.name)
        return normalizedName in knownStation.normalizedAliases ||
            station.uri.equals(knownStation.uri, ignoreCase = true)
    }

    private fun needsStationUpdate(
        station: RadioStationEntity,
        knownStation: KnownRadioStation
    ): Boolean {
        return station.name != knownStation.name ||
            station.uri != knownStation.uri ||
            station.imageUri.orEmpty() != knownStation.imageUri
    }

    private fun normalizeStationText(value: String): String {
        return value.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_DASHBOARD_EXPANDED, isDashboardExpanded)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class KnownRadioStation(
        val name: String,
        val aliases: Set<String>,
        val uri: String,
        val imageUri: String = "",
        val externalUrl: String? = null
    ) {
        val normalizedAliases: Set<String>
            get() = aliases.mapTo(linkedSetOf()) {
                it.lowercase(Locale.US)
                    .replace(Regex("[^a-z0-9]+"), " ")
                    .trim()
            }
    }

    private data class PendingArtworkTarget(
        val kind: ArtworkKind,
        val onImageReady: (String) -> Unit
    )

    private sealed interface RadioLaunchTarget

    private data class NativeRadioLaunchTarget(
        val station: RadioStationEntity
    ) : RadioLaunchTarget

    private data class ExternalRadioLaunchTarget(
        val url: String,
        val station: RadioStationEntity? = null
    ) : RadioLaunchTarget

    private enum class ArtworkKind {
        STATION,
        CATEGORY
    }

    companion object {
        private const val KEY_DASHBOARD_EXPANDED = "key_dashboard_expanded"
        private const val DASHBOARD_ANIMATION_DURATION_MS = 260L
        private const val CLICK_DRAG_TOLERANCE = 10f
        private const val MENU_MANAGE_CATEGORIES = 1001
        private const val NO_CATEGORY_LABEL = "No Category"

        private val KNOWN_STATIONS = listOf(
            KnownRadioStation(
                name = "Nigeria Info 99.3 Lagos",
                aliases = setOf(
                    "Nigeria Info 99.3 Lagos",
                    "Nigeria Info Lagos",
                    "Nigeria Info 99.3 FM",
                    "Nigeria Info"
                ),
                uri = "https://nigeriainfofmlagos993-atunwadigital.streamguys1.com/nigeriainfofmlagos993/playlist.m3u8",
                imageUri = "https://www.nigeriainfo.fm/favicon.ico",
                externalUrl = "https://tunein.com/radio/Nigeria-Info-Fm-Lagos-993-s341184/"
            ),
            KnownRadioStation(
                name = "Nigerian Gospel Music Radio",
                aliases = setOf(
                    "Nigerian Gospel Music Radio",
                    "Nigerian Gospel Music Radio (MP3)",
                    "Afrofusion Gospel Radio"
                ),
                uri = "https://stream.zeno.fm/3fmqr74a7f8uv",
                imageUri = "https://nigeriangospelmusic.com.ng/wp-content/uploads/2022/09/NGM-icon.png"
            ),
            KnownRadioStation(
                name = "Live365 Radio",
                aliases = setOf("Live365 Radio"),
                uri = "https://streaming.live365.com/a48337"
            )
        )
    }
}
