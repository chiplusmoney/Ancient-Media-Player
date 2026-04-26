package player.music.ancient.network

import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeApiService {
    @GET("channels")
    suspend fun getChannelDetails(
        @Query("part") part: String = "snippet,statistics",
        @Query("forHandle") forHandle: String? = null,
        @Query("forUsername") forUsername: String? = null,
        @Query("id") id: String? = null,
        @Query("key") apiKey: String
    ): YoutubeChannelResponse

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("channelId") channelId: String,
        @Query("order") order: String = "date",
        @Query("maxResults") maxResults: Int = 10,
        @Query("type") type: String = "video",
        @Query("key") apiKey: String
    ): YoutubeSearchResponse
}

data class YoutubeChannelResponse(
    val items: List<YoutubeChannelItem>?
)

data class YoutubeChannelItem(
    val id: String,
    val snippet: ChannelSnippet,
    val statistics: ChannelStatistics
)

data class ChannelSnippet(
    val title: String,
    val description: String,
    val thumbnails: Thumbnails
)

data class ChannelStatistics(
    val subscriberCount: String
)

data class YoutubeSearchResponse(
    val items: List<YoutubeSearchItem>?
)

data class YoutubeSearchItem(
    val id: VideoId,
    val snippet: VideoSnippet
)

data class VideoId(
    val videoId: String
)

data class VideoSnippet(
    val publishedAt: String,
    val channelId: String,
    val title: String,
    val description: String,
    val thumbnails: Thumbnails,
    val channelTitle: String
)

data class Thumbnails(
    val default: Thumbnail?,
    val medium: Thumbnail?,
    val high: Thumbnail?
)

data class Thumbnail(
    val url: String
)
