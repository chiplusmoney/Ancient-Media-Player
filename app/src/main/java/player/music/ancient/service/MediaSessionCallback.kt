/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package player.music.ancient.service

import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat
import player.music.ancient.auto.AutoMediaIDHelper
import player.music.ancient.helper.MusicPlayerRemote
import player.music.ancient.helper.MusicPlayerRemote.cycleRepeatMode
import player.music.ancient.helper.ShuffleHelper.makeShuffleList
import player.music.ancient.db.RadioStationEntity
import player.music.ancient.model.Album
import player.music.ancient.model.Artist
import player.music.ancient.model.Playlist
import player.music.ancient.model.Song
import player.music.ancient.repository.*
import player.music.ancient.service.MusicService.Companion.CYCLE_REPEAT
import player.music.ancient.service.MusicService.Companion.TOGGLE_FAVORITE
import player.music.ancient.service.MusicService.Companion.TOGGLE_SHUFFLE
import player.music.ancient.util.MusicUtil
import player.music.ancient.util.logD
import player.music.ancient.util.logE
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Created by hemanths on 2019-08-01.
 */

class MediaSessionCallback(
    private val musicService: MusicService,
) : MediaSessionCompat.Callback(), KoinComponent {

    private val songRepository by inject<SongRepository>()
    private val albumRepository by inject<AlbumRepository>()
    private val artistRepository by inject<ArtistRepository>()
    private val genreRepository by inject<GenreRepository>()
    private val playlistRepository by inject<PlaylistRepository>()
    private val topPlayedRepository by inject<TopPlayedRepository>()
    private val roomRepository by inject<RoomRepository>()

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        super.onPlayFromMediaId(mediaId, extras)
        val musicId = AutoMediaIDHelper.extractMusicID(mediaId!!)
        logD("Music Id $musicId")
        val itemId = musicId?.toLong() ?: -1
        val songs: ArrayList<Song> = ArrayList()
        when (val category = AutoMediaIDHelper.extractCategory(mediaId)) {
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM -> {
                val album: Album = albumRepository.album(itemId)
                songs.addAll(album.songs)
                musicService.openQueue(songs, 0, true)
            }
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST -> {
                val artist: Artist = artistRepository.artist(itemId)
                songs.addAll(artist.songs)
                musicService.openQueue(songs, 0, true)
            }
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM_ARTIST -> {
                val artist: Artist =
                    artistRepository.albumArtist(albumRepository.album(itemId).albumArtist!!)
                songs.addAll(artist.songs)
                musicService.openQueue(songs, 0, true)
            }
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_PLAYLIST -> {
                val playlist: Playlist = playlistRepository.playlist(itemId)
                songs.addAll(playlist.getSongs())
                musicService.openQueue(songs, 0, true)
            }
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE -> {
                songs.addAll(genreRepository.songs(itemId))
                musicService.openQueue(songs, 0, true)
            }
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_SHUFFLE -> {
                val allSongs = songRepository.songs().toMutableList()
                makeShuffleList(allSongs, -1)
                musicService.openQueue(allSongs, 0, true)
            }
            AutoMediaIDHelper.MEDIA_ID_RADIO -> {
                songs.addAll(
                    roomRepository.getAllRadioStationsSync().map { station -> station.toRadioSong() }
                )
                var songIndex = MusicUtil.indexOfSongInList(songs, itemId)
                if (songIndex == -1) {
                    songIndex = 0
                }
                musicService.openQueue(songs, songIndex, true)
            }
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_HISTORY,
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_SUGGESTIONS,
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_TOP_TRACKS,
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_QUEUE,
            -> {
                val tracks = tracksForAutoCategory(category)
                songs.addAll(tracks)
                var songIndex = MusicUtil.indexOfSongInList(tracks, itemId)
                if (songIndex == -1) {
                    songIndex = 0
                }
                musicService.openQueue(songs, songIndex, true)
            }
        }
        musicService.play()
    }

    override fun onPlayFromSearch(query: String?, extras: Bundle?) {
        val songs = ArrayList<Song>()
        if (query.isNullOrEmpty()) {
            // The user provided generic string e.g. 'Play music'
            // Build appropriate playlist queue
            songs.addAll(songRepository.songs())
        } else {
            // Build a queue based on songs that match "query" or "extras" param
            val mediaFocus: String? = extras?.getString(MediaStore.EXTRA_MEDIA_FOCUS)
            if (mediaFocus == MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE) {
                val artistQuery = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                if (artistQuery != null) {
                    artistRepository.artists(artistQuery).forEach {
                        songs.addAll(it.songs)
                    }
                }
            } else if (mediaFocus == MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE) {
                val albumQuery = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                if (albumQuery != null) {
                    albumRepository.albums(albumQuery).forEach {
                        songs.addAll(it.songs)
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            val radioStations = query?.let(::matchingRadioStations).orEmpty()
            if (radioStations.isNotEmpty()) {
                val radioSongs = radioStations.map { it.toRadioSong() }
                musicService.openQueue(radioSongs, 0, true)
                musicService.play()
                return
            }

            // No focus found, search by query for song title
            query?.also {
                songs.addAll(songRepository.songs(it))
            }
        }

        musicService.openQueue(songs, 0, true)

        musicService.play()
    }

    override fun onPrepare() {
        super.onPrepare()
        if (musicService.currentSong != Song.emptySong)
            musicService.restoreState(::onPlay)
    }

    override fun onPlay() {
        super.onPlay()
        if (musicService.currentSong != Song.emptySong) musicService.play()
    }

    override fun onPause() {
        super.onPause()
        musicService.pause()
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        musicService.playNextSong(true)
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        musicService.playPreviousSong(true)
    }

    override fun onStop() {
        super.onStop()
        musicService.quit()
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        musicService.seek(pos.toInt())
    }

    override fun onCustomAction(action: String, extras: Bundle?) {
        when (action) {
            CYCLE_REPEAT -> {
                cycleRepeatMode()
                musicService.updateMediaSessionPlaybackState()
            }

            TOGGLE_SHUFFLE -> {
                musicService.toggleShuffle()
                musicService.updateMediaSessionPlaybackState()
            }
            TOGGLE_FAVORITE -> {
                musicService.toggleFavorite()
            }
            else -> {
                logE("Unsupported action: $action")
            }
        }
    }

    private fun checkAndStartPlaying(songs: ArrayList<Song>, itemId: Long) {
        var songIndex = MusicUtil.indexOfSongInList(songs, itemId)
        if (songIndex == -1) {
            songIndex = 0
        }
        openQueue(songs, songIndex)
    }

    private fun openQueue(songs: ArrayList<Song>, index: Int, startPlaying: Boolean = true) {
        MusicPlayerRemote.openQueue(songs, index, startPlaying)
    }

    private fun tracksForAutoCategory(category: String?): List<Song> {
        return when (category) {
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_HISTORY -> topPlayedRepository.recentlyPlayedTracks()
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_SUGGESTIONS -> topPlayedRepository.notRecentlyPlayedTracks().take(8)
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_TOP_TRACKS -> topPlayedRepository.topTracks()
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_QUEUE -> musicService.playingQueue
            else -> emptyList()
        }
    }

    private fun matchingRadioStations(query: String): List<RadioStationEntity> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        return roomRepository.getAllRadioStationsSync().filter { station ->
            station.name.contains(normalizedQuery, ignoreCase = true) ||
                station.uri.contains(normalizedQuery, ignoreCase = true)
        }
    }

    private fun RadioStationEntity.toRadioSong(): Song {
        return Song(
            id = id,
            title = name,
            trackNumber = 0,
            year = 0,
            duration = -1,
            data = uri,
            dateModified = System.currentTimeMillis(),
            albumId = -1,
            albumName = "Radio",
            artistId = -1,
            artistName = "Radio Stream",
            composer = "",
            albumArtist = "",
            isRadio = true
        )
    }
}
