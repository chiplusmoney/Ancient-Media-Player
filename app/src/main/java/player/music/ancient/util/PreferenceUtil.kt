package player.music.ancient.util

import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.content.res.use
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import player.music.ancient.SHOW_RADIO_FAB
import player.music.ancient.RADIO_FAB_X
import player.music.ancient.RADIO_FAB_Y
import player.music.ancient.MINI_PLAYER_X
import player.music.ancient.MINI_PLAYER_Y
import player.music.ancient.util.VersionUtils
import player.music.ancient.ADAPTIVE_COLOR_APP
import player.music.ancient.ALBUM_ARTISTS_ONLY
import player.music.ancient.ALBUM_ART_ON_LOCK_SCREEN
import player.music.ancient.ALBUM_COVER_STYLE
import player.music.ancient.ALBUM_COVER_TRANSFORM
import player.music.ancient.ALBUM_DETAIL_SONG_SORT_ORDER
import player.music.ancient.ALBUM_GRID_SIZE
import player.music.ancient.ALBUM_GRID_SIZE_LAND
import player.music.ancient.ALBUM_GRID_STYLE
import player.music.ancient.ALBUM_SONG_SORT_ORDER
import player.music.ancient.ALBUM_SORT_ORDER
import player.music.ancient.APPBAR_MODE
import player.music.ancient.ARTIST_ALBUM_SORT_ORDER
import player.music.ancient.ARTIST_DETAIL_SONG_SORT_ORDER
import player.music.ancient.ARTIST_GRID_SIZE
import player.music.ancient.ARTIST_GRID_SIZE_LAND
import player.music.ancient.ARTIST_GRID_STYLE
import player.music.ancient.ARTIST_SONG_SORT_ORDER
import player.music.ancient.ARTIST_SORT_ORDER
import player.music.ancient.AUDIO_FADE_DURATION
import player.music.ancient.AUTO_DOWNLOAD_IMAGES_POLICY
import player.music.ancient.App
import player.music.ancient.BATTERY_SAVER_MODE
import player.music.ancient.BASS_BOOST_STRENGTH
import player.music.ancient.BLACK_THEME
import player.music.ancient.BLUETOOTH_PLAYBACK
import player.music.ancient.BLURRED_ALBUM_ART
import player.music.ancient.CAROUSEL_EFFECT
import player.music.ancient.CIRCLE_PLAY_BUTTON
import player.music.ancient.COLORED_APP_SHORTCUTS
import player.music.ancient.CROSS_FADE_DURATION
import player.music.ancient.CUSTOM_FONT
import player.music.ancient.DESATURATED_COLOR
import player.music.ancient.ENABLE_SEARCH_PLAYLIST
import player.music.ancient.EQUALIZER_BANDS
import player.music.ancient.EQUALIZER_ENABLED
import player.music.ancient.EQUALIZER_PRESET
import player.music.ancient.EXPAND_NOW_PLAYING_PANEL
import player.music.ancient.EXTRA_SONG_INFO
import player.music.ancient.FILTER_SONG
import player.music.ancient.GENERAL_THEME
import player.music.ancient.GENRE_SORT_ORDER
import player.music.ancient.HOME_ALBUM_GRID_STYLE
import player.music.ancient.HOME_ARTIST_GRID_STYLE
import player.music.ancient.HOME_SHORTCUTS
import player.music.ancient.IGNORE_MEDIA_STORE_ARTWORK
import player.music.ancient.INITIALIZED_BLACKLIST
import player.music.ancient.KEEP_SCREEN_ON
import player.music.ancient.LANGUAGE_NAME
import player.music.ancient.LAST_ADDED_CUTOFF
import player.music.ancient.LAST_CHANGELOG_VERSION
import player.music.ancient.LAST_DIRECTORY
import player.music.ancient.LAST_SLEEP_TIMER_VALUE
import player.music.ancient.LAST_USED_TAB
import player.music.ancient.LIBRARY_CATEGORIES
import player.music.ancient.LOCALE_AUTO_STORE_ENABLED
import player.music.ancient.LOCK_SCREEN
import player.music.ancient.LYRICS_OPTIONS
import player.music.ancient.LYRICS_TYPE
import player.music.ancient.MANAGE_AUDIO_FOCUS
import player.music.ancient.MATERIAL_YOU
import player.music.ancient.NEW_BLUR_AMOUNT
import player.music.ancient.NEXT_SLEEP_TIMER_ELAPSED_REALTIME
import player.music.ancient.NOW_PLAYING_SCREEN_ID
import player.music.ancient.PAUSE_HISTORY
import player.music.ancient.PAUSE_ON_ZERO_VOLUME
import player.music.ancient.PLAYBACK_PITCH
import player.music.ancient.PLAYBACK_SPEED
import player.music.ancient.PLAYLIST_GRID_SIZE
import player.music.ancient.PLAYLIST_GRID_SIZE_LAND
import player.music.ancient.PLAYLIST_SORT_ORDER
import player.music.ancient.R
import player.music.ancient.RECENTLY_PLAYED_CUTOFF
import player.music.ancient.REMEMBER_LAST_TAB
import player.music.ancient.SAF_SDCARD_URI
import player.music.ancient.SAVE_LAST_DIRECTORY
import player.music.ancient.SCREEN_ON_LYRICS
import player.music.ancient.SHOW_LYRICS
import player.music.ancient.SHOW_WHEN_LOCKED
import player.music.ancient.SLEEP_TIMER_FINISH_SONG
import player.music.ancient.SNOWFALL
import player.music.ancient.SONG_GRID_SIZE
import player.music.ancient.SONG_GRID_SIZE_LAND
import player.music.ancient.SONG_GRID_STYLE
import player.music.ancient.SONG_SORT_ORDER
import player.music.ancient.START_DIRECTORY
import player.music.ancient.SWIPE_ANYWHERE_NOW_PLAYING
import player.music.ancient.SWIPE_DOWN_DISMISS
import player.music.ancient.TAB_TEXT_MODE
import player.music.ancient.TOGGLE_ADD_CONTROLS
import player.music.ancient.TOGGLE_FULL_SCREEN
import player.music.ancient.TOGGLE_HEADSET
import player.music.ancient.TOGGLE_HOME_BANNER
import player.music.ancient.TOGGLE_SUGGESTIONS
import player.music.ancient.TOGGLE_VOLUME
import player.music.ancient.TV_DEFAULTS_INITIALIZED
import player.music.ancient.TV_DEFAULTS_VERSION
import player.music.ancient.USER_NAME
import player.music.ancient.VIRTUALIZER_STRENGTH
import player.music.ancient.WALLPAPER_ACCENT
import player.music.ancient.WHITELIST_MUSIC
import player.music.ancient.YOUTUBE_DEFAULTS_INITIALIZED
import player.music.ancient.YOUTUBE_FEED_LAST_ATTEMPT_AT
import player.music.ancient.YOUTUBE_FEED_LAST_FAILURE_KIND
import player.music.ancient.YOUTUBE_FEED_LAST_SYNC_AT
import player.music.ancient.YOUTUBE_FEED_SYNC_SIGNATURE
import player.music.ancient.extensions.getIntRes
import player.music.ancient.extensions.getStringOrDefault
import player.music.ancient.fragments.AlbumCoverStyle
import player.music.ancient.fragments.GridStyle
import player.music.ancient.fragments.NowPlayingScreen
import player.music.ancient.fragments.folder.FoldersFragment
import player.music.ancient.helper.SortOrder.AlbumSongSortOrder
import player.music.ancient.helper.SortOrder.AlbumSortOrder
import player.music.ancient.helper.SortOrder.ArtistAlbumSortOrder
import player.music.ancient.helper.SortOrder.ArtistSongSortOrder
import player.music.ancient.helper.SortOrder.ArtistSortOrder
import player.music.ancient.helper.SortOrder.GenreSortOrder
import player.music.ancient.helper.SortOrder.PlaylistSortOrder
import player.music.ancient.helper.SortOrder.SongSortOrder
import player.music.ancient.model.CategoryInfo
import player.music.ancient.transform.CascadingPageTransformer
import player.music.ancient.transform.DepthTransformation
import player.music.ancient.transform.HingeTransformation
import player.music.ancient.transform.HorizontalFlipTransformation
import player.music.ancient.transform.NormalPageTransformer
import player.music.ancient.transform.VerticalFlipTransformation
import player.music.ancient.transform.VerticalStackTransformer
import player.music.ancient.util.theme.ThemeMode
import player.music.ancient.views.TopAppBarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File


object PreferenceUtil {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(App.getContext())

    val defaultCategories = listOf(
        CategoryInfo(CategoryInfo.Category.Home, true),
        CategoryInfo(CategoryInfo.Category.Songs, true),
        CategoryInfo(CategoryInfo.Category.Albums, true),
        CategoryInfo(CategoryInfo.Category.Artists, true),
        CategoryInfo(CategoryInfo.Category.Playlists, false),
        CategoryInfo(CategoryInfo.Category.Radio, true),
        CategoryInfo(CategoryInfo.Category.Tv, true),
        CategoryInfo(CategoryInfo.Category.Videos, true),
        CategoryInfo(CategoryInfo.Category.Youtube, true),
        CategoryInfo(CategoryInfo.Category.Genres, false),
        CategoryInfo(CategoryInfo.Category.Folder, false),
        CategoryInfo(CategoryInfo.Category.Search, false)
    )

    var libraryCategory: List<CategoryInfo>
        get() {
            val gson = Gson()
            val collectionType = object : TypeToken<List<CategoryInfo>>() {}.type

            val data = sharedPreferences.getStringOrDefault(
                LIBRARY_CATEGORIES,
                gson.toJson(defaultCategories, collectionType)
            )
            return try {
                normalizeLibraryCategories(Gson().fromJson(data, collectionType))
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
                return defaultCategories
            }
        }
        set(value) {
            val collectionType = object : TypeToken<List<CategoryInfo?>?>() {}.type
            sharedPreferences.edit {
                putString(LIBRARY_CATEGORIES, Gson().toJson(value, collectionType))
            }
        }

    private fun normalizeLibraryCategories(categories: List<CategoryInfo>?): List<CategoryInfo> {
        val currentCategories = categories.orEmpty()
        val normalized = currentCategories
            .filter { saved -> defaultCategories.any { it.category == saved.category } }
            .map { CategoryInfo(it.category, it.visible) }
            .toMutableList()

        defaultCategories.forEach { default ->
            if (normalized.none { it.category == default.category }) {
                normalized += CategoryInfo(default.category, default.visible)
            }
        }

        return normalized
    }

    fun registerOnSharedPreferenceChangedListener(
        listener: OnSharedPreferenceChangeListener,
    ) = sharedPreferences.registerOnSharedPreferenceChangeListener(listener)


    fun unregisterOnSharedPreferenceChangedListener(
        changeListener: OnSharedPreferenceChangeListener,
    ) = sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener)


    val baseTheme get() = sharedPreferences.getStringOrDefault(GENERAL_THEME, "auto")

    fun getGeneralThemeValue(isSystemDark: Boolean): ThemeMode {
        val themeMode: String =
            sharedPreferences.getStringOrDefault(GENERAL_THEME, "auto")
        return if (isBlackMode && isSystemDark && themeMode != "light") {
            ThemeMode.BLACK
        } else {
            if (isBlackMode && themeMode == "dark") {
                ThemeMode.BLACK
            } else {
                when (themeMode) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    "auto" -> ThemeMode.AUTO
                    else -> ThemeMode.AUTO
                }
            }
        }
    }

    var languageCode: String
        get() = sharedPreferences.getString(LANGUAGE_NAME, "auto") ?: "auto"
        set(value) = sharedPreferences.edit {
            putString(LANGUAGE_NAME, value)
        }

    var isLocaleAutoStorageEnabled: Boolean
        get() = sharedPreferences.getBoolean(
            LOCALE_AUTO_STORE_ENABLED,
            false
        )
        set(value) = sharedPreferences.edit {
            putBoolean(LOCALE_AUTO_STORE_ENABLED, value)
        }

    var Fragment.userName
        get() = sharedPreferences.getString(
            USER_NAME,
            getString(R.string.user_name)
        )
        set(value) = sharedPreferences.edit {
            putString(USER_NAME, value)
        }

    var safSdCardUri
        get() = sharedPreferences.getStringOrDefault(SAF_SDCARD_URI, "")
        set(value) = sharedPreferences.edit {
            putString(SAF_SDCARD_URI, value)
        }

    private val autoDownloadImagesPolicy
        get() = sharedPreferences.getStringOrDefault(
            AUTO_DOWNLOAD_IMAGES_POLICY,
            "only_wifi"
        )

    var albumArtistsOnly
        get() = sharedPreferences.getBoolean(
            ALBUM_ARTISTS_ONLY,
            false
        )
        set(value) = sharedPreferences.edit { putBoolean(ALBUM_ARTISTS_ONLY, value) }

    var albumDetailSongSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ALBUM_DETAIL_SONG_SORT_ORDER,
            AlbumSongSortOrder.SONG_TRACK_LIST
        )
        set(value) = sharedPreferences.edit { putString(ALBUM_DETAIL_SONG_SORT_ORDER, value) }

    var artistDetailSongSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ARTIST_DETAIL_SONG_SORT_ORDER,
            ArtistSongSortOrder.SONG_A_Z
        )
        set(value) = sharedPreferences.edit { putString(ARTIST_DETAIL_SONG_SORT_ORDER, value) }

    var songSortOrder
        get() = sharedPreferences.getStringOrDefault(
            SONG_SORT_ORDER,
            SongSortOrder.SONG_A_Z
        )
        set(value) = sharedPreferences.edit {
            putString(SONG_SORT_ORDER, value)
        }

    var albumSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ALBUM_SORT_ORDER,
            AlbumSortOrder.ALBUM_A_Z
        )
        set(value) = sharedPreferences.edit {
            putString(ALBUM_SORT_ORDER, value)
        }


    var artistSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ARTIST_SORT_ORDER,
            ArtistSortOrder.ARTIST_A_Z
        )
        set(value) = sharedPreferences.edit {
            putString(ARTIST_SORT_ORDER, value)
        }

    val albumSongSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ALBUM_SONG_SORT_ORDER,
            AlbumSongSortOrder.SONG_TRACK_LIST
        )

    val artistSongSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ARTIST_SONG_SORT_ORDER,
            AlbumSongSortOrder.SONG_TRACK_LIST
        )

    var artistAlbumSortOrder
        get() = sharedPreferences.getStringOrDefault(
            ARTIST_ALBUM_SORT_ORDER,
            ArtistAlbumSortOrder.ALBUM_YEAR
        )
        set(value) = sharedPreferences.edit {
            putString(ARTIST_ALBUM_SORT_ORDER, value)
        }

    var playlistSortOrder
        get() = sharedPreferences.getStringOrDefault(
            PLAYLIST_SORT_ORDER,
            PlaylistSortOrder.PLAYLIST_A_Z
        )
        set(value) = sharedPreferences.edit {
            putString(PLAYLIST_SORT_ORDER, value)
        }

    val genreSortOrder
        get() = sharedPreferences.getStringOrDefault(
            GENRE_SORT_ORDER,
            GenreSortOrder.GENRE_A_Z
        )

    val isIgnoreMediaStoreArtwork
        get() = sharedPreferences.getBoolean(
            IGNORE_MEDIA_STORE_ARTWORK,
            false
        )

    val isVolumeVisibilityMode
        get() = sharedPreferences.getBoolean(
            TOGGLE_VOLUME, false
        )

    var isInitializedBlacklist
        get() = sharedPreferences.getBoolean(
            INITIALIZED_BLACKLIST, false
        )
        set(value) = sharedPreferences.edit {
            putBoolean(INITIALIZED_BLACKLIST, value)
        }

    private val isBlackMode
        get() = sharedPreferences.getBoolean(
            BLACK_THEME, false
        )

    private val isSystemBatterySaverEnabled: Boolean
        get() = App.getContext().getSystemService<PowerManager>()?.isPowerSaveMode == true

    val batterySaverMode: Boolean
        get() = sharedPreferences.getBoolean(BATTERY_SAVER_MODE, true) || isSystemBatterySaverEnabled

    val isExtraControls
        get() = sharedPreferences.getBoolean(
            TOGGLE_ADD_CONTROLS, false
        )

    val isHomeBanner
        get() = sharedPreferences.getBoolean(
            TOGGLE_HOME_BANNER, false
        )

    val isScreenOnEnabled get() = !batterySaverMode && sharedPreferences.getBoolean(KEEP_SCREEN_ON, false)

    val isShowWhenLockedEnabled get() = !batterySaverMode && sharedPreferences.getBoolean(SHOW_WHEN_LOCKED, false)

    val isSongInfo get() = sharedPreferences.getBoolean(EXTRA_SONG_INFO, false)

    val isPauseOnZeroVolume get() = sharedPreferences.getBoolean(PAUSE_ON_ZERO_VOLUME, false)

    var isSleepTimerFinishMusic
        get() = sharedPreferences.getBoolean(
            SLEEP_TIMER_FINISH_SONG, false
        )
        set(value) = sharedPreferences.edit {
            putBoolean(SLEEP_TIMER_FINISH_SONG, value)
        }

    val isExpandPanel get() = sharedPreferences.getBoolean(EXPAND_NOW_PLAYING_PANEL, false)

    val isHeadsetPlugged
        get() = sharedPreferences.getBoolean(
            TOGGLE_HEADSET, false
        )

    val isAlbumArtOnLockScreen
        get() = sharedPreferences.getBoolean(
            ALBUM_ART_ON_LOCK_SCREEN, true
        )

    val isBluetoothSpeaker
        get() = sharedPreferences.getBoolean(
            BLUETOOTH_PLAYBACK, false
        )

    val isSpeakerDisabled get() = sharedPreferences.getBoolean("disable_speaker", false)

    val isBlurredAlbumArt
        get() = sharedPreferences.getBoolean(
            BLURRED_ALBUM_ART, false
        ) && !VersionUtils.hasR() && !batterySaverMode

    val blurAmount get() = sharedPreferences.getInt(NEW_BLUR_AMOUNT, 25)

    val isCarouselEffect
        get() = sharedPreferences.getBoolean(
            CAROUSEL_EFFECT, false
        ) && !batterySaverMode

    var isColoredAppShortcuts
        get() = sharedPreferences.getBoolean(
            COLORED_APP_SHORTCUTS, true
        )
        set(value) = sharedPreferences.edit {
            putBoolean(COLORED_APP_SHORTCUTS, value)
        }

    var isDesaturatedColor
        get() = sharedPreferences.getBoolean(
            DESATURATED_COLOR, false
        )
        set(value) = sharedPreferences.edit {
            putBoolean(DESATURATED_COLOR, value)
        }

    val isAdaptiveColor
        get() = sharedPreferences.getBoolean(
            ADAPTIVE_COLOR_APP, false
        )

    val isFullScreenMode
        get() = sharedPreferences.getBoolean(
            TOGGLE_FULL_SCREEN, false
        )

    val isAudioFocusEnabled
        get() = sharedPreferences.getBoolean(
            MANAGE_AUDIO_FOCUS, false
        )

    val isLockScreen get() = sharedPreferences.getBoolean(LOCK_SCREEN, false)

    @Suppress("deprecation")
    fun isAllowedToDownloadMetadata(context: Context): Boolean {
        if (batterySaverMode) {
            return false
        }
        return when (autoDownloadImagesPolicy) {
            "always" -> true
            "only_wifi" -> {
                val connectivityManager = context.getSystemService<ConnectivityManager>()
                val network = connectivityManager?.activeNetwork
                val capabilities = connectivityManager?.getNetworkCapabilities(network)
                capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            }

            "never" -> false
            else -> false
        }
    }


    var lyricsOption
        get() = sharedPreferences.getInt(LYRICS_OPTIONS, 1)
        set(value) = sharedPreferences.edit {
            putInt(LYRICS_OPTIONS, value)
        }

    var songGridStyle: GridStyle
        get() {
            val id: Int = sharedPreferences.getInt(SONG_GRID_STYLE, 0)
            // We can directly use "first" kotlin extension function here but
            // there maybe layout id stored in this so to avoid a crash we use
            // "firstOrNull"
            return GridStyle.values().firstOrNull { gridStyle ->
                gridStyle.id == id
            } ?: GridStyle.Grid
        }
        set(value) = sharedPreferences.edit {
            putInt(SONG_GRID_STYLE, value.id)
        }

    var albumGridStyle: GridStyle
        get() {
            val id: Int = sharedPreferences.getInt(ALBUM_GRID_STYLE, 0)
            return GridStyle.values().firstOrNull { gridStyle ->
                gridStyle.id == id
            } ?: GridStyle.Grid
        }
        set(value) = sharedPreferences.edit {
            putInt(ALBUM_GRID_STYLE, value.id)
        }

    var artistGridStyle: GridStyle
        get() {
            val id: Int = sharedPreferences.getInt(ARTIST_GRID_STYLE, 3)
            return GridStyle.values().firstOrNull { gridStyle ->
                gridStyle.id == id
            } ?: GridStyle.Circular
        }
        set(value) = sharedPreferences.edit {
            putInt(ARTIST_GRID_STYLE, value.id)
        }

    val filterLength get() = sharedPreferences.getInt(FILTER_SONG, 20)

    var lastVersion
        // This was stored as an integer before now it's a long, so avoid a ClassCastException
        get() = try {
            sharedPreferences.getLong(LAST_CHANGELOG_VERSION, 0)
        } catch (e: ClassCastException) {
            sharedPreferences.edit { remove(LAST_CHANGELOG_VERSION) }
            0
        }
        set(value) = sharedPreferences.edit {
            putLong(LAST_CHANGELOG_VERSION, value)
        }

    var lastSleepTimerValue
        get() = sharedPreferences.getInt(
            LAST_SLEEP_TIMER_VALUE,
            30
        )
        set(value) = sharedPreferences.edit {
            putInt(LAST_SLEEP_TIMER_VALUE, value)
        }


    var nextSleepTimerElapsedRealTime
        get() = sharedPreferences.getInt(
            NEXT_SLEEP_TIMER_ELAPSED_REALTIME,
            -1
        )
        set(value) = sharedPreferences.edit {
            putInt(NEXT_SLEEP_TIMER_ELAPSED_REALTIME, value)
        }

    fun themeResFromPrefValue(themePrefValue: String): Int {
        return when (themePrefValue) {
            "light" -> R.style.Theme_AncientMusic_Light
            "dark" -> R.style.Theme_AncientMusic
            else -> R.style.Theme_AncientMusic
        }
    }

    val homeArtistGridStyle: Int
        get() {
            val position = sharedPreferences.getStringOrDefault(
                HOME_ARTIST_GRID_STYLE, "0"
            ).toInt()
            val layoutRes =
                App.getContext().resources.obtainTypedArray(R.array.pref_home_grid_style_layout)
                    .use {
                        it.getResourceId(position, 0)
                    }
            return if (layoutRes == 0) {
                R.layout.item_artist
            } else layoutRes
        }

    val homeAlbumGridStyle: Int
        get() {
            val position = sharedPreferences.getStringOrDefault(
                HOME_ALBUM_GRID_STYLE, "4"
            ).toInt()
            val layoutRes = App.getContext()
                .resources.obtainTypedArray(R.array.pref_home_grid_style_layout).use {
                    it.getResourceId(position, 0)
                }
            return if (layoutRes == 0) {
                R.layout.item_image
            } else layoutRes
        }

    val tabTitleMode: Int
        get() {
            return when (sharedPreferences.getStringOrDefault(
                TAB_TEXT_MODE, "0"
            ).toInt()) {
                0 -> BottomNavigationView.LABEL_VISIBILITY_AUTO
                1 -> BottomNavigationView.LABEL_VISIBILITY_LABELED
                2 -> BottomNavigationView.LABEL_VISIBILITY_SELECTED
                3 -> BottomNavigationView.LABEL_VISIBILITY_UNLABELED
                else -> BottomNavigationView.LABEL_VISIBILITY_LABELED
            }
        }


    var songGridSize
        get() = sharedPreferences.getInt(
            SONG_GRID_SIZE,
            App.getContext().getIntRes(R.integer.default_list_columns)
        )
        set(value) = sharedPreferences.edit {
            putInt(SONG_GRID_SIZE, value)
        }

    var songGridSizeLand
        get() = sharedPreferences.getInt(
            SONG_GRID_SIZE_LAND,
            App.getContext().getIntRes(R.integer.default_grid_columns_land)
        )
        set(value) = sharedPreferences.edit {
            putInt(SONG_GRID_SIZE_LAND, value)
        }


    var albumGridSize: Int
        get() = sharedPreferences.getInt(
            ALBUM_GRID_SIZE,
            App.getContext().getIntRes(R.integer.default_grid_columns)
        )
        set(value) = sharedPreferences.edit {
            putInt(ALBUM_GRID_SIZE, value)
        }


    var albumGridSizeLand
        get() = sharedPreferences.getInt(
            ALBUM_GRID_SIZE_LAND,
            App.getContext().getIntRes(R.integer.default_grid_columns_land)
        )
        set(value) = sharedPreferences.edit {
            putInt(ALBUM_GRID_SIZE_LAND, value)
        }


    var artistGridSize
        get() = sharedPreferences.getInt(
            ARTIST_GRID_SIZE,
            App.getContext().getIntRes(R.integer.default_grid_columns)
        )
        set(value) = sharedPreferences.edit {
            putInt(ARTIST_GRID_SIZE, value)
        }


    var artistGridSizeLand
        get() = sharedPreferences.getInt(
            ARTIST_GRID_SIZE_LAND,
            App.getContext().getIntRes(R.integer.default_grid_columns_land)
        )
        set(value) = sharedPreferences.edit {
            putInt(ALBUM_GRID_SIZE_LAND, value)
        }


    var playlistGridSize
        get() = sharedPreferences.getInt(
            PLAYLIST_GRID_SIZE,
            App.getContext().getIntRes(R.integer.default_grid_columns)
        )
        set(value) = sharedPreferences.edit {
            putInt(PLAYLIST_GRID_SIZE, value)
        }


    var playlistGridSizeLand
        get() = sharedPreferences.getInt(
            PLAYLIST_GRID_SIZE_LAND,
            App.getContext().getIntRes(R.integer.default_grid_columns_land)
        )
        set(value) = sharedPreferences.edit {
            putInt(PLAYLIST_GRID_SIZE, value)
        }

    var albumCoverStyle: AlbumCoverStyle
        get() {
            val id: Int = sharedPreferences.getInt(ALBUM_COVER_STYLE, 0)
            for (albumCoverStyle in AlbumCoverStyle.values()) {
                if (albumCoverStyle.id == id) {
                    return albumCoverStyle
                }
            }
            return AlbumCoverStyle.Card
        }
        set(value) = sharedPreferences.edit { putInt(ALBUM_COVER_STYLE, value.id) }


    var nowPlayingScreen: NowPlayingScreen
        get() {
            val id: Int = sharedPreferences.getInt(NOW_PLAYING_SCREEN_ID, 0)
            for (nowPlayingScreen in NowPlayingScreen.values()) {
                if (nowPlayingScreen.id == id) {
                    return nowPlayingScreen
                }
            }
            return NowPlayingScreen.Adaptive
        }
        set(value) = sharedPreferences.edit {
            putInt(NOW_PLAYING_SCREEN_ID, value.id)
            // Also set a cover theme for that now playing
            value.defaultCoverTheme?.let { coverTheme -> albumCoverStyle = coverTheme }
        }

    val albumCoverTransform: ViewPager.PageTransformer
        get() {
            val style = sharedPreferences.getStringOrDefault(
                ALBUM_COVER_TRANSFORM,
                "0"
            ).toInt()
            return when (style) {
                0 -> NormalPageTransformer()
                1 -> CascadingPageTransformer()
                2 -> DepthTransformation()
                3 -> HorizontalFlipTransformation()
                4 -> VerticalFlipTransformation()
                5 -> HingeTransformation()
                6 -> VerticalStackTransformer()
                else -> ViewPager.PageTransformer { _, _ -> }
            }
        }

    var startDirectory: File
        get() {
            val folderPath = FoldersFragment.defaultStartDirectory.path
            val filePath: String = sharedPreferences.getStringOrDefault(START_DIRECTORY, folderPath)
            return File(filePath)
        }
        set(value) = sharedPreferences.edit {
            putString(
                START_DIRECTORY,
                FileUtil.safeGetCanonicalPath(value)
            )
        }

    var lastDirectory: File
        get() {
            val folderPath = FoldersFragment.defaultStartDirectory.path
            val filePath: String = sharedPreferences.getStringOrDefault(LAST_DIRECTORY, folderPath)
            return File(filePath)
        }
        set(value) = sharedPreferences.edit {
            putString(
                LAST_DIRECTORY,
                FileUtil.safeGetCanonicalPath(value)
            )
        }

    var saveLastDirectory: Boolean
        get() = sharedPreferences.getBoolean(SAVE_LAST_DIRECTORY, false)
        set(value) = sharedPreferences.edit { putBoolean(SAVE_LAST_DIRECTORY, value) }

    fun getRecentlyPlayedCutoffTimeMillis(): Long {
        val calendarUtil = CalendarUtil()
        val interval: Long = when (sharedPreferences.getString(RECENTLY_PLAYED_CUTOFF, "")) {
            "today" -> calendarUtil.elapsedToday
            "this_week" -> calendarUtil.elapsedWeek
            "past_seven_days" -> calendarUtil.getElapsedDays(7)
            "past_three_months" -> calendarUtil.getElapsedMonths(3)
            "this_year" -> calendarUtil.elapsedYear
            "this_month" -> calendarUtil.elapsedMonth
            else -> calendarUtil.elapsedMonth
        }
        return System.currentTimeMillis() - interval
    }

    val lastAddedCutoff: Long
        get() {
            val calendarUtil = CalendarUtil()
            val interval =
                when (sharedPreferences.getStringOrDefault(LAST_ADDED_CUTOFF, "this_month")) {
                    "today" -> calendarUtil.elapsedToday
                    "this_week" -> calendarUtil.elapsedWeek
                    "past_three_months" -> calendarUtil.getElapsedMonths(3)
                    "this_year" -> calendarUtil.elapsedYear
                    "this_month" -> calendarUtil.elapsedMonth
                    else -> calendarUtil.elapsedMonth
                }
            return (System.currentTimeMillis() - interval) / 1000
        }

    val homeSuggestions: Boolean
        get() = sharedPreferences.getBoolean(
            TOGGLE_SUGGESTIONS,
            true
        )

    val pauseHistory: Boolean
        get() = sharedPreferences.getBoolean(
            PAUSE_HISTORY,
            false
        )

    var audioFadeDuration
        get() = sharedPreferences
            .getInt(AUDIO_FADE_DURATION, 0)
        set(value) = sharedPreferences.edit { putInt(AUDIO_FADE_DURATION, value) }

    var showLyrics: Boolean
        get() = sharedPreferences.getBoolean(SHOW_LYRICS, false)
        set(value) = sharedPreferences.edit { putBoolean(SHOW_LYRICS, value) }

    val rememberLastTab: Boolean
        get() = sharedPreferences.getBoolean(REMEMBER_LAST_TAB, true)

    val enableSearchPlaylist: Boolean
        get() = sharedPreferences.getBoolean(ENABLE_SEARCH_PLAYLIST, true)

    var lastTab: Int
        get() = sharedPreferences
            .getInt(LAST_USED_TAB, 0)
        set(value) = sharedPreferences.edit { putInt(LAST_USED_TAB, value) }

    val isWhiteList: Boolean
        get() = sharedPreferences.getBoolean(WHITELIST_MUSIC, false)

    val crossFadeDuration
        get() = sharedPreferences
            .getInt(CROSS_FADE_DURATION, 0)
            .takeUnless { batterySaverMode }
            ?: 0

    val isCrossfadeEnabled get() = crossFadeDuration > 0

    val materialYou
        get() = sharedPreferences.getBoolean(MATERIAL_YOU, VersionUtils.hasS())

    val isCustomFont
        get() = sharedPreferences.getBoolean(CUSTOM_FONT, false)

    val isSnowFalling
        get() = sharedPreferences.getBoolean(SNOWFALL, false) && !batterySaverMode

    val lyricsType: CoverLyricsType
        get() = if (sharedPreferences.getString(LYRICS_TYPE, "0") == "0") {
            CoverLyricsType.REPLACE_COVER
        } else {
            CoverLyricsType.OVER_COVER
        }

    var playbackSpeed
        get() = sharedPreferences
            .getFloat(PLAYBACK_SPEED, 1F)
        set(value) = sharedPreferences.edit { putFloat(PLAYBACK_SPEED, value) }

    var playbackPitch
        get() = sharedPreferences
            .getFloat(PLAYBACK_PITCH, 1F)
        set(value) = sharedPreferences.edit { putFloat(PLAYBACK_PITCH, value) }

    val appBarMode: TopAppBarLayout.AppBarMode
        get() = if (sharedPreferences.getString(APPBAR_MODE, "1") == "0") {
            TopAppBarLayout.AppBarMode.COLLAPSING
        } else {
            TopAppBarLayout.AppBarMode.SIMPLE
        }

    val wallpaperAccent
        get() = sharedPreferences.getBoolean(
            WALLPAPER_ACCENT,
            VersionUtils.hasOreoMR1() && !VersionUtils.hasS()
        )

    val lyricsScreenOn
        get() = !batterySaverMode && sharedPreferences.getBoolean(SCREEN_ON_LYRICS, false)

    val circlePlayButton
        get() = sharedPreferences.getBoolean(CIRCLE_PLAY_BUTTON, false)

    val swipeAnywhereToChangeSong
        get() = sharedPreferences.getBoolean(SWIPE_ANYWHERE_NOW_PLAYING, true)

    val swipeDownToDismiss
        get() = sharedPreferences.getBoolean(SWIPE_DOWN_DISMISS, true)

    var equalizerEnabled: Boolean
        get() = sharedPreferences.getBoolean(EQUALIZER_ENABLED, false)
        set(value) = sharedPreferences.edit { putBoolean(EQUALIZER_ENABLED, value) }

    var equalizerPreset: Int
        get() = sharedPreferences.getInt(EQUALIZER_PRESET, -1)
        set(value) = sharedPreferences.edit { putInt(EQUALIZER_PRESET, value) }

    var bassBoostStrength: Int
        get() = sharedPreferences.getInt(BASS_BOOST_STRENGTH, 0)
        set(value) = sharedPreferences.edit { putInt(BASS_BOOST_STRENGTH, value) }

    var virtualizerStrength: Int
        get() = sharedPreferences.getInt(VIRTUALIZER_STRENGTH, 0)
        set(value) = sharedPreferences.edit { putInt(VIRTUALIZER_STRENGTH, value) }

    var equalizerBands: String
        get() = sharedPreferences.getStringOrDefault(EQUALIZER_BANDS, "")
        set(value) = sharedPreferences.edit { putString(EQUALIZER_BANDS, value) }

    var showRadioFab: Boolean
        get() = sharedPreferences.getBoolean(SHOW_RADIO_FAB, true)
        set(value) = sharedPreferences.edit { putBoolean(SHOW_RADIO_FAB, value) }

    var radioFabX: Float
        get() = sharedPreferences.getFloat(RADIO_FAB_X, -1f)
        set(value) = sharedPreferences.edit { putFloat(RADIO_FAB_X, value) }

    var radioFabY: Float
        get() = sharedPreferences.getFloat(RADIO_FAB_Y, -1f)
        set(value) = sharedPreferences.edit { putFloat(RADIO_FAB_Y, value) }

    var miniPlayerX: Float
        get() = sharedPreferences.getFloat(MINI_PLAYER_X, -1f)
        set(value) = sharedPreferences.edit { putFloat(MINI_PLAYER_X, value) }

    var miniPlayerY: Float
        get() = sharedPreferences.getFloat(MINI_PLAYER_Y, -1f)
        set(value) = sharedPreferences.edit { putFloat(MINI_PLAYER_Y, value) }

    var homeShortcuts: Set<String>
        get() = sharedPreferences.getStringSet(HOME_SHORTCUTS, DEFAULT_HOME_SHORTCUTS)?.toSet()
            ?: DEFAULT_HOME_SHORTCUTS
        set(value) = sharedPreferences.edit { putStringSet(HOME_SHORTCUTS, value) }

    var tvDefaultsInitialized: Boolean
        get() = sharedPreferences.getBoolean(TV_DEFAULTS_INITIALIZED, false)
        set(value) = sharedPreferences.edit { putBoolean(TV_DEFAULTS_INITIALIZED, value) }

    var tvDefaultsVersion: Int
        get() = sharedPreferences.getInt(TV_DEFAULTS_VERSION, 0)
        set(value) = sharedPreferences.edit { putInt(TV_DEFAULTS_VERSION, value) }

    var youtubeDefaultsInitialized: Boolean
        get() = sharedPreferences.getBoolean(YOUTUBE_DEFAULTS_INITIALIZED, false)
        set(value) = sharedPreferences.edit { putBoolean(YOUTUBE_DEFAULTS_INITIALIZED, value) }

    var youtubeFeedLastSyncAt: Long
        get() = sharedPreferences.getLong(YOUTUBE_FEED_LAST_SYNC_AT, 0L)
        set(value) = sharedPreferences.edit { putLong(YOUTUBE_FEED_LAST_SYNC_AT, value) }

    var youtubeFeedLastAttemptAt: Long
        get() = sharedPreferences.getLong(YOUTUBE_FEED_LAST_ATTEMPT_AT, 0L)
        set(value) = sharedPreferences.edit { putLong(YOUTUBE_FEED_LAST_ATTEMPT_AT, value) }

    var youtubeFeedLastFailureKind: String
        get() = sharedPreferences.getStringOrDefault(YOUTUBE_FEED_LAST_FAILURE_KIND, "")
        set(value) = sharedPreferences.edit { putString(YOUTUBE_FEED_LAST_FAILURE_KIND, value) }

    var youtubeFeedSyncSignature: String
        get() = sharedPreferences.getStringOrDefault(YOUTUBE_FEED_SYNC_SIGNATURE, "")
        set(value) = sharedPreferences.edit { putString(YOUTUBE_FEED_SYNC_SIGNATURE, value) }

    private val DEFAULT_HOME_SHORTCUTS = setOf(
        "history",
        "last_added",
        "top_played",
        "shuffle",
        "folder",
        "radio",
        "tv",
        "videos",
        "youtube"
    )
}

enum class CoverLyricsType {
    REPLACE_COVER, OVER_COVER
}
