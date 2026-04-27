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
package player.music.ancient.helper

import android.app.SearchManager
import android.os.Bundle
import android.provider.MediaStore
import player.music.ancient.model.Song
import player.music.ancient.repository.RealSongRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object SearchQueryHelper : KoinComponent {
    private const val TITLE_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.TITLE + ") = ?"
    private const val ALBUM_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.ALBUM + ") = ?"
    private const val ARTIST_SELECTION = "lower(" + MediaStore.Audio.AudioColumns.ARTIST + ") = ?"
    private const val AND = " AND "
    private val songRepository by inject<RealSongRepository>()
    var songs = ArrayList<Song>()

    @JvmStatic
    fun getSongs(extras: Bundle): List<Song> {
        val query = extras.getString(SearchManager.QUERY, null)
        val artistName = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST, null)
        val albumName = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM, null)
        val titleName = extras.getString(MediaStore.EXTRA_MEDIA_TITLE, null)

        val candidates = sequence {
            if (artistName != null && albumName != null && titleName != null) {
                yield(Pair(ARTIST_SELECTION + AND + ALBUM_SELECTION + AND + TITLE_SELECTION, arrayOf(artistName, albumName, titleName)))
            }
            if (artistName != null && titleName != null) {
                yield(Pair(ARTIST_SELECTION + AND + TITLE_SELECTION, arrayOf(artistName, titleName)))
            }
            if (albumName != null && titleName != null) {
                yield(Pair(ALBUM_SELECTION + AND + TITLE_SELECTION, arrayOf(albumName, titleName)))
            }
            if (artistName != null) {
                yield(Pair(ARTIST_SELECTION, arrayOf(artistName)))
            }
            if (albumName != null) {
                yield(Pair(ALBUM_SELECTION, arrayOf(albumName)))
            }
            if (titleName != null) {
                yield(Pair(TITLE_SELECTION, arrayOf(titleName)))
            }
            if (query != null) {
                yield(Pair(ARTIST_SELECTION, arrayOf(query)))
                yield(Pair(ALBUM_SELECTION, arrayOf(query)))
                yield(Pair(TITLE_SELECTION, arrayOf(query)))
            }
        }

        for ((selection, args) in candidates) {
            val songs = songRepository.songs(
                songRepository.makeSongCursor(selection, args.map { it.lowercase() }.toTypedArray())
            )
            if (songs.isNotEmpty()) {
                return songs
            }
        }

        return emptyList()
    }
}
