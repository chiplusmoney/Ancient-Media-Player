package player.music.ancient.model.smartplaylist

import player.music.ancient.App
import player.music.ancient.R
import player.music.ancient.model.Song
import kotlinx.parcelize.Parcelize

@Parcelize
class TopTracksPlaylist : AbsSmartPlaylist(
    name = App.getContext().getString(R.string.my_top_tracks),
    iconRes = R.drawable.ic_trending_up
) {
    override fun songs(): List<Song> {
        return topPlayedRepository.topTracks()
    }
}