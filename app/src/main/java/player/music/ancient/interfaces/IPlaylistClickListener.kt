package player.music.ancient.interfaces

import android.view.View
import player.music.ancient.db.PlaylistWithSongs

interface IPlaylistClickListener {
    fun onPlaylistClick(playlistWithSongs: PlaylistWithSongs, view: View)
}