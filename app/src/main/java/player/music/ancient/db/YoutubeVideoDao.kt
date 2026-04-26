package player.music.ancient.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface YoutubeVideoDao {
    @Query(
        "SELECT * FROM youtube_videos " +
            "WHERE source_channel_id IN (:sourceChannelIds) " +
            "ORDER BY published_at DESC, video_id DESC"
    )
    fun observeVideosForSources(sourceChannelIds: List<Long>): LiveData<List<YoutubeVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<YoutubeVideoEntity>)

    @Query("DELETE FROM youtube_videos WHERE source_channel_id = :sourceChannelId")
    suspend fun deleteVideosForSource(sourceChannelId: Long)

    @Query("DELETE FROM youtube_videos WHERE source_channel_id NOT IN (:sourceChannelIds)")
    suspend fun deleteVideosForOtherSources(sourceChannelIds: List<Long>)

    @Query("DELETE FROM youtube_videos")
    suspend fun clear()

    @Transaction
    suspend fun replaceVideosForSource(
        sourceChannelId: Long,
        videos: List<YoutubeVideoEntity>
    ) {
        deleteVideosForSource(sourceChannelId)
        if (videos.isNotEmpty()) {
            insertVideos(videos)
        }
    }
}
