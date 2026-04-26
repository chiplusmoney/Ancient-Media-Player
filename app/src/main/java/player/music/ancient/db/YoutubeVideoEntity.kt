package player.music.ancient.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "youtube_videos",
    indices = [
        Index(value = ["source_channel_id"]),
        Index(value = ["published_at"])
    ]
)
data class YoutubeVideoEntity(
    @PrimaryKey
    @ColumnInfo(name = "video_id")
    val videoId: String,
    @ColumnInfo(name = "source_channel_id")
    val sourceChannelId: Long,
    @ColumnInfo(name = "youtube_channel_id")
    val youtubeChannelId: String,
    val title: String,
    val description: String,
    @ColumnInfo(name = "published_at")
    val publishedAt: String,
    @ColumnInfo(name = "channel_title")
    val channelTitle: String,
    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String?
)
