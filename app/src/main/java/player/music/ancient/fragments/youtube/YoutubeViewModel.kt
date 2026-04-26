package player.music.ancient.fragments.youtube

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.os.PowerManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import player.music.ancient.App
import player.music.ancient.BuildConfig
import player.music.ancient.db.YoutubeChannelEntity
import player.music.ancient.db.YoutubeVideoEntity
import player.music.ancient.network.YoutubeApiService
import player.music.ancient.repository.Repository
import player.music.ancient.util.PreferenceUtil
import retrofit2.HttpException

data class YoutubeFeedUiState(
    val videos: List<YoutubeFeedVideo> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val placeholder: YoutubePlaceholderState? = null
)

data class YoutubePlaceholderState(
    val kind: YoutubePlaceholderKind,
    val action: YoutubePlaceholderAction = YoutubePlaceholderAction.NONE,
    val detail: String? = null
)

enum class YoutubePlaceholderKind {
    NO_CHANNELS,
    EMPTY_FEED,
    NETWORK_ERROR,
    QUOTA_EXCEEDED,
    CONFIGURATION_ERROR,
    CHANNEL_RESOLUTION_ERROR,
    SERVICE_ERROR,
    UNKNOWN_ERROR
}

enum class YoutubePlaceholderAction {
    NONE,
    RETRY,
    ADD_CHANNEL
}

class YoutubeViewModel(
    private val repository: Repository,
    private val apiService: YoutubeApiService
) : ViewModel() {
    val youtubeChannels: LiveData<List<YoutubeChannelEntity>> = repository.getAllYoutubeChannels()

    private val _uiState = MutableLiveData(YoutubeFeedUiState())
    val uiState: LiveData<YoutubeFeedUiState> = _uiState

    private val _messages = MutableSharedFlow<YoutubePlaceholderState>(extraBufferCapacity = 1)
    val messages: SharedFlow<YoutubePlaceholderState> = _messages.asSharedFlow()

    private var trackedChannels: List<YoutubeChannelEntity> = emptyList()
    private var trackedSignature: String? = null
    private var feedJob: Job? = null
    private var cachedVideoSource: LiveData<List<YoutubeVideoEntity>>? = null
    private var cachedVideosSnapshot: List<YoutubeFeedVideo> = emptyList()

    private val cachedVideoObserver = Observer<List<YoutubeVideoEntity>> { entities ->
        cachedVideosSnapshot = entities.orEmpty().map(YoutubeVideoEntity::toFeedVideo)
        val currentState = _uiState.value ?: YoutubeFeedUiState()
        if (cachedVideosSnapshot.isNotEmpty()) {
            _uiState.value = currentState.copy(
                videos = cachedVideosSnapshot,
                isInitialLoading = false,
                placeholder = null
            )
        } else if (trackedChannels.isNotEmpty()) {
            _uiState.value = currentState.copy(videos = emptyList())
        }
    }

    fun onChannelsChanged(channels: List<YoutubeChannelEntity>) {
        trackedChannels = channels.sortedBy { it.id }

        if (trackedChannels.isEmpty()) {
            trackedSignature = null
            feedJob?.cancel()
            cachedVideoSource?.removeObserver(cachedVideoObserver)
            cachedVideoSource = null
            cachedVideosSnapshot = emptyList()
            viewModelScope.launch(IO) {
                repository.pruneCachedYoutubeVideos(emptyList())
            }
            _uiState.value = YoutubeFeedUiState(
                placeholder = YoutubePlaceholderState(
                    kind = YoutubePlaceholderKind.NO_CHANNELS,
                    action = YoutubePlaceholderAction.ADD_CHANNEL
                )
            )
            return
        }

        val nextSignature = trackedChannels.joinToString(separator = "|") { channel ->
            "${channel.id}:${channel.name}:${channel.url}"
        }
        val signatureChanged = nextSignature != trackedSignature
        trackedSignature = nextSignature

        if (signatureChanged) {
            _uiState.value = YoutubeFeedUiState(isInitialLoading = true)
            subscribeToCachedVideos(trackedChannels)
            refreshFeed(force = cachedVideosSnapshot.isEmpty() && recentFailureKind() == null)
            return
        }

        val currentState = _uiState.value ?: YoutubeFeedUiState()
        if (currentState.videos.isEmpty()) {
            refreshFeed(force = currentState.placeholder == null && recentFailureKind() == null)
        } else {
            refreshFeed(force = false)
        }
    }

    fun refreshFeed(force: Boolean = true) {
        if (trackedChannels.isEmpty()) {
            _uiState.value = YoutubeFeedUiState(
                placeholder = YoutubePlaceholderState(
                    kind = YoutubePlaceholderKind.NO_CHANNELS,
                    action = YoutubePlaceholderAction.ADD_CHANNEL
                )
            )
            return
        }

        if (feedJob?.isActive == true && !force) {
            return
        }

        if (force) {
            feedJob?.cancel()
        }

        val cachedVideos = cachedVideosSnapshot
        if (!force && !shouldAutoRefresh(cachedVideos)) {
            applyDeferredState(cachedVideos)
            return
        }

        feedJob = viewModelScope.launch(IO) {
            val currentState = _uiState.value ?: YoutubeFeedUiState()

            _uiState.postValue(
                if (cachedVideos.isEmpty()) {
                    YoutubeFeedUiState(isInitialLoading = true)
                } else {
                    currentState.copy(
                        videos = cachedVideos,
                        isInitialLoading = false,
                        isRefreshing = true,
                        placeholder = null
                    )
                }
            )

            try {
                PreferenceUtil.youtubeFeedLastAttemptAt = System.currentTimeMillis()
                PreferenceUtil.youtubeFeedSyncSignature = trackedSignature.orEmpty()

                if (BuildConfig.YOUTUBE_API_KEY.contains("missing", ignoreCase = true)) {
                    throw YoutubeFeedException.Configuration
                }

                repository.pruneCachedYoutubeVideos(trackedChannels.map { it.id })

                val allVideos = mutableListOf<YoutubeFeedVideo>()
                var successfulChannelCount = 0
                var resolvedFailures = 0

                for (channel in trackedChannels) {
                    val channelId = resolveChannelId(channel)
                    if (channelId == null) {
                        resolvedFailures++
                        repository.replaceCachedYoutubeVideos(channel.id, emptyList())
                        continue
                    }

                    val response = apiService.searchVideos(
                        channelId = channelId,
                        apiKey = BuildConfig.YOUTUBE_API_KEY
                    )
                    val cachedEntities = response.items.orEmpty()
                        .map { it.toCachedEntity(channel.id, channelId) }
                    repository.replaceCachedYoutubeVideos(channel.id, cachedEntities)
                    allVideos += cachedEntities.map(YoutubeVideoEntity::toFeedVideo)
                    successfulChannelCount++
                }

                allVideos.sortByDescending { it.publishedAt }

                if (successfulChannelCount == 0 && resolvedFailures > 0) {
                    throw YoutubeFeedException.ChannelResolution
                }

                _uiState.postValue(
                    if (allVideos.isEmpty()) {
                        YoutubeFeedUiState(
                            placeholder = YoutubePlaceholderState(
                                kind = YoutubePlaceholderKind.EMPTY_FEED,
                                action = YoutubePlaceholderAction.RETRY
                            )
                        )
                    } else {
                        YoutubeFeedUiState(videos = allVideos)
                    }
                )

                PreferenceUtil.youtubeFeedLastSyncAt = System.currentTimeMillis()
                PreferenceUtil.youtubeFeedSyncSignature = trackedSignature.orEmpty()
                PreferenceUtil.youtubeFeedLastFailureKind = ""

                if (resolvedFailures > 0 && allVideos.isNotEmpty()) {
                    _messages.emit(
                        YoutubePlaceholderState(
                            kind = YoutubePlaceholderKind.CHANNEL_RESOLUTION_ERROR,
                            action = YoutubePlaceholderAction.RETRY
                        )
                    )
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }
                PreferenceUtil.youtubeFeedLastFailureKind =
                    exception.toPlaceholderState().kind.name
                PreferenceUtil.youtubeFeedSyncSignature = trackedSignature.orEmpty()
                handleFeedFailure(
                    throwable = exception,
                    cachedVideos = cachedVideosSnapshot.ifEmpty { cachedVideos }
                )
            }
        }
    }

    fun insertYoutubeChannel(channel: YoutubeChannelEntity) {
        viewModelScope.launch(IO) {
            repository.insertYoutubeChannel(channel)
        }
    }

    fun updateYoutubeChannel(channel: YoutubeChannelEntity) {
        viewModelScope.launch(IO) {
            repository.updateYoutubeChannel(channel)
        }
    }

    fun deleteYoutubeChannel(channel: YoutubeChannelEntity) {
        viewModelScope.launch(IO) {
            repository.deleteYoutubeChannel(channel)
        }
    }

    private suspend fun handleFeedFailure(
        throwable: Throwable,
        cachedVideos: List<YoutubeFeedVideo>
    ) {
        val placeholder = throwable.toPlaceholderState()
        if (cachedVideos.isNotEmpty()) {
            _uiState.postValue(YoutubeFeedUiState(videos = cachedVideos))
            _messages.emit(placeholder)
        } else {
            _uiState.postValue(YoutubeFeedUiState(placeholder = placeholder))
        }
    }

    private fun shouldAutoRefresh(cachedVideos: List<YoutubeFeedVideo>): Boolean {
        if (trackedChannels.isEmpty()) return false

        val now = System.currentTimeMillis()
        recentFailureKind()?.let { failureKind ->
            val elapsedSinceAttempt = now - PreferenceUtil.youtubeFeedLastAttemptAt
            if (elapsedSinceAttempt in 1 until failureCooldownMs(failureKind)) {
                return false
            }
        }

        if (PreferenceUtil.youtubeFeedSyncSignature != trackedSignature.orEmpty()) {
            return true
        }

        if (cachedVideos.isEmpty()) {
            return true
        }

        val elapsedSinceSync = now - PreferenceUtil.youtubeFeedLastSyncAt
        return elapsedSinceSync >= freshnessWindowMs()
    }

    private fun applyDeferredState(cachedVideos: List<YoutubeFeedVideo>) {
        if (cachedVideos.isNotEmpty()) {
            val currentState = _uiState.value ?: YoutubeFeedUiState()
            _uiState.value = currentState.copy(
                videos = cachedVideos,
                isInitialLoading = false,
                isRefreshing = false,
                placeholder = null
            )
            return
        }

        recentFailureKind()?.toStoredPlaceholderState()?.let { placeholder ->
            _uiState.value = YoutubeFeedUiState(placeholder = placeholder)
        }
    }

    private fun recentFailureKind(): YoutubePlaceholderKind? {
        val rawKind = PreferenceUtil.youtubeFeedLastFailureKind
        return runCatching { YoutubePlaceholderKind.valueOf(rawKind) }.getOrNull()
    }

    private fun YoutubePlaceholderKind.toStoredPlaceholderState(): YoutubePlaceholderState {
        return when (this) {
            YoutubePlaceholderKind.NO_CHANNELS -> YoutubePlaceholderState(
                kind = this,
                action = YoutubePlaceholderAction.ADD_CHANNEL
            )

            YoutubePlaceholderKind.EMPTY_FEED,
            YoutubePlaceholderKind.NETWORK_ERROR,
            YoutubePlaceholderKind.CHANNEL_RESOLUTION_ERROR,
            YoutubePlaceholderKind.SERVICE_ERROR,
            YoutubePlaceholderKind.UNKNOWN_ERROR -> YoutubePlaceholderState(
                kind = this,
                action = YoutubePlaceholderAction.RETRY
            )

            YoutubePlaceholderKind.QUOTA_EXCEEDED,
            YoutubePlaceholderKind.CONFIGURATION_ERROR -> YoutubePlaceholderState(kind = this)
        }
    }

    private fun failureCooldownMs(kind: YoutubePlaceholderKind): Long {
        return when (kind) {
            YoutubePlaceholderKind.QUOTA_EXCEEDED -> QUOTA_RETRY_COOLDOWN_MS
            YoutubePlaceholderKind.NETWORK_ERROR -> NETWORK_RETRY_COOLDOWN_MS
            YoutubePlaceholderKind.CHANNEL_RESOLUTION_ERROR -> RESOLUTION_RETRY_COOLDOWN_MS
            YoutubePlaceholderKind.SERVICE_ERROR,
            YoutubePlaceholderKind.UNKNOWN_ERROR -> ERROR_RETRY_COOLDOWN_MS
            YoutubePlaceholderKind.NO_CHANNELS,
            YoutubePlaceholderKind.EMPTY_FEED,
            YoutubePlaceholderKind.CONFIGURATION_ERROR -> DEFAULT_RETRY_COOLDOWN_MS
        }
    }

    private fun freshnessWindowMs(): Long {
        val appContext = App.getContext()
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val powerManager =
            appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSaveEnabled = powerManager?.isPowerSaveMode == true
        val isMetered = connectivityManager?.isActiveNetworkMetered == true

        return when {
            isPowerSaveEnabled || PreferenceUtil.batterySaverMode -> BATTERY_SAVER_REFRESH_WINDOW_MS
            isMetered -> METERED_REFRESH_WINDOW_MS
            else -> DEFAULT_REFRESH_WINDOW_MS
        }
    }

    private fun subscribeToCachedVideos(channels: List<YoutubeChannelEntity>) {
        cachedVideoSource?.removeObserver(cachedVideoObserver)
        cachedVideosSnapshot = emptyList()

        val sourceChannelIds = channels.map(YoutubeChannelEntity::id)
        if (sourceChannelIds.isEmpty()) {
            cachedVideoSource = null
            return
        }

        repository.getCachedYoutubeVideos(sourceChannelIds).also { source ->
            cachedVideoSource = source
            source.observeForever(cachedVideoObserver)
        }
    }

    private suspend fun resolveChannelId(channel: YoutubeChannelEntity): String? {
        knownChannelIds[channel.name.normalizedKey()]?.let { return it }

        val rawUrl = channel.url.trim()
        CHANNEL_ID_PATTERN.find(rawUrl)?.value?.let { return it }

        val uri = rawUrl.takeIf { it.isNotBlank() }?.let(Uri::parse)
        val pathSegments = uri?.pathSegments.orEmpty().filter { it.isNotBlank() }
        val queryTerm = uri?.getQueryParameter("search_query")

        val aliasCandidates = buildList {
            add(channel.name)
            add(queryTerm)
            add(pathSegments.lastOrNull())
            if (pathSegments.firstOrNull().equals("channel", ignoreCase = true)) {
                add(pathSegments.getOrNull(1))
            }
            if (pathSegments.firstOrNull().equals("user", ignoreCase = true)) {
                add(pathSegments.getOrNull(1))
            }
            if (pathSegments.firstOrNull().equals("c", ignoreCase = true)) {
                add(pathSegments.getOrNull(1))
            }
        }

        aliasCandidates
            .flatMap(::aliasVariants)
            .firstNotNullOfOrNull(knownChannelIds::get)
            ?.let { return it }

        val lastSegment = pathSegments.lastOrNull()?.removePrefix("@")
        return when {
            pathSegments.firstOrNull().equals("channel", ignoreCase = true) ->
                pathSegments.getOrNull(1)?.takeIf { CHANNEL_ID_PATTERN.matches(it) }

            pathSegments.firstOrNull().equals("user", ignoreCase = true) ->
                resolveChannelIdByUsername(pathSegments.getOrNull(1))

            pathSegments.firstOrNull().equals("c", ignoreCase = true) ->
                resolveChannelIdByLookup(pathSegments.getOrNull(1))

            rawUrl.startsWith("@") ->
                resolveChannelIdByHandle(rawUrl.removePrefix("@"))

            lastSegment != null ->
                resolveChannelIdByLookup(lastSegment)

            else -> null
        }
    }

    private suspend fun resolveChannelIdByLookup(candidate: String?): String? {
        val normalized = candidate?.trim().orEmpty()
        if (normalized.isBlank()) return null

        resolveChannelIdByHandle(normalized)?.let { return it }
        resolveChannelIdByUsername(normalized)?.let { return it }
        return null
    }

    private suspend fun resolveChannelIdByHandle(handle: String?): String? {
        val cleaned = handle?.trim()?.removePrefix("@").orEmpty()
        if (cleaned.isBlank()) return null
        return apiService.getChannelDetails(
            forHandle = cleaned,
            apiKey = BuildConfig.YOUTUBE_API_KEY
        ).items?.firstOrNull()?.id
    }

    private suspend fun resolveChannelIdByUsername(username: String?): String? {
        val cleaned = username?.trim().orEmpty()
        if (cleaned.isBlank()) return null
        return apiService.getChannelDetails(
            forUsername = cleaned,
            apiKey = BuildConfig.YOUTUBE_API_KEY
        ).items?.firstOrNull()?.id
    }

    private fun aliasVariants(value: String?): List<String> {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return emptyList()

        val normalized = raw
            .lowercase(Locale.US)
            .replace("@", "")
            .replace("official", "")
            .replace("tv", " television ")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

        return buildList {
            add(raw.normalizedKey())
            if (normalized.isNotBlank()) {
                add(normalized.replace(" ", ""))
                add(normalized.replace(" ", "_"))
                add(normalized)
            }
        }.distinct()
    }

    private fun String.normalizedKey(): String {
        return lowercase(Locale.US)
            .replace("@", "")
            .replace("official", "")
            .replace(Regex("[^a-z0-9]+"), "")
    }

    private fun Throwable.toPlaceholderState(): YoutubePlaceholderState {
        return when (this) {
            YoutubeFeedException.Configuration -> YoutubePlaceholderState(
                kind = YoutubePlaceholderKind.CONFIGURATION_ERROR
            )

            YoutubeFeedException.ChannelResolution -> YoutubePlaceholderState(
                kind = YoutubePlaceholderKind.CHANNEL_RESOLUTION_ERROR,
                action = YoutubePlaceholderAction.RETRY
            )

            is HttpException -> {
                val errorBody = response()?.errorBody()?.string().orEmpty()
                val reason = runCatching {
                    JSONObject(errorBody)
                        .optJSONObject("error")
                        ?.optJSONArray("errors")
                        ?.optJSONObject(0)
                        ?.optString("reason")
                }.getOrNull().orEmpty()

                if (code() == 403 && reason.contains("quota", ignoreCase = true)) {
                    YoutubePlaceholderState(
                        kind = YoutubePlaceholderKind.QUOTA_EXCEEDED
                    )
                } else {
                    YoutubePlaceholderState(
                        kind = YoutubePlaceholderKind.SERVICE_ERROR,
                        action = YoutubePlaceholderAction.RETRY,
                        detail = code().toString()
                    )
                }
            }

            is SocketTimeoutException,
            is UnknownHostException,
            is IOException -> YoutubePlaceholderState(
                kind = YoutubePlaceholderKind.NETWORK_ERROR,
                action = YoutubePlaceholderAction.RETRY
            )

            else -> YoutubePlaceholderState(
                kind = YoutubePlaceholderKind.UNKNOWN_ERROR,
                action = YoutubePlaceholderAction.RETRY
            )
        }
    }

    override fun onCleared() {
        cachedVideoSource?.removeObserver(cachedVideoObserver)
        super.onCleared()
    }

    private sealed class YoutubeFeedException : IllegalStateException() {
        data object Configuration : YoutubeFeedException()
        data object ChannelResolution : YoutubeFeedException()
    }

    companion object {
        private val CHANNEL_ID_PATTERN = Regex("UC[\\w-]{22}")
        private const val DEFAULT_REFRESH_WINDOW_MS = 2 * 60 * 60 * 1000L
        private const val METERED_REFRESH_WINDOW_MS = 4 * 60 * 60 * 1000L
        private const val BATTERY_SAVER_REFRESH_WINDOW_MS = 6 * 60 * 60 * 1000L
        private const val DEFAULT_RETRY_COOLDOWN_MS = 30 * 60 * 1000L
        private const val NETWORK_RETRY_COOLDOWN_MS = 20 * 60 * 1000L
        private const val ERROR_RETRY_COOLDOWN_MS = 60 * 60 * 1000L
        private const val RESOLUTION_RETRY_COOLDOWN_MS = 2 * 60 * 60 * 1000L
        private const val QUOTA_RETRY_COOLDOWN_MS = 6 * 60 * 60 * 1000L

        private val knownChannelIds = mapOf(
            "godtv" to "UCaH2G2F_Z_1dXXr5e8i9DNg",
            "daystartelevision" to "UCCo-sL38y_i094x16V8y9ew",
            "daystartv" to "UCCo-sL38y_i094x16V8y9ew",
            "tbn" to "UC6_rP2u9H4HntO8Pq9n8JBg",
            "thechosen" to "UCBXOFnNTULFaAnj24PAeblg"
        )
    }
}
