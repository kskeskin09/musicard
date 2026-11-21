/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.sponsorblock

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive

val SponsorBlockPodcastsKey = booleanPreferencesKey("sponsorBlockPodcasts")
val SponsorBlockMusicKey = booleanPreferencesKey("sponsorBlockMusicOfftopic")

private const val PODCAST_VIDEO_TYPE = "MUSIC_VIDEO_TYPE_PODCAST_EPISODE"

fun sponsorBlockCategoriesFor(
    metadata: MediaMetadata,
    podcasts: Boolean,
    music: Boolean,
): Set<String> {
    if (metadata.id.startsWith("local_") || metadata.uploadEntityId != null) return emptySet()
    val isPodcast = metadata.isEpisode || metadata.musicVideoType == PODCAST_VIDEO_TYPE
    return when {
        isPodcast && podcasts -> SponsorBlockPodcastCategories
        // Audio-only (ATV) tracks have no intros or talking to skip.
        !isPodcast && music && metadata.musicVideoType != MUSIC_VIDEO_TYPE_ATV -> setOf(SponsorBlockMusicCategory)
        else -> emptySet()
    }
}

/**
 * Skips SponsorBlock segments of the playing item. Runs until cancelled; does nothing
 * (no network, no polling) while both options are off.
 */
suspend fun observeSponsorBlockSkipping(
    currentMetadata: Flow<MediaMetadata?>,
    preferences: Flow<Preferences>,
    currentMediaId: () -> String?,
    isPlaying: () -> Boolean,
    positionMs: () -> Long,
    durationMs: () -> Long,
    canSeek: () -> Boolean,
    seekTo: (Long) -> Unit,
    onSkipped: () -> Unit,
) {
    val options = preferences
        .map { (it[SponsorBlockPodcastsKey] ?: false) to (it[SponsorBlockMusicKey] ?: false) }
        .distinctUntilChanged()
    combine(currentMetadata.distinctUntilChanged { a, b -> a?.id == b?.id }, options) { metadata, opts -> metadata to opts }
        .collectLatest { (metadata, opts) ->
            if (metadata == null) return@collectLatest
            val categories = sponsorBlockCategoriesFor(metadata, opts.first, opts.second)
            if (categories.isEmpty()) return@collectLatest
            val segments = SponsorBlockClient.segments(metadata.id, categories)
            if (segments.isEmpty()) return@collectLatest
            var lastTarget: Long? = null
            while (currentCoroutineContext().isActive) {
                if (currentMediaId() == metadata.id && isPlaying()) {
                    val target = segments.skipTarget(positionMs(), durationMs())
                    if (target == null) {
                        lastTarget = null
                    } else if (target != lastTarget && canSeek()) {
                        // Remember the target so a user seeking back into the part can listen to it.
                        lastTarget = target
                        seekTo(target)
                        onSkipped()
                    }
                    delay(400)
                } else {
                    delay(1_000)
                }
            }
        }
}
