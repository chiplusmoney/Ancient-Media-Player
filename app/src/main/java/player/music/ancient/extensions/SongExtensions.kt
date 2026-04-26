package player.music.ancient.extensions

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import androidx.core.net.toUri
import player.music.ancient.model.Song
import player.music.ancient.util.MusicUtil

val Song.uri get() = if (isRadio) data.toUri() else MusicUtil.getSongFileUri(songId = id)

val Song.albumArtUri
    get() = if (isRadio || albumId < 0) null else MusicUtil.getMediaStoreAlbumCoverUri(albumId)

fun ArrayList<Song>.toMediaSessionQueue(): List<QueueItem> {
    return map { song ->
        val mediaDescription = MediaDescriptionCompat.Builder()
            .setMediaId(song.id.toString())
            .setTitle(song.title)
            .setSubtitle(song.artistName)
            .apply {
                song.albumArtUri?.let(::setIconUri)
            }
            .build()
        QueueItem(mediaDescription, song.hashCode().toLong())
    }
}
