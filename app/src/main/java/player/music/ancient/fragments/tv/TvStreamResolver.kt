package player.music.ancient.fragments.tv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL

internal sealed interface TvLaunchTarget

internal data class NativeTvLaunchTarget(
    val streamUrl: String,
    val referer: String? = null,
    val origin: String? = null,
    val userAgent: String = USER_AGENT,
) : TvLaunchTarget

internal data class ExternalTvLaunchTarget(
    val url: String,
) : TvLaunchTarget

internal object TvStreamResolver {
    private const val MAX_DEPTH = 2

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun resolve(url: String): TvLaunchTarget = withContext(Dispatchers.IO) {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) {
            ExternalTvLaunchTarget(url)
        } else {
            resolveInternal(normalizedUrl, 0)
        }
    }

    private fun resolveInternal(url: String, depth: Int): TvLaunchTarget {
        if (isDirectPlayableUrl(url)) {
            return NativeTvLaunchTarget(streamUrl = url)
        }
        if (depth > MAX_DEPTH) {
            return ExternalTvLaunchTarget(url)
        }

        val document = fetchBody(url) ?: return ExternalTvLaunchTarget(url)

        extractDirectVideoUrl(document)?.let { directUrl ->
            return NativeTvLaunchTarget(streamUrl = directUrl, referer = url, origin = originOf(url))
        }

        extractLightcastConfigUrl(document, url)?.let { configUrl ->
            resolveLightcastConfig(configUrl, url)?.let { return it }
        }

        extractIframeUrl(document, url)?.takeIf { it != url }?.let { iframeUrl ->
            return resolveInternal(iframeUrl, depth + 1)
        }

        return ExternalTvLaunchTarget(url)
    }

    private fun resolveLightcastConfig(configUrl: String, refererUrl: String): NativeTvLaunchTarget? {
        val configBody = fetchBody(
            url = configUrl,
            headers = mapOf(
                "Referer" to refererUrl,
                "Origin" to originOf(refererUrl),
            )
        ) ?: return null

        val streamUrl = DIRECT_VIDEO_REGEX.find(configBody)?.value?.let(::decodeJsUrl)
            ?: return null

        return NativeTvLaunchTarget(
            streamUrl = streamUrl,
            referer = refererUrl,
            origin = originOf(refererUrl),
        )
    }

    private fun fetchBody(url: String, headers: Map<String, String> = emptyMap()): String? {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .apply {
                    headers.forEach { (name, value) ->
                        if (value.isNotBlank()) {
                            header(name, value)
                        }
                    }
                }
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return null
                }
                response.body?.string()
            }
        }.getOrNull()
    }

    private fun extractLightcastConfigUrl(document: String, baseUrl: String): String? {
        val relativeOrAbsolute = LIGHTCAST_CONFIG_REGEX.find(document)?.groupValues?.get(1)
            ?: return null
        return resolveUrl(baseUrl, decodeJsUrl(relativeOrAbsolute))
    }

    private fun extractIframeUrl(document: String, baseUrl: String): String? {
        val iframeUrl = IFRAME_REGEX.find(document)?.groupValues?.get(1)
            ?: return null
        return resolveUrl(baseUrl, decodeJsUrl(iframeUrl))
    }

    private fun extractDirectVideoUrl(document: String): String? {
        return DIRECT_VIDEO_REGEX.find(document)?.value?.let(::decodeJsUrl)
    }

    private fun isDirectPlayableUrl(url: String): Boolean {
        return DIRECT_EXTENSION_REGEX.containsMatchIn(url)
    }

    private fun resolveUrl(baseUrl: String, targetUrl: String): String {
        return runCatching { URL(URL(baseUrl), targetUrl).toString() }
            .getOrElse { targetUrl }
    }

    private fun originOf(url: String): String {
        return runCatching {
            val parsed = URL(url)
            "${parsed.protocol}://${parsed.host}" +
                if (parsed.port > 0 && parsed.port != parsed.defaultPort) ":${parsed.port}" else ""
        }.getOrDefault(url)
    }

    private fun decodeJsUrl(value: String): String {
        return value
            .replace("\\/", "/")
            .replace("\\u0026", "&")
            .replace("&amp;", "&")
    }

    private val DIRECT_EXTENSION_REGEX =
        Regex("""(?i)\.(m3u8|mp4|webm|mpd)(\?.*)?$""")
    private val DIRECT_VIDEO_REGEX =
        Regex("""https?://[^"'\\s]+?\.(?:m3u8|mp4|webm|mpd)[^"'\\s]*""", RegexOption.IGNORE_CASE)
    private val LIGHTCAST_CONFIG_REGEX =
        Regex("""configUrl\s*[:=]\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val IFRAME_REGEX =
        Regex("""<iframe[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
}

private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
