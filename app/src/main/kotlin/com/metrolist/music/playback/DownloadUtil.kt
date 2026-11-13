/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.datastore.preferences.core.edit
import com.metrolist.music.constants.AutoDownloadLibraryKey
import com.metrolist.music.constants.AutoDownloadWifiOnlyKey
import com.metrolist.music.constants.AutoDownloadedSongIdsKey
import com.metrolist.music.utils.BatteryOptimizationFollowSystemKey
import com.metrolist.music.utils.BatteryOptimizationKey
import com.metrolist.music.utils.BatteryOptimizationThresholdKey
import com.metrolist.music.utils.DEFAULT_BATTERY_OPTIMIZATION_THRESHOLD
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.isBatteryOptimizationActive
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.AudioQualityKey
import com.metrolist.music.constants.DownloadQuality
import com.metrolist.music.constants.DownloadQualityKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.AlbumEntity
import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.di.DownloadCache
import com.metrolist.music.di.PlayerCache
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.utils.InnerTubeXPlayer
import com.metrolist.music.utils.enumPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext private val context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: Cache,
    @PlayerCache val playerCache: Cache,
) {
    private val TAG = "DownloadUtil"
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val playingQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val downloadQuality by enumPreference(context, DownloadQualityKey, DownloadQuality.SAME_AS_PLAYING)

    // "Download quality" in Storage settings; "Same as playing quality" uses the player's setting.
    private val audioQuality: AudioQuality
        get() =
            when (downloadQuality) {
                DownloadQuality.SAME_AS_PLAYING -> playingQuality
                DownloadQuality.LOW -> AudioQuality.LOW
                DownloadQuality.HIGH -> AudioQuality.HIGH
            }
    private val songUrlCache = StreamUrlCache()
    private val streamHttpClient =
        OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .proxyAuthenticator { _, response ->
                YouTube.proxyAuth?.let { auth ->
                    response.request.newBuilder()
                        .header("Proxy-Authorization", auth)
                        .build()
                } ?: response.request
            }
            .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadPreparations = Semaphore(3)

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setCacheWriteDataSinkFactory(null)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(streamHttpClient),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val length = if (dataSpec.length >= 0) dataSpec.length else 1

            if (playerCache.isCached(mediaId, dataSpec.position, length)) {
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.let { cachedStream ->
                return@Factory dataSpec.withResolvedStream(cachedStream)
            }
            val cacheGeneration = songUrlCache.generation(mediaId)

            val playbackData = runBlocking(Dispatchers.IO) {
                val song = database.songEntity(mediaId)
                InnerTubeXPlayer.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                    contentHints = ContentHints(
                        isExplicit = song?.explicit,
                        isUploaded = song?.isUploaded,
                    ),
                    allowBoundedRange = false,
                )
            }.getOrThrow()
            val format = playbackData.format

            val actualContentLength =
                format.contentLength?.takeIf { it > 0L } ?: run {
                    val request = okhttp3.Request.Builder()
                        .get()
                        .url(playbackData.streamUrl)
                        .apply {
                            playbackData.streamHeaders.forEach { (name, value) ->
                                header(name, value)
                            }
                        }
                        .header("Range", "bytes=0-0")
                        .build()
                    try {
                        streamHttpClient.newCall(request).execute().use { response ->
                            downloadContentLength(
                                statusCode = response.code,
                                contentRange = response.header("Content-Range"),
                                contentLength = response.header("Content-Length"),
                            )
                        }
                    } catch (_: IOException) {
                        null
                    }
                }

            database.query {
                if (actualContentLength != null) {
                    upsert(
                        FormatEntity(
                            id = mediaId,
                            itag = format.itag,
                            mimeType = format.mimeType.substringBefore(";"),
                            codecs =
                                format.mimeType
                                    .substringAfter("codecs=", missingDelimiterValue = "")
                                    .substringBefore(";")
                                    .trim()
                                    .removeSurrounding("\""),
                            bitrate = format.bitrate,
                            sampleRate = format.audioSampleRate,
                            contentLength = actualContentLength,
                            loudnessDb = playbackData.audioConfig?.loudnessDb,
                            perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
                            playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                        ),
                    )
                } else {
                    deleteFormat(mediaId)
                }

                // Metadata registration only — dateDownload is intentionally NOT set here.
                // It belongs solely to onDownloadChanged()'s STATE_COMPLETED branch below,
                // which only fires once the download has actually finished. Setting it here
                // (at URL-resolve time, i.e. the moment the download merely *starts*) would
                // mark the song as "cached" before a single byte is written.
                val existing = getSongByIdBlocking(mediaId)?.song
                val updatedSong = existing ?: SongEntity(
                    id = mediaId,
                    title = playbackData.videoDetails?.title ?: "Unknown",
                    duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                    thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url,
                    dateDownload = null,
                    isDownloaded = false
                )

                upsert(updatedSong)
            }

            // googlevideo throttles unbounded requests to roughly playback speed; a range param covering
            // the whole file lets the download run at full speed.
            val streamUrl =
                if (actualContentLength != null && "&range=" !in playbackData.streamUrl) {
                    "${playbackData.streamUrl}&range=0-${actualContentLength - 1}"
                } else {
                    playbackData.streamUrl
                }

            songUrlCache.put(
                mediaId = mediaId,
                url = streamUrl,
                requestHeaders = playbackData.streamHeaders,
                clientName = playbackData.streamClient,
                expiresInSeconds = playbackData.streamExpiresInSeconds,
                requireBoundedRange = playbackData.requireBoundedRange,
                rangeChunkSizeBytes = playbackData.rangeChunkSizeBytes,
                useRangeChunks = playbackData.useRangeChunks,
                expectedGeneration = cacheGeneration,
            )
            dataSpec.withResolvedStream(
                CachedStreamUrl(
                    url = streamUrl,
                    requestHeaders = playbackData.streamHeaders,
                    clientName = playbackData.streamClient,
                    requireBoundedRange = playbackData.requireBoundedRange,
                    rangeChunkSizeBytes = playbackData.rangeChunkSizeBytes,
                    useRangeChunks = playbackData.useRangeChunks,
                ),
            )
        }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    @OptIn(DelicateCoroutinesApi::class)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run)
        ).apply {
            maxParallelDownloads = 3
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        if (download.state == Download.STATE_FAILED && finalException.isExpiredStreamError()) {
                            songUrlCache.invalidate(download.request.id)
                        }

                        downloads.update { map ->
                            map.toMutableMap().apply {
                                set(download.request.id, download)
                            }
                        }

                        scope.launch {
                            when (download.state) {
                                Download.STATE_COMPLETED -> {
                                    removeFromPlayerCache(download.request.id)
                                    database.updateDownloadedInfo(download.request.id, true, LocalDateTime.now())
                                }
                                Download.STATE_FAILED,
                                Download.STATE_STOPPED,
                                Download.STATE_REMOVING -> {
                                    database.updateDownloadedInfo(download.request.id, false, null)
                                }
                                else -> {
                                }
                            }
                        }
                    }

                    override fun onDownloadRemoved(
                        downloadManager: DownloadManager,
                        download: Download,
                    ) {
                        val downloadId = download.request.id
                        songUrlCache.invalidate(downloadId)

                        runCatching {
                            database.updateDownloadedInfo(downloadId, false, null)
                        }.onSuccess {
                            downloads.update { map ->
                                map.toMutableMap().apply {
                                    remove(downloadId)
                                }
                            }
                            Timber.tag(TAG).d("Successfully removed download $downloadId from in-memory map")
                        }.onFailure { error ->
                            Timber.tag(TAG).e(error, "Failed to update database for removed download $downloadId, keeping in-memory entry")
                        }
                    }
                }
            )
        }

    init {
        val result = mutableMapOf<String, Download>()
        downloadManager.downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.download.request.id] = cursor.download
            }
        }
        downloads.value = result
        scope.launch {
            result.values
                .filter { it.state == Download.STATE_COMPLETED }
                .forEach { removeFromPlayerCache(it.request.id) }
        }
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    fun download(song: Song) = download(song.toMediaMetadata())

    fun download(song: SongItem) = download(song.toMediaMetadata())

    fun download(mediaMetadata: MediaMetadata) = download(mediaMetadata, automatic = false)

    /**
     * [automatic] downloads come from "Automatically download library". They are remembered in
     * [AutoDownloadedSongIdsKey] so the Downloaded list only shows what the download button saved.
     */
    private fun download(mediaMetadata: MediaMetadata, automatic: Boolean) {
        scope.launch {
            try {
                prepareDownload(mediaMetadata, automatic)
            } finally {
                if (automatic) autoPreparing.remove(mediaMetadata.id)
            }
        }
    }

    private suspend fun prepareDownload(mediaMetadata: MediaMetadata, automatic: Boolean) {
            if (!automatic) markAutomaticDownloads(remove = setOf(mediaMetadata.id))
            downloadPreparations.withPermit {
                if (automatic && downloads.value[mediaMetadata.id] != null) return@withPermit
                if (!shouldPrepareDownload(downloads.value[mediaMetadata.id]?.state)) return@withPermit

                mediaMetadata.album?.let { album ->
                    if (database.albumEntity(album.id) == null) {
                        database.insert(
                            AlbumEntity(
                                id = album.id,
                                title = album.title,
                                thumbnailUrl = mediaMetadata.thumbnailUrl,
                                songCount = 0,
                                duration = 0,
                            ),
                        )
                    }
                }

                val existing = database.getSongByIdBlocking(mediaMetadata.id)
                if (existing == null) {
                    database.insert(mediaMetadata)
                } else {
                    database.update(
                        existing,
                        mediaMetadata,
                        overwriteTitle = false,
                        overwriteArtists = false,
                    )
                }

                if (!shouldPrepareDownload(downloadManager.downloadIndex.getDownload(mediaMetadata.id)?.state)) {
                    return@withPermit
                }
                if (automatic) {
                    // Another download (e.g. the button) may have started while this one waited.
                    if (downloadManager.downloadIndex.getDownload(mediaMetadata.id) != null) return@withPermit
                    markAutomaticDownloads(add = setOf(mediaMetadata.id))
                }

                val request =
                    DownloadRequest
                        .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                        .setCustomCacheKey(mediaMetadata.id)
                        .setData(mediaMetadata.title.toByteArray())
                        .build()
                DownloadService.sendAddDownload(
                    context,
                    ExoDownloadService::class.java,
                    request,
                    if (automatic && !autoDownloadAllowed.value) AUTO_DOWNLOAD_PAUSED else Download.STOP_REASON_NONE,
                    false,
                )

                val albumArtwork = database.getSongByIdBlocking(mediaMetadata.id)?.album?.thumbnailUrl
                downloadArtworkUrls(mediaMetadata.thumbnailUrl, albumArtwork).forEach { artworkUrl ->
                    runCatching {
                        context.imageLoader.execute(
                            ImageRequest
                                .Builder(context)
                                .data(artworkUrl)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .networkCachePolicy(CachePolicy.ENABLED)
                                .build(),
                        )
                    }
                }
            }
        }

    fun download(songId: String) {
        scope.launch {
            database.getSongByIdBlocking(songId)?.let { song ->
                download(song)
            }
        }
    }

    fun release() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        scope.cancel()
    }

    // ---- Automatically download library ----

    private val networkState = MutableStateFlow(currentNetworkState())
    private val autoDownloadAllowed = MutableStateFlow(false)

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networkState.value = currentNetworkState()
            }

            override fun onLost(network: Network) {
                networkState.value = currentNetworkState()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                networkState.value = currentNetworkState()
            }
        }

    // Songs whose automatic download is being prepared, so a re-check doesn't start them twice.
    private val autoPreparing: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    init {
        startLibraryAutoDownload()
    }

    private fun currentNetworkState(): AutoDownloadNetwork {
        val network = connectivityManager.activeNetwork ?: return AutoDownloadNetwork.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return AutoDownloadNetwork.NONE
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return AutoDownloadNetwork.NONE
        return if (connectivityManager.isActiveNetworkMetered) AutoDownloadNetwork.METERED else AutoDownloadNetwork.UNMETERED
    }

    private suspend fun markAutomaticDownloads(
        add: Set<String> = emptySet(),
        remove: Set<String> = emptySet(),
    ) {
        if (add.isEmpty() && remove.isEmpty()) return
        runCatching {
            context.dataStore.edit { prefs ->
                val current = prefs[AutoDownloadedSongIdsKey].orEmpty()
                val updated = (current - remove) + add
                if (updated != current) prefs[AutoDownloadedSongIdsKey] = updated
            }
        }.onFailure { Timber.tag(TAG).w(it, "Failed to update automatic download list") }
    }

    private data class AutoDownloadSettings(
        val enabled: Boolean,
        val wifiOnly: Boolean,
        val batteryOptimization: Boolean,
        val batteryThreshold: Int,
        val batteryFollowSystem: Boolean,
    )

    private fun batteryOptimizationActive(settings: AutoDownloadSettings) =
        isBatteryOptimizationActive(
            context,
            settings.batteryOptimization,
            settings.batteryThreshold,
            settings.batteryFollowSystem,
        )

    @OptIn(FlowPreview::class)
    private fun startLibraryAutoDownload() {
        runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }
            .onFailure { Timber.tag(TAG).w(it, "Failed to watch the network") }

        val settings =
            context.dataStore.data
                .map {
                    AutoDownloadSettings(
                        enabled = it[AutoDownloadLibraryKey] ?: false,
                        wifiOnly = it[AutoDownloadWifiOnlyKey] ?: true,
                        batteryOptimization = it[BatteryOptimizationKey] ?: false,
                        batteryThreshold = it[BatteryOptimizationThresholdKey] ?: DEFAULT_BATTERY_OPTIMIZATION_THRESHOLD,
                        batteryFollowSystem = it[BatteryOptimizationFollowSystemKey] ?: true,
                    )
                }.distinctUntilChanged()
        val autoIds = context.dataStore.data.map { it[AutoDownloadedSongIdsKey].orEmpty() }.distinctUntilChanged()
        // Battery level has no callback here, so look again every few minutes.
        val batteryTicker =
            flow {
                while (true) {
                    emit(Unit)
                    delay(BATTERY_CHECK_INTERVAL_MS)
                }
            }

        scope.launch {
            combine(
                settings,
                database.autoDownloadLibrarySongIds().map { it.toSet() }.distinctUntilChanged(),
                autoIds,
                networkState,
                downloads.map { map -> map.mapValues { it.value.state to it.value.stopReason } }.distinctUntilChanged(),
            ) { s, library, auto, network, states -> AutoDownloadInputs(s, library, auto, network, states) }
                .combine(batteryTicker) { inputs, _ -> inputs }
                .debounce(1_000)
                .collectLatest { inputs -> runCatching { applyLibraryAutoDownload(inputs) }
                    .onFailure { Timber.tag(TAG).e(it, "Automatic library download failed") } }
        }
    }

    private data class AutoDownloadInputs(
        val settings: AutoDownloadSettings,
        val library: Set<String>,
        val auto: Set<String>,
        val network: AutoDownloadNetwork,
        val states: Map<String, Pair<Int, Int>>,
    )

    private suspend fun applyLibraryAutoDownload(inputs: AutoDownloadInputs) {
        val (settings, library, auto, network, states) = inputs
        val allowed =
            settings.enabled &&
                network.allowsAutoDownload(wifiOnly = settings.wifiOnly) &&
                !batteryOptimizationActive(settings)
        autoDownloadAllowed.value = allowed

        val plan = planLibraryAutoDownload(
            enabled = settings.enabled,
            allowed = allowed,
            library = library,
            automatic = auto,
            states = states,
            pausedReason = AUTO_DOWNLOAD_PAUSED,
        )

        plan.remove.forEach { id ->
            DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, id, false)
        }
        markAutomaticDownloads(remove = plan.forget)
        if (plan.pause.isNotEmpty()) {
            // The download manager lives on the main thread; this avoids one service call per song.
            withContext(Dispatchers.Main) {
                plan.pause.forEach { id -> downloadManager.setStopReason(id, AUTO_DOWNLOAD_PAUSED) }
            }
        }
        plan.resume.forEach { id ->
            DownloadService.sendSetStopReason(context, ExoDownloadService::class.java, id, Download.STOP_REASON_NONE, false)
        }
        plan.start.filter(autoPreparing::add).forEach { id ->
            val song = database.getSongByIdBlocking(id)
            if (song != null) download(song.toMediaMetadata(), automatic = true) else autoPreparing.remove(id)
        }
    }

    private fun removeFromPlayerCache(songId: String) {
        runCatching { playerCache.removeResource(songId) }
            .onFailure { Timber.tag(TAG).w(it, "Failed to remove downloaded song $songId from player cache") }
    }

    private fun Throwable?.isExpiredStreamError(): Boolean {
        var current = this
        while (current != null) {
            if (current is HttpDataSource.InvalidResponseCodeException &&
                (current.responseCode == 403 || current.responseCode == 410 || current.responseCode == 416)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}

/** Stop reason for automatic library downloads waiting for Wi-Fi or for battery optimization to end. */
internal const val AUTO_DOWNLOAD_PAUSED = 0x4D43 // "MC"

private const val BATTERY_CHECK_INTERVAL_MS = 5 * 60 * 1000L

internal enum class AutoDownloadNetwork {
    NONE,
    METERED,
    UNMETERED,
    ;

    fun allowsAutoDownload(wifiOnly: Boolean) =
        when (this) {
            NONE -> false
            METERED -> !wifiOnly
            UNMETERED -> true
        }
}

internal data class LibraryAutoDownloadPlan(
    val start: List<String> = emptyList(),
    val pause: List<String> = emptyList(),
    val resume: List<String> = emptyList(),
    val remove: List<String> = emptyList(),
    val forget: Set<String> = emptySet(),
)

/**
 * Decides what "Automatically download library" does next.
 *
 * [states] maps a song id to its download state and stop reason. Only downloads this feature
 * started ([automatic]) are ever paused or removed; the download button's are left alone.
 */
internal fun planLibraryAutoDownload(
    enabled: Boolean,
    allowed: Boolean,
    library: Set<String>,
    automatic: Set<String>,
    states: Map<String, Pair<Int, Int>>,
    pausedReason: Int,
): LibraryAutoDownloadPlan {
    // Turned off: drop unfinished automatic downloads, keep the finished ones.
    // A song that left the library: its automatic download goes too. An empty library is more
    // likely a database that is still loading than one emptied by hand, so nothing goes then.
    val unfinished = { id: String -> states[id]?.first != Download.STATE_COMPLETED }
    val leftLibrary = { id: String -> library.isNotEmpty() && id !in library }
    val remove =
        automatic.filter { id ->
            states[id] != null && (leftLibrary(id) || (!enabled && unfinished(id)))
        }
    val forget =
        automatic.filter { id ->
            leftLibrary(id) || (!enabled && unfinished(id))
        }.toSet()
    if (!enabled) return LibraryAutoDownloadPlan(remove = remove, forget = forget)

    val kept = automatic - forget
    val active = setOf(Download.STATE_QUEUED, Download.STATE_DOWNLOADING, Download.STATE_RESTARTING)
    if (!allowed) {
        return LibraryAutoDownloadPlan(
            pause = kept.filter { states[it]?.first in active },
            remove = remove,
            forget = forget,
        )
    }
    return LibraryAutoDownloadPlan(
        start = library.filter { it !in states }.sorted(),
        resume = kept.filter { id -> states[id]?.let { it.first == Download.STATE_STOPPED && it.second == pausedReason } == true },
        remove = remove,
        forget = forget,
    )
}

internal fun shouldPrepareDownload(downloadState: Int?): Boolean = downloadState != Download.STATE_COMPLETED

internal fun downloadArtworkUrls(
    songArtwork: String?,
    albumArtwork: String?,
): List<String> = listOfNotNull(songArtwork, albumArtwork).filter(String::isNotBlank).distinct()

internal fun downloadContentLength(
    statusCode: Int,
    contentRange: String?,
    contentLength: String?,
): Long? {
    val rangePattern =
        when (statusCode) {
            206 -> PARTIAL_CONTENT_RANGE
            416 -> UNSATISFIED_CONTENT_RANGE
            else -> null
        }
    if (rangePattern != null) {
        return contentRange
            ?.trim()
            ?.let(rangePattern::matchEntire)
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()
            ?.takeIf { it > 0L }
    }
    return if (statusCode == 200) contentLength?.toLongOrNull()?.takeIf { it > 0L } else null
}

private val PARTIAL_CONTENT_RANGE = Regex("""bytes\s+0-0/(\d+)""", RegexOption.IGNORE_CASE)
private val UNSATISFIED_CONTENT_RANGE = Regex("""bytes\s+\*/(\d+)""", RegexOption.IGNORE_CASE)
