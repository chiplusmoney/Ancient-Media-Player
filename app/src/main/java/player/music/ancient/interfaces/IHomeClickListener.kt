package player.music.ancient.interfaces

import player.music.ancient.model.Album
import player.music.ancient.model.Artist
import player.music.ancient.model.Genre

interface IHomeClickListener {
    fun onAlbumClick(album: Album)

    fun onArtistClick(artist: Artist)

    fun onGenreClick(genre: Genre)
}