package com.metrolist.innertube.models

import java.text.Normalizer
import java.util.Locale

/**
 * YouTube Music sometimes ranks a video upload (official music video or a user upload) above the
 * official audio release of the same song. These helpers detect that case and move the official
 * release (MUSIC_VIDEO_TYPE_ATV, square cover art) in front of the video.
 */
object OfficialSongMatcher {
    private val bracketed = Regex("""[(\[{【].*?[)\]}】]""")
    private val noiseWords =
        Regex(
            """\b(official|offical|music|video|audio|lyric|lyrics|visualizer|clip|klip|resmi|hd|hq|4k|mv|m/v|video clip|sözleri|sozleri)\b""",
        )
    private val nonAlnum = Regex("""[^\p{L}\p{N}]+""")
    private val combiningMarks = Regex("""\p{M}+""")

    fun normalize(text: String): String =
        Normalizer
            .normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .replace('ı', 'i')
            .replace(bracketed, " ")
            .replace(noiseWords, " ")
            .replace(nonAlnum, " ")
            .trim()
            .replace(Regex("""\s+"""), " ")

    private fun artistNames(song: SongItem) = song.artists.map { normalize(it.name) }.filter { it.isNotBlank() }

    /** True when [official] looks like the official release of the same song as [video]. */
    fun isSameSong(
        video: SongItem,
        official: SongItem,
    ): Boolean {
        val officialTitle = normalize(official.title)
        if (officialTitle.isBlank()) return false
        val officialArtists = artistNames(official)
        val videoArtists = artistNames(video)

        // "Artist - Title (Official Video)" → try the whole title and every " - " part on its own.
        val rawVideoTitle = video.title
        val candidates =
            (listOf(rawVideoTitle) + rawVideoTitle.split(" - ", " – ", " — ", " | "))
                .map { normalize(it) }
                .toMutableSet()
        // Also strip the official artists' names from the video title ("Ezgi Erdoğan İz Bırakan Yaralar").
        candidates.toList().forEach { c ->
            var stripped = c
            officialArtists.forEach { stripped = stripped.replace(it, " ") }
            candidates += stripped.trim().replace(Regex("""\s+"""), " ")
        }
        if (officialTitle !in candidates) return false

        // Same title: also require the same artist, either credited or written in the video title.
        if (officialArtists.isEmpty()) return true
        val videoTitleNorm = normalize(rawVideoTitle)
        return officialArtists.any { a -> a in videoArtists || videoTitleNorm.contains(a) } ||
            videoArtists.isEmpty()
    }
}

/**
 * Within one list, moves the official release of a song to the position of a video version of the
 * same song that was ranked above it (the video takes the official one's old place).
 */
fun <T : YTItem> List<T>.preferOfficialSongs(enabled: Boolean = true): List<T> {
    if (!enabled || size < 2) return this
    val result = toMutableList()
    for (i in result.indices) {
        val video = result[i] as? SongItem ?: continue
        if (!video.isVideoSong) continue
        val j =
            (i + 1 until result.size).firstOrNull { k ->
                val other = result[k] as? SongItem
                other != null && !other.isVideoSong && OfficialSongMatcher.isSameSong(video, other)
            } ?: continue
        val official = result[j]
        result[j] = result[i]
        result[i] = official
    }
    return result
}
