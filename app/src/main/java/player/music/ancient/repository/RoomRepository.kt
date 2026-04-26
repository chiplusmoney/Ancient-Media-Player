package player.music.ancient.repository

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import player.music.ancient.R
import player.music.ancient.db.*
import player.music.ancient.helper.SortOrder.PlaylistSortOrder.Companion.PLAYLIST_A_Z
import player.music.ancient.helper.SortOrder.PlaylistSortOrder.Companion.PLAYLIST_SONG_COUNT
import player.music.ancient.helper.SortOrder.PlaylistSortOrder.Companion.PLAYLIST_SONG_COUNT_DESC
import player.music.ancient.helper.SortOrder.PlaylistSortOrder.Companion.PLAYLIST_Z_A
import player.music.ancient.model.Song
import player.music.ancient.util.PreferenceUtil
import java.text.Collator


interface RoomRepository {
    fun historySongs(): List<HistoryEntity>
    fun favoritePlaylistLiveData(favorite: String): LiveData<List<SongEntity>>
    fun observableHistorySongs(): LiveData<List<HistoryEntity>>
    fun getSongs(playListId: Long): LiveData<List<SongEntity>>
    suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long
    suspend fun checkPlaylistExists(playlistName: String): List<PlaylistEntity>
    suspend fun playlists(): List<PlaylistEntity>
    suspend fun playlistWithSongs(): List<PlaylistWithSongs>
    suspend fun insertSongs(songs: List<SongEntity>)
    suspend fun deletePlaylistEntities(playlistEntities: List<PlaylistEntity>)
    suspend fun renamePlaylistEntity(playlistId: Long, name: String)
    suspend fun deleteSongsInPlaylist(songs: List<SongEntity>)
    suspend fun deletePlaylistSongs(playlists: List<PlaylistEntity>)
    suspend fun favoritePlaylist(favorite: String): PlaylistEntity
    suspend fun isFavoriteSong(songEntity: SongEntity): List<SongEntity>
    suspend fun removeSongFromPlaylist(songEntity: SongEntity)
    suspend fun upsertSongInHistory(currentSong: Song)
    suspend fun favoritePlaylistSongs(favorite: String): List<SongEntity>
    suspend fun upsertSongInPlayCount(playCountEntity: PlayCountEntity)
    suspend fun deleteSongInPlayCount(playCountEntity: PlayCountEntity)
    suspend fun deleteSongInHistory(songId: Long)
    suspend fun clearSongHistory()
    suspend fun findSongExistInPlayCount(songId: Long): PlayCountEntity?
    suspend fun playCountSongs(): List<PlayCountEntity>
    suspend fun deleteSongs(songs: List<Song>)
    suspend fun isSongFavorite(context: Context, songId: Long): Boolean
    fun checkPlaylistExists(playListId: Long): LiveData<Boolean>
    fun getPlaylist(playlistId: Long): LiveData<PlaylistWithSongs>
    fun getAllRadioStations(): LiveData<List<RadioStationEntity>>
    fun getAllRadioStationsSync(): List<RadioStationEntity>
    suspend fun insertRadioStation(radioStation: RadioStationEntity): Long
    suspend fun deleteRadioStation(radioStation: RadioStationEntity)
    suspend fun updateRadioStation(radioStation: RadioStationEntity)
    fun getAllRadioCategories(): LiveData<List<RadioCategoryEntity>>
    suspend fun insertRadioCategory(radioCategory: RadioCategoryEntity): Long
    suspend fun deleteRadioCategory(radioCategory: RadioCategoryEntity)
    suspend fun updateRadioCategory(radioCategory: RadioCategoryEntity)
    suspend fun clearCategoryFromStations(categoryId: Long)
    fun getAllTvChannels(): LiveData<List<TvChannelEntity>>
    fun getAllTvCategories(): LiveData<List<TvCategoryEntity>>
    suspend fun insertTvChannel(tvChannel: TvChannelEntity): Long
    suspend fun deleteTvChannel(tvChannel: TvChannelEntity)
    suspend fun updateTvChannel(tvChannel: TvChannelEntity)
    suspend fun insertTvCategory(tvCategory: TvCategoryEntity): Long
    suspend fun deleteTvCategory(tvCategory: TvCategoryEntity)
    suspend fun updateTvCategory(tvCategory: TvCategoryEntity)
    suspend fun clearCategoryFromTvChannels(categoryId: Long)
    fun getAllYoutubeChannels(): LiveData<List<YoutubeChannelEntity>>
    suspend fun insertYoutubeChannel(channel: YoutubeChannelEntity): Long
    suspend fun deleteYoutubeChannel(channel: YoutubeChannelEntity)
    suspend fun updateYoutubeChannel(channel: YoutubeChannelEntity)
    fun getCachedYoutubeVideos(sourceChannelIds: List<Long>): LiveData<List<YoutubeVideoEntity>>
    suspend fun replaceCachedYoutubeVideos(
        sourceChannelId: Long,
        videos: List<YoutubeVideoEntity>
    )
    suspend fun pruneCachedYoutubeVideos(validSourceChannelIds: List<Long>)
}

class RealRoomRepository(
    private val playlistDao: PlaylistDao,
    private val playCountDao: PlayCountDao,
    private val historyDao: HistoryDao,
    private val radioStationDao: RadioStationDao,
    private val radioCategoryDao: RadioCategoryDao,
    private val tvChannelDao: TvChannelDao,
    private val tvCategoryDao: TvCategoryDao,
    private val youtubeChannelDao: YoutubeChannelDao,
    private val youtubeVideoDao: YoutubeVideoDao
) : RoomRepository {
    @WorkerThread
    override suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long =
        playlistDao.createPlaylist(playlistEntity)

    @WorkerThread
    override suspend fun checkPlaylistExists(playlistName: String): List<PlaylistEntity> =
        playlistDao.playlist(playlistName)

    @WorkerThread
    override suspend fun playlists(): List<PlaylistEntity> = playlistDao.playlists()

    @WorkerThread
    override suspend fun playlistWithSongs(): List<PlaylistWithSongs> {
        val collator = Collator.getInstance()
        return when (PreferenceUtil.playlistSortOrder) {
            PLAYLIST_A_Z ->
                playlistDao.playlistsWithSongs().sortedWith { p1, p2 ->
                    collator.compare(p1.playlistEntity.playlistName, p2.playlistEntity.playlistName)
                }
            PLAYLIST_Z_A -> playlistDao.playlistsWithSongs()
                .sortedWith { p1, p2 ->
                    collator.compare(p2.playlistEntity.playlistName, p1.playlistEntity.playlistName)
                }
            PLAYLIST_SONG_COUNT -> playlistDao.playlistsWithSongs().sortedBy { it.songs.size }
            PLAYLIST_SONG_COUNT_DESC -> playlistDao.playlistsWithSongs()
                .sortedByDescending { it.songs.size }
            else -> playlistDao.playlistsWithSongs().sortedWith { p1, p2 ->
                collator.compare(p1.playlistEntity.playlistName, p2.playlistEntity.playlistName)
            }
        }
    }

    @WorkerThread
    override fun getPlaylist(playlistId: Long): LiveData<PlaylistWithSongs> = playlistDao.getPlaylist(playlistId)

    @WorkerThread
    override suspend fun insertSongs(songs: List<SongEntity>) {

        playlistDao.insertSongsToPlaylist(songs)
    }

    override fun getSongs(playListId: Long): LiveData<List<SongEntity>> =
        playlistDao.songsFromPlaylist(playListId)

    override fun checkPlaylistExists(playListId: Long): LiveData<Boolean> =
        playlistDao.checkPlaylistExists(playListId)

    override suspend fun deletePlaylistEntities(playlistEntities: List<PlaylistEntity>) =
        playlistDao.deletePlaylists(playlistEntities)

    override suspend fun renamePlaylistEntity(playlistId: Long, name: String) =
        playlistDao.renamePlaylist(playlistId, name)

    override suspend fun deleteSongsInPlaylist(songs: List<SongEntity>) {
        songs.forEach {
            playlistDao.deleteSongFromPlaylist(it.playlistCreatorId, it.id)
        }
    }

    override suspend fun deletePlaylistSongs(playlists: List<PlaylistEntity>) =
        playlists.forEach {
            playlistDao.deletePlaylistSongs(it.playListId)
        }

    override suspend fun favoritePlaylist(favorite: String): PlaylistEntity {
        val playlist: PlaylistEntity? = playlistDao.playlist(favorite).firstOrNull()
        return if (playlist != null) {
            playlist
        } else {
            createPlaylist(PlaylistEntity(playlistName = favorite))
            playlistDao.playlist(favorite).first()
        }
    }

    override suspend fun isFavoriteSong(songEntity: SongEntity): List<SongEntity> =
        playlistDao.isSongExistsInPlaylist(
            songEntity.playlistCreatorId,
            songEntity.id
        )

    override suspend fun removeSongFromPlaylist(songEntity: SongEntity) =
        playlistDao.deleteSongFromPlaylist(songEntity.playlistCreatorId, songEntity.id)

    override suspend fun upsertSongInHistory(currentSong: Song) =
        historyDao.upsertSongInHistory(currentSong.toHistoryEntity(System.currentTimeMillis()))

    override fun observableHistorySongs(): LiveData<List<HistoryEntity>> =
        historyDao.observableHistorySongs()

    override fun historySongs(): List<HistoryEntity> = historyDao.historySongs()

    override fun favoritePlaylistLiveData(favorite: String): LiveData<List<SongEntity>> =
        playlistDao.favoritesSongsLiveData(favorite)

    override suspend fun favoritePlaylistSongs(favorite: String): List<SongEntity> =
        if (playlistDao.playlist(favorite).isNotEmpty()) playlistDao.favoritesSongs(
            playlistDao.playlist(favorite).first().playListId
        ) else emptyList()

    override suspend fun upsertSongInPlayCount(playCountEntity: PlayCountEntity) =
        playCountDao.upsertSongInPlayCount(playCountEntity)

    override suspend fun deleteSongInPlayCount(playCountEntity: PlayCountEntity) =
        playCountDao.deleteSongInPlayCount(playCountEntity)

    override suspend fun deleteSongInHistory(songId: Long) {
        historyDao.deleteSongInHistory(songId)
    }

    override suspend fun clearSongHistory() {
        historyDao.clearHistory()
    }

    override suspend fun findSongExistInPlayCount(songId: Long): PlayCountEntity? =
        playCountDao.findSongExistInPlayCount(songId)

    override suspend fun playCountSongs(): List<PlayCountEntity> =
        playCountDao.playCountSongs()

    override suspend fun deleteSongs(songs: List<Song>) = songs.forEach {
        playCountDao.deleteSong(it.id)
    }

    override suspend fun isSongFavorite(context: Context, songId: Long): Boolean {
        return playlistDao.isSongExistsInPlaylist(
            playlistDao.playlist(context.getString(R.string.favorites)).firstOrNull()?.playListId
                ?: -1,
            songId
        ).isNotEmpty()
    }

    override fun getAllRadioStations(): LiveData<List<RadioStationEntity>> =
        radioStationDao.getAllRadioStations()

    override fun getAllRadioStationsSync(): List<RadioStationEntity> =
        radioStationDao.getAllRadioStationsSync()

    override suspend fun insertRadioStation(radioStation: RadioStationEntity): Long =
        radioStationDao.insertRadioStation(radioStation)

    override suspend fun deleteRadioStation(radioStation: RadioStationEntity) =
        radioStationDao.deleteRadioStation(radioStation)

    override suspend fun updateRadioStation(radioStation: RadioStationEntity) =
        radioStationDao.updateRadioStation(radioStation)

    override fun getAllRadioCategories(): LiveData<List<RadioCategoryEntity>> =
        radioCategoryDao.getAllRadioCategories()

    override suspend fun insertRadioCategory(radioCategory: RadioCategoryEntity): Long =
        radioCategoryDao.insertRadioCategory(radioCategory)

    override suspend fun deleteRadioCategory(radioCategory: RadioCategoryEntity) =
        radioCategoryDao.deleteRadioCategory(radioCategory)

    override suspend fun updateRadioCategory(radioCategory: RadioCategoryEntity) =
        radioCategoryDao.updateRadioCategory(radioCategory)

    override suspend fun clearCategoryFromStations(categoryId: Long) =
        radioStationDao.clearCategoryFromStations(categoryId)

    override fun getAllTvChannels(): LiveData<List<TvChannelEntity>> =
        tvChannelDao.getAllTvChannels()

    override fun getAllTvCategories(): LiveData<List<TvCategoryEntity>> =
        tvCategoryDao.getAllTvCategories()

    override suspend fun insertTvChannel(tvChannel: TvChannelEntity): Long =
        tvChannelDao.insertTvChannel(tvChannel)

    override suspend fun deleteTvChannel(tvChannel: TvChannelEntity) =
        tvChannelDao.deleteTvChannel(tvChannel)

    override suspend fun updateTvChannel(tvChannel: TvChannelEntity) =
        tvChannelDao.updateTvChannel(tvChannel)

    override suspend fun insertTvCategory(tvCategory: TvCategoryEntity): Long =
        tvCategoryDao.insertTvCategory(tvCategory)

    override suspend fun deleteTvCategory(tvCategory: TvCategoryEntity) =
        tvCategoryDao.deleteTvCategory(tvCategory)

    override suspend fun updateTvCategory(tvCategory: TvCategoryEntity) =
        tvCategoryDao.updateTvCategory(tvCategory)

    override suspend fun clearCategoryFromTvChannels(categoryId: Long) =
        tvChannelDao.clearCategoryFromChannels(categoryId)

    override fun getAllYoutubeChannels(): LiveData<List<YoutubeChannelEntity>> =
        youtubeChannelDao.getAllYoutubeChannels()

    override suspend fun insertYoutubeChannel(channel: YoutubeChannelEntity): Long =
        youtubeChannelDao.insertYoutubeChannel(channel)

    override suspend fun deleteYoutubeChannel(channel: YoutubeChannelEntity) =
        youtubeChannelDao.deleteYoutubeChannel(channel)

    override suspend fun updateYoutubeChannel(channel: YoutubeChannelEntity) =
        youtubeChannelDao.updateYoutubeChannel(channel)

    override fun getCachedYoutubeVideos(sourceChannelIds: List<Long>): LiveData<List<YoutubeVideoEntity>> =
        youtubeVideoDao.observeVideosForSources(sourceChannelIds)

    override suspend fun replaceCachedYoutubeVideos(
        sourceChannelId: Long,
        videos: List<YoutubeVideoEntity>
    ) = youtubeVideoDao.replaceVideosForSource(sourceChannelId, videos)

    override suspend fun pruneCachedYoutubeVideos(validSourceChannelIds: List<Long>) {
        if (validSourceChannelIds.isEmpty()) {
            youtubeVideoDao.clear()
        } else {
            youtubeVideoDao.deleteVideosForOtherSources(validSourceChannelIds)
        }
    }
}
