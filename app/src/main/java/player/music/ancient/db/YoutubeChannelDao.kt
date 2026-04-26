package player.music.ancient.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface YoutubeChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYoutubeChannel(channel: YoutubeChannelEntity): Long

    @Delete
    suspend fun deleteYoutubeChannel(channel: YoutubeChannelEntity)

    @Update
    suspend fun updateYoutubeChannel(channel: YoutubeChannelEntity)

    @Query("SELECT * FROM youtube_channels ORDER BY name COLLATE NOCASE ASC")
    fun getAllYoutubeChannels(): LiveData<List<YoutubeChannelEntity>>
}
