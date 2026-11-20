package com.metrolist.innertube.pages

import com.metrolist.innertube.models.PlaylistPanelVideoRenderer
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/**
 * The song version that YouTube Music pairs with a music video: the item behind the Song / Video
 * switch in the official app. The /next response holds it as a
 * playlistPanelVideoWrapperRenderer with a primaryRenderer (one version) and a counterpart (the other),
 * plus a segment map that tells which part of the video matches which part of the song.
 */
data class SongCounterpart(
    val song: SongItem,
    val segments: List<Segment>,
) {
    data class Segment(
        val videoStartMs: Long,
        val songStartMs: Long,
        val durationMs: Long,
    )

    /** Maps a position in the video to the same moment in the song (videos often have a longer intro). */
    fun songPosition(videoPositionMs: Long): Long {
        val position = videoPositionMs.coerceAtLeast(0L)
        for (segment in segments) {
            if (position < segment.videoStartMs) return segment.songStartMs
            val offset = position - segment.videoStartMs
            if (offset < segment.durationMs) return segment.songStartMs + offset
        }
        return segments.lastOrNull()?.let { it.songStartMs + it.durationMs } ?: position
    }

    companion object {
        private val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                coerceInputValues = true
            }

        /**
         * Finds the official song (ATV) paired with [videoId] in a raw /next response, or null when
         * YouTube Music shows no song version for it.
         */
        fun parse(
            response: JsonElement,
            videoId: String,
        ): SongCounterpart? {
            for (wrapper in response.findObjects("playlistPanelVideoWrapperRenderer")) {
                val primary = wrapper.panelRenderer("primaryRenderer") ?: continue
                val primaryId = primary.stringAt("videoId") ?: continue
                val counterparts = wrapper["counterpart"] as? JsonArray ?: continue
                for (entry in counterparts) {
                    val counterpart = entry as? JsonObject ?: continue
                    val other = counterpart.panelRenderer("counterpartRenderer") ?: continue
                    val otherId = other.stringAt("videoId") ?: continue
                    // The video can be either side of the pair.
                    val videoIsPrimary = primaryId == videoId
                    if (!videoIsPrimary && otherId != videoId) continue
                    val songRenderer = if (videoIsPrimary) other else primary
                    val song = decodeSong(songRenderer) ?: continue
                    if (song.id == videoId || song.musicVideoType != MUSIC_VIDEO_TYPE_ATV) continue
                    return SongCounterpart(
                        song = song,
                        segments = counterpart.segments(videoIsPrimary),
                    )
                }
            }
            return null
        }

        private fun decodeSong(renderer: JsonObject): SongItem? =
            runCatching {
                // "selected" is required by the model but is left out on the counterpart side.
                val complete = if ("selected" in renderer) renderer else JsonObject(renderer + ("selected" to JsonPrimitive(false)))
                json.decodeFromJsonElement(PlaylistPanelVideoRenderer.serializer(), complete)
            }.getOrNull()?.let(NextPage::fromPlaylistPanelVideoRenderer)

        private fun JsonObject.panelRenderer(key: String): JsonObject? =
            (this[key] as? JsonObject)?.get("playlistPanelVideoRenderer") as? JsonObject

        private fun JsonObject.stringAt(key: String): String? =
            (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

        private fun JsonObject.segments(videoIsPrimary: Boolean): List<Segment> =
            ((this["segmentMap"] as? JsonObject)?.get("segment") as? JsonArray)
                .orEmpty()
                .mapNotNull { element ->
                    val segment = element as? JsonObject ?: return@mapNotNull null
                    val primaryStart = segment.longAt("primaryVideoStartTimeMilliseconds") ?: return@mapNotNull null
                    val otherStart = segment.longAt("counterpartVideoStartTimeMilliseconds") ?: return@mapNotNull null
                    val duration = segment.longAt("durationMilliseconds")?.takeIf { it >= 0L } ?: return@mapNotNull null
                    Segment(
                        videoStartMs = if (videoIsPrimary) primaryStart else otherStart,
                        songStartMs = if (videoIsPrimary) otherStart else primaryStart,
                        durationMs = duration,
                    )
                }.sortedBy { it.videoStartMs }

        private fun JsonObject.longAt(key: String): Long? {
            val value = this[key] as? JsonPrimitive ?: return null
            return value.longOrNull ?: value.contentOrNull?.toLongOrNull()
        }

        private fun JsonElement.findObjects(key: String): Sequence<JsonObject> =
            sequence {
                when (val element = this@findObjects) {
                    is JsonObject -> {
                        (element[key] as? JsonObject)?.let { yield(it) }
                        element.values.forEach { yieldAll(it.findObjects(key)) }
                    }
                    is JsonArray -> element.forEach { yieldAll(it.findObjects(key)) }
                    else -> Unit
                }
            }
    }
}
