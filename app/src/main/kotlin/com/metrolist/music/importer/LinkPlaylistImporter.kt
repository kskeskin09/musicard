/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.util.UUID

/** The services a pasted link can come from. */
enum class LinkService(val displayName: String) {
    SPOTIFY("Spotify"),
    APPLE_MUSIC("Apple Music"),
    DEEZER("Deezer"),
}

class LinkImportException(val reason: Reason) : Exception(reason.name) {
    enum class Reason { NOT_A_LINK, NOT_FOUND, EMPTY, INCOMPLETE }
}

/**
 * Reads the song list of a public playlist or album from a share link, the way Metrolist does:
 * - Spotify: the public embed page (first 100 songs), then Spotify's own web player API with the
 *   anonymous token from that page for the rest. No account is needed.
 * - Deezer: Deezer's public API (api.deezer.com).
 * - Apple Music: Apple's catalog API with the public token the music.apple.com web player uses.
 * Only titles and artists are read; the songs are then looked up on YouTube Music.
 */
object LinkPlaylistImporter {
    const val MAX_TRACKS = 5_000
    private const val UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val http by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
            }
            expectSuccess = false
        }
    }

    fun detect(input: String): LinkService? {
        val lower = input.trim().lowercase()
        return when {
            "spotify.com" in lower || lower.startsWith("spotify:") -> LinkService.SPOTIFY
            "music.apple.com" in lower -> LinkService.APPLE_MUSIC
            "deezer.com" in lower || "deezer.page.link" in lower || "link.deezer.com" in lower -> LinkService.DEEZER
            else -> null
        }
    }

    /** Finds the first link in shared text such as "Listen to X on Spotify: https://…". */
    fun extractLink(text: String): String =
        Regex("""https?://\S+""").find(text)?.value?.trimEnd('.', ',', ')') ?: text.trim()

    suspend fun load(input: String, defaultCountry: String, onProgress: (Int) -> Unit = {}): ImportPlaylist {
        val link = extractLink(input)
        val playlist = when (detect(link)) {
            LinkService.SPOTIFY -> Spotify.load(link, onProgress)
            LinkService.APPLE_MUSIC -> Apple.load(link, defaultCountry, onProgress)
            LinkService.DEEZER -> Deezer.load(link, onProgress)
            null -> throw LinkImportException(LinkImportException.Reason.NOT_A_LINK)
        }
        if (playlist.tracks.isEmpty()) throw LinkImportException(LinkImportException.Reason.EMPTY)
        return playlist
    }

    // --- JSON helpers -------------------------------------------------------------------------

    private fun JsonElement?.obj(key: String): JsonObject? = (this as? JsonObject)?.get(key) as? JsonObject
    private fun JsonElement?.arr(key: String): JsonArray? = (this as? JsonObject)?.get(key) as? JsonArray
    private fun JsonElement?.str(key: String): String? = ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.contentOrNull

    private suspend fun HttpResponse.textOrNull(): String? = if (status.isSuccess()) bodyAsText() else null

    private fun splitArtists(text: String): List<String> =
        text.replace(' ', ' ').split(", ", " & ").map(String::trim).filter(String::isNotEmpty)

    private fun track(title: String?, artists: List<String>, album: String? = null): ImportTrack? =
        title?.trim()?.takeIf { it.isNotEmpty() }?.let { ImportTrack(it, artists, album) }

    // --- Spotify ------------------------------------------------------------------------------

    private object Spotify {
        private const val CLIENT_TOKEN_URL = "https://clienttoken.spotify.com/v1/clienttoken"
        private const val PATHFINDER_URL = "https://api-partner.spotify.com/pathfinder/v2/query"
        private const val WEB_CLIENT_ID = "d8a5ed958d274c2e8ee717e6a4b0971d"
        private const val APP_VERSION = "1.2.96.390.gd46a85db"
        private const val PLAYLIST_CONTENTS_HASH = "e4b2953f160e58e38ac025d79b5a9b3aceee5c4c716598e9830bfceb69faff5f"
        private const val PAGE_SIZE = 50

        private val tokenLock = Mutex()
        private var clientToken: String? = null
        private var clientTokenExpiresAt = 0L

        private fun parse(input: String): Pair<String, String>? {
            Regex("""spotify\.com/(?:[a-zA-Z0-9_-]+/)?(playlist|album)/([a-zA-Z0-9]+)""").find(input)
                ?.let { return it.groupValues[1] to it.groupValues[2] }
            Regex("""spotify:(playlist|album):([a-zA-Z0-9]+)""").find(input)
                ?.let { return it.groupValues[1] to it.groupValues[2] }
            return null
        }

        suspend fun load(input: String, onProgress: (Int) -> Unit): ImportPlaylist {
            val resolved = if ("spotify.link" in input || "spotify.app.link" in input) resolveShortLink(input) else input
            val (type, id) = parse(resolved) ?: throw LinkImportException(LinkImportException.Reason.NOT_A_LINK)
            val html = http.get("https://open.spotify.com/embed/$type/$id") {
                header(HttpHeaders.UserAgent, UA)
                header(HttpHeaders.Accept, "text/html")
            }.textOrNull() ?: throw LinkImportException(LinkImportException.Reason.NOT_FOUND)
            val nextData = Regex("""<script id="__NEXT_DATA__"[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
                .find(html)?.groupValues?.get(1) ?: throw LinkImportException(LinkImportException.Reason.NOT_FOUND)
            val root = json.parseToJsonElement(nextData)
            val state = root.obj("props").obj("pageProps").obj("state")
            val entity = state.obj("data").obj("entity") ?: throw LinkImportException(LinkImportException.Reason.NOT_FOUND)
            val title = entity.str("name") ?: entity.str("title") ?: "Spotify"
            val trackList = entity.arr("trackList") ?: JsonArray(emptyList())
            val tracks = trackList.mapNotNull { item ->
                track(item.str("title"), splitArtists(item.str("subtitle").orEmpty()), if (type == "album") title else null)
            }.toMutableList()
            onProgress(tracks.size)
            // The embed lists at most 100 songs; ask the web player API for the rest.
            if (type == "playlist" && trackList.size >= 100) {
                val accessToken = state.obj("settings").obj("session").str("accessToken")
                    ?: throw LinkImportException(LinkImportException.Reason.INCOMPLETE)
                loadRest(id, accessToken, trackList.size, tracks, onProgress)
            }
            return ImportPlaylist(title, tracks)
        }

        private suspend fun resolveShortLink(input: String): String {
            val response = http.get(input) { header(HttpHeaders.UserAgent, UA) }
            val finalUrl = response.call.request.url.toString()
            if ("open.spotify.com" in finalUrl) return finalUrl
            // Some short links answer with an HTML page that points to the real one.
            return Regex("""https://open\.spotify\.com/[^"'\s<>]+""").find(response.bodyAsText())?.value ?: finalUrl
        }

        private suspend fun clientToken(): String? = tokenLock.withLock {
            val now = System.currentTimeMillis()
            clientToken?.takeIf { now < clientTokenExpiresAt }?.let { return@withLock it }
            val payload = buildJsonObject {
                put("client_data", buildJsonObject {
                    put("client_version", APP_VERSION)
                    put("client_id", WEB_CLIENT_ID)
                    put("js_sdk_data", buildJsonObject {
                        put("device_brand", "unknown")
                        put("device_model", "unknown")
                        put("os", "android")
                        put("os_version", "unknown")
                        put("device_id", UUID.randomUUID().toString())
                        put("device_type", "computer")
                    })
                })
            }
            val body = runCatching {
                http.post(CLIENT_TOKEN_URL) {
                    header(HttpHeaders.UserAgent, UA)
                    header(HttpHeaders.Accept, "application/json")
                    header(HttpHeaders.Origin, "https://open.spotify.com")
                    header(HttpHeaders.Referrer, "https://open.spotify.com/")
                    contentType(ContentType.Application.Json)
                    setBody(payload.toString())
                }.textOrNull()
            }.getOrNull() ?: return@withLock null
            val granted = json.parseToJsonElement(body).obj("granted_token") ?: return@withLock null
            val token = granted.str("token") ?: return@withLock null
            val expiresSec = granted.str("expires_after_seconds")?.toLongOrNull() ?: 1_209_600L
            clientToken = token
            clientTokenExpiresAt = now + expiresSec * 1000 - 60_000
            token
        }

        private suspend fun loadRest(
            playlistId: String,
            accessToken: String,
            startOffset: Int,
            tracks: MutableList<ImportTrack>,
            onProgress: (Int) -> Unit,
        ) {
            val token = clientToken() ?: throw LinkImportException(LinkImportException.Reason.INCOMPLETE)
            var offset = startOffset
            var retries = 0
            while (tracks.size < MAX_TRACKS) {
                val request = buildJsonObject {
                    put("operationName", "fetchPlaylistContents")
                    put("variables", buildJsonObject {
                        put("uri", "spotify:playlist:$playlistId")
                        put("offset", offset)
                        put("limit", PAGE_SIZE)
                        put("includeEpisodeContentRatingsV2", true)
                    })
                    put("extensions", buildJsonObject {
                        put("persistedQuery", buildJsonObject {
                            put("version", 1)
                            put("sha256Hash", PLAYLIST_CONTENTS_HASH)
                        })
                    })
                }
                val response = http.post(PATHFINDER_URL) {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    header("client-token", token)
                    header(HttpHeaders.Accept, "application/json")
                    header(HttpHeaders.Origin, "https://open.spotify.com")
                    header(HttpHeaders.Referrer, "https://open.spotify.com/")
                    header("app-platform", "WebPlayer")
                    header("spotify-app-version", APP_VERSION)
                    header(HttpHeaders.UserAgent, UA)
                    contentType(ContentType.Application.Json)
                    setBody(request.toString())
                }
                if (response.status.value == 429) {
                    val wait = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull() ?: 2L
                    if (wait in 1..5 && retries++ < 2) {
                        delay(wait * 1000)
                        continue
                    }
                    throw LinkImportException(LinkImportException.Reason.INCOMPLETE)
                }
                retries = 0
                val body = response.textOrNull() ?: throw LinkImportException(LinkImportException.Reason.INCOMPLETE)
                val content = json.parseToJsonElement(body).obj("data").obj("playlistV2").obj("content")
                val items = content.arr("items") ?: throw LinkImportException(LinkImportException.Reason.INCOMPLETE)
                val total = content.str("totalCount")?.toIntOrNull() ?: throw LinkImportException(LinkImportException.Reason.INCOMPLETE)
                if (offset >= total) return
                if (items.isEmpty()) throw LinkImportException(LinkImportException.Reason.INCOMPLETE)
                for (entry in items) {
                    val wrapper = entry.obj("itemV2") ?: entry.obj("track") ?: (entry as? JsonObject) ?: continue
                    val data = wrapper.obj("data") ?: wrapper
                    val uri = data.str("uri") ?: wrapper.str("_uri") ?: wrapper.str("uri").orEmpty()
                    if (uri.startsWith("spotify:local:")) continue
                    val artists = data.obj("artists").arr("items").orEmpty().mapNotNull { it.obj("profile").str("name") }
                    track(data.str("name"), artists.flatMap(::splitArtists), data.obj("albumOfTrack").str("name"))?.let { tracks += it }
                    if (tracks.size >= MAX_TRACKS) break
                }
                onProgress(tracks.size)
                offset += items.size
                if (offset >= total) return
            }
        }
    }

    // --- Deezer -------------------------------------------------------------------------------

    private object Deezer {
        suspend fun load(input: String, onProgress: (Int) -> Unit): ImportPlaylist {
            var link = input.trim()
            if ("deezer.page.link" in link || "link.deezer.com" in link) {
                val response = http.get(link) { header(HttpHeaders.UserAgent, UA) }
                val redirected = response.call.request.url
                val path = redirected.encodedPath
                link = if (redirected.host.endsWith("deezer.com") && ("/playlist/" in path || "/album/" in path)) {
                    redirected.toString()
                } else {
                    Regex("""https://www\.deezer\.com/[^"'\s<>]+""").find(response.bodyAsText())?.value
                        ?: throw LinkImportException(LinkImportException.Reason.NOT_FOUND)
                }
            }
            Regex("""deezer\.com(?:/[a-z]{2})?/playlist/(\d+)""", RegexOption.IGNORE_CASE).find(link)?.let {
                return collection("playlist", it.groupValues[1], onProgress)
            }
            Regex("""deezer\.com(?:/[a-z]{2})?/album/(\d+)""", RegexOption.IGNORE_CASE).find(link)?.let {
                return collection("album", it.groupValues[1], onProgress)
            }
            throw LinkImportException(LinkImportException.Reason.NOT_A_LINK)
        }

        private suspend fun get(url: String): JsonObject {
            val parsed = Url(if (url.startsWith("/")) "https://api.deezer.com$url" else url)
            require(parsed.protocol.name == "https" && parsed.host == "api.deezer.com")
            val body = http.get(parsed) { header(HttpHeaders.UserAgent, UA) }.textOrNull()
                ?: throw LinkImportException(LinkImportException.Reason.NOT_FOUND)
            val root = json.parseToJsonElement(body) as? JsonObject
                ?: throw LinkImportException(LinkImportException.Reason.NOT_FOUND)
            if (root.containsKey("error")) throw LinkImportException(LinkImportException.Reason.NOT_FOUND)
            return root
        }

        private suspend fun collection(type: String, id: String, onProgress: (Int) -> Unit): ImportPlaylist {
            val meta = get("https://api.deezer.com/$type/$id")
            val title = meta.str("title") ?: "Deezer"
            val albumArtist = meta.obj("artist").str("name").orEmpty()
            val tracks = mutableListOf<ImportTrack>()
            var next: String? = "https://api.deezer.com/$type/$id/tracks?limit=100"
            val visited = HashSet<String>()
            while (next != null && tracks.size < MAX_TRACKS && visited.add(next)) {
                val page = get(next)
                page.arr("data").orEmpty().forEach { item ->
                    val artist = item.obj("artist").str("name") ?: albumArtist
                    track(
                        item.str("title"),
                        listOf(artist).filter(String::isNotBlank),
                        if (type == "album") title else item.obj("album").str("title"),
                    )?.let { tracks += it }
                }
                onProgress(tracks.size)
                next = page.str("next")
            }
            return ImportPlaylist(title, tracks)
        }
    }

    // --- Apple Music --------------------------------------------------------------------------

    private object Apple {
        private val tokenLock = Mutex()
        private var token: String? = null
        private var tokenExpiresAt = 0L

        suspend fun load(input: String, defaultCountry: String, onProgress: (Int) -> Unit): ImportPlaylist {
            val playlist = Regex("""music\.apple\.com(?:/([a-z]{2}))?/playlist/(?:[^/]+/)?(pl\.[a-zA-Z0-9_-]+)""", RegexOption.IGNORE_CASE)
                .find(input)
            val album = Regex("""music\.apple\.com(?:/([a-z]{2}))?/album/(?:[^/]+/)?(\d+)""", RegexOption.IGNORE_CASE)
                .find(input)
            val match = playlist ?: album ?: throw LinkImportException(LinkImportException.Reason.NOT_A_LINK)
            val storefront = (match.groups[1]?.value ?: defaultCountry).lowercase().ifBlank { "us" }
            val id = match.groupValues[2]
            val devToken = devToken() ?: throw LinkImportException(LinkImportException.Reason.NOT_FOUND)
            val kind = if (playlist != null) "playlists" else "albums"

            val meta = get("https://api.music.apple.com/v1/catalog/$storefront/$kind/$id", devToken)
            val item = meta.arr("data")?.firstOrNull() as? JsonObject ?: throw LinkImportException(LinkImportException.Reason.NOT_FOUND)
            val title = item.obj("attributes").str("name") ?: "Apple Music"
            val albumArtist = item.obj("attributes").str("artistName").orEmpty()
            val tracks = mutableListOf<ImportTrack>()
            var next: String? = "https://api.music.apple.com/v1/catalog/$storefront/$kind/$id/tracks?limit=100"
            val visited = HashSet<String>()
            while (next != null && tracks.size < MAX_TRACKS && visited.add(next)) {
                val page = get(next, devToken)
                page.arr("data").orEmpty().forEach { entry ->
                    val attrs = entry.obj("attributes")
                    track(
                        attrs.str("name"),
                        splitArtists(attrs.str("artistName") ?: albumArtist),
                        attrs.str("albumName"),
                    )?.let { tracks += it }
                }
                onProgress(tracks.size)
                next = page.str("next")
            }
            return ImportPlaylist(title, tracks)
        }

        private suspend fun get(url: String, devToken: String): JsonObject {
            val full = if (url.startsWith("/")) "https://api.music.apple.com$url" else url
            require(Url(full).host == "api.music.apple.com")
            val body = http.get(full) {
                header(HttpHeaders.Authorization, "Bearer $devToken")
                header(HttpHeaders.Origin, "https://music.apple.com")
                header(HttpHeaders.UserAgent, UA)
            }.textOrNull() ?: throw LinkImportException(LinkImportException.Reason.NOT_FOUND)
            return json.parseToJsonElement(body) as? JsonObject ?: throw LinkImportException(LinkImportException.Reason.NOT_FOUND)
        }

        /** The public token music.apple.com ships in its web player script. */
        private suspend fun devToken(): String? = tokenLock.withLock {
            val now = System.currentTimeMillis() / 1000
            token?.takeIf { tokenExpiresAt - now > 300 }?.let { return@withLock it }
            val html = runCatching {
                http.get("https://music.apple.com/browse") {
                    header(HttpHeaders.UserAgent, UA)
                    header(HttpHeaders.Accept, "text/html")
                }.textOrNull()
            }.getOrNull() ?: return@withLock null
            val bundle = Regex("""/assets/index[^"'\s]*\.js""").find(html)?.value ?: return@withLock null
            val js = runCatching {
                http.get("https://music.apple.com$bundle") { header(HttpHeaders.UserAgent, UA) }.textOrNull()
            }.getOrNull() ?: return@withLock null
            val jwt = Regex("""eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""").find(js)?.value ?: return@withLock null
            val exp = runCatching {
                val payload = String(java.util.Base64.getUrlDecoder().decode(jwt.split('.')[1]))
                json.parseToJsonElement(payload).str("exp")?.toLongOrNull()
            }.getOrNull() ?: (now + 86_400)
            token = jwt
            tokenExpiresAt = exp
            jwt
        }
    }
}
