package player.music.ancient.service

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import player.music.ancient.extensions.albumArtUri
import player.music.ancient.model.Song

class PersistentStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveSong(song: Song) {
        prefs.edit {
            putLong("song_id", song.id)
            putString("song_title", song.title)
            putString("song_artist", song.artistName)
            putString("song_cover", song.albumArtUri?.toString())
        }
    }

    fun recentSong(): MediaBrowserCompat.MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setMediaId(prefs.getLong("song_id", 0L).toString())
            .setTitle(prefs.getString("song_title", ""))
            .setSubtitle(prefs.getString("song_artist", ""))
            .apply {
                prefs.getString("song_cover", null)
                    ?.takeIf { it.isNotBlank() }
                    ?.toUri()
                    ?.let(::setIconUri)
            }
            .build()
        return MediaBrowserCompat.MediaItem(
            description,
            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        )
    }

    companion object {
        const val PREFERENCES_NAME = "ancient_recent"

        @Volatile
        private var instance: PersistentStorage? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: PersistentStorage(context).also { instance = it }
            }
    }
}
