package player.music.ancient.fragments.youtube

import player.music.ancient.db.YoutubeVideoEntity
import player.music.ancient.network.YoutubeSearchItem

data class YoutubeFeedVideo(
    val videoId: String,
    val title: String,
    val description: String,
    val publishedAt: String,
    val channelId: String,
    val channelTitle: String,
    val thumbnailUrl: String?
)

fun YoutubeVideoEntity.toFeedVideo(): YoutubeFeedVideo {
    return YoutubeFeedVideo(
        videoId = videoId,
        title = title,
        description = description,
        publishedAt = publishedAt,
        channelId = youtubeChannelId,
        channelTitle = channelTitle,
        thumbnailUrl = thumbnailUrl
    )
}

fun YoutubeSearchItem.toCachedEntity(
    sourceChannelId: Long,
    resolvedChannelId: String
): YoutubeVideoEntity {
    val snippet = snippet
    return YoutubeVideoEntity(
        videoId = id.videoId,
        sourceChannelId = sourceChannelId,
        youtubeChannelId = resolvedChannelId,
        title = snippet.title,
        description = snippet.description,
        publishedAt = snippet.publishedAt,
        channelTitle = snippet.channelTitle,
        thumbnailUrl = snippet.thumbnails.high?.url
            ?: snippet.thumbnails.medium?.url
            ?: snippet.thumbnails.default?.url
    )
}
