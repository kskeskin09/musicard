/**
 * Musicard
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_UGC
import com.metrolist.innertube.pages.SongCounterpart
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.Queue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * "Prefer songs over videos" during playback: when a music video is about to play (or has just
 * started), it is replaced in the queue by the official song that YouTube Music itself pairs with
 * it (the Song / Video switch in the official app). Nothing is removed from lists: if YouTube Music
 * has no song version, the video simply plays.
 *
 * Only the playing item and the next one are looked up, so a long queue costs no extra requests.
 */
class SongVersionSwapper(
    private val scope: CoroutineScope,
    private val player: () -> ExoPlayer,
    enabled: Flow<Boolean>,
    /** False while casting or following someone else's Listen Together room. */
    private val canSwap: () -> Boolean,
    private val isDownloaded: (String) -> Boolean,
) {
    private class Lookup(
        val counterpart: SongCounterpart?,
    )

    private val results =
        object : LinkedHashMap<String, Lookup>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Lookup>?) = size > MAX_CACHED
        }
    private val inFlight = HashSet<String>()

    @Volatile
    private var isOn = true

    init {
        scope.launch { enabled.distinctUntilChanged().collect { isOn = it } }
    }

    private fun isEnabled() = isOn

    fun isCandidate(metadata: MediaMetadata?): Boolean =
        metadata != null &&
            !metadata.isEpisode &&
            metadata.uploadEntityId == null &&
            (metadata.musicVideoType == MUSIC_VIDEO_TYPE_OMV || metadata.musicVideoType == MUSIC_VIDEO_TYPE_UGC) &&
            !isDownloaded(metadata.id)

    private var startJob: Job? = null
    private var startCheckedId: String? = null

    /**
     * For the first item of a new queue: when it is a video, looks up its song version first (at
     * most [START_LOOKUP_TIMEOUT_MS]) so a tapped video starts as the song instead of switching after
     * a moment. Returns true when it took over; [play] is then called with the song, or with null
     * when there is none, and the caller should start the queue again from there.
     */
    fun lookUpBeforeStart(
        first: MediaMetadata?,
        play: (MediaMetadata?) -> Unit,
    ): Boolean {
        startJob?.cancel()
        startJob = null
        if (first == null) return false
        if (first.id == startCheckedId) {
            startCheckedId = null
            return false
        }
        if (!isEnabled() || !canSwap() || !isCandidate(first)) return false
        val known = synchronized(results) { results[first.id] }
        if (known != null && known.counterpart == null) return false
        startJob =
            scope.launch {
                val counterpart = withTimeoutOrNull(START_LOOKUP_TIMEOUT_MS) { lookup(first.id) }
                startCheckedId = if (counterpart == null) first.id else null
                play(counterpart?.let { replacementFor(first, it) })
            }
        return true
    }

    /** Called on queue changes and song changes: checks the playing item and the next one. */
    fun check() {
        if (!isEnabled() || !canSwap()) return
        val p = player()
        if (p.mediaItemCount == 0) return
        for (index in watchedIndices(p)) {
            val metadata = p.getMediaItemAt(index).metadata ?: continue
            if (!isCandidate(metadata)) continue
            val id = metadata.id
            val cached = synchronized(results) { results[id] }
            if (cached != null) {
                cached.counterpart?.let { apply(id, it) }
                continue
            }
            if (!inFlight.add(id)) continue
            scope.launch {
                val counterpart =
                    try {
                        lookup(id)
                    } finally {
                        inFlight.remove(id)
                    }
                if (counterpart != null) apply(id, counterpart)
            }
        }
    }

    private suspend fun lookup(videoId: String): SongCounterpart? {
        synchronized(results) { results[videoId] }?.let { return it.counterpart }
        val result = withContext(Dispatchers.IO) { YouTube.songCounterpart(videoId) }
        result
            .onSuccess { counterpart ->
                // Only a clear answer is remembered; a network error is tried again next time.
                synchronized(results) { results[videoId] = Lookup(counterpart) }
            }.onFailure { Timber.tag(TAG).d(it, "Song version lookup failed for $videoId") }
        return result.getOrNull()
    }

    private fun apply(
        videoId: String,
        counterpart: SongCounterpart,
    ) {
        if (!isEnabled() || !canSwap()) return
        val p = player()
        val index = watchedIndices(p).firstOrNull { p.getMediaItemAt(it).mediaId == videoId } ?: return
        val source = p.getMediaItemAt(index).metadata ?: return
        val replacement = replacementFor(source, counterpart).toMediaItem()
        val isCurrent = index == p.currentMediaItemIndex
        val position = if (isCurrent) p.currentPosition else 0L
        // Switching late would restart the song the listener is already into.
        if (isCurrent && position > MAX_CURRENT_SWAP_POSITION_MS) return
        val shuffleOrder = shuffledIndices(p)

        p.replaceMediaItem(index, replacement)
        // Keep the shuffle order: a replaced item would otherwise land at a random place.
        shuffleOrder?.let { order ->
            if (order.size == p.mediaItemCount) p.setShuffleOrder(DefaultShuffleOrder(order, System.nanoTime()))
        }
        if (isCurrent) {
            val target = if (counterpart.segments.isNotEmpty()) counterpart.songPosition(position) else 0L
            if (target > 1_000L) p.seekTo(index, target)
        }
        Timber.tag(TAG).d("Playing the song version ${counterpart.song.id} instead of the video $videoId")
    }

    private fun replacementFor(
        source: MediaMetadata,
        counterpart: SongCounterpart,
    ): MediaMetadata =
        counterpart.song.toMediaMetadata().copy(
            setVideoId = source.setVideoId,
            suggestedBy = source.suggestedBy,
        )

    private fun watchedIndices(p: Player): List<Int> =
        listOf(p.currentMediaItemIndex, p.nextMediaItemIndex)
            .filter { it != C.INDEX_UNSET && it in 0 until p.mediaItemCount }
            .distinct()

    private fun shuffledIndices(p: Player): IntArray? {
        if (!p.shuffleModeEnabled) return null
        val timeline = p.currentTimeline
        val order = IntArray(timeline.windowCount)
        var index = timeline.getFirstWindowIndex(true)
        var count = 0
        while (index != C.INDEX_UNSET && count < order.size) {
            order[count++] = index
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
        }
        return order.takeIf { count == order.size }
    }

    companion object {
        private const val TAG = "SongVersionSwapper"
        private const val MAX_CACHED = 300
        private const val START_LOOKUP_TIMEOUT_MS = 1_500L
        private const val MAX_CURRENT_SWAP_POSITION_MS = 20_000L
    }
}

/** The same queue, starting with [item] (the song version of its first video) instead. */
internal fun Queue.withPreloadItem(item: MediaMetadata): Queue = PreloadReplacedQueue(this, item)

private class PreloadReplacedQueue(
    base: Queue,
    override val preloadItem: MediaMetadata,
) : Queue by base
