package com.metrolist.innertube.models

import java.text.Normalizer
import java.util.Locale

/**
 * Ranks search results by how well they match what was typed, instead of showing YouTube Music's
 * shelves (Songs, Videos, Episodes, ...) one under the other. Every shelf is filled whether or not
 * its items are close to the query, which is where most unrelated results came from.
 *
 * Score, roughly:
 * - how many of the typed words appear in the item's name or artist (whole word, or the start of one),
 * - a bonus when the name itself is (almost) exactly what was typed,
 * - a mild boost for official songs, a smaller one for videos, a penalty for podcasts and episodes,
 * - YouTube's own order as a small tie-breaker.
 * When enough items match well, items sharing no word with the query are left out.
 */
object SearchRelevance {
    private val combiningMarks = Regex("""\p{M}+""")
    private val nonAlnum = Regex("""[^\p{L}\p{N}]+""")
    private val spaces = Regex("""\s+""")

    fun normalize(text: String): String =
        Normalizer
            .normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .replace('ı', 'i')
            .replace(nonAlnum, " ")
            .trim()
            .replace(spaces, " ")

    private fun tokens(text: String): List<String> = normalize(text).split(' ').filter { it.isNotEmpty() }

    private fun artistText(item: YTItem): String =
        when (item) {
            is SongItem -> item.artists.joinToString(" ") { it.name }
            is AlbumItem -> item.artists.orEmpty().joinToString(" ") { it.name }
            is PlaylistItem -> item.author?.name.orEmpty()
            is PodcastItem -> item.author?.name.orEmpty()
            is EpisodeItem -> item.author?.name.orEmpty()
            is ArtistItem -> ""
        }

    /** 1 for the same word, 0.8 when a typed word is the start of a longer one ("tark" → "tarkan"). */
    private fun wordMatch(
        typed: String,
        words: List<String>,
    ): Double =
        when {
            typed in words -> 1.0
            typed.length >= 2 && words.any { it.startsWith(typed) } -> 0.8
            else -> 0.0
        }

    /** Share of the typed words found in the item's name or artist, from 0 to 1. */
    fun coverage(
        query: String,
        item: YTItem,
    ): Double {
        val typed = tokens(query).distinct()
        if (typed.isEmpty()) return 0.0
        val words = tokens(item.title) + tokens(artistText(item))
        return typed.sumOf { wordMatch(it, words) } / typed.size
    }

    fun score(
        query: String,
        item: YTItem,
        youtubeRank: Int,
    ): Double {
        val q = normalize(query)
        val typed = tokens(query).toSet()
        val cover = coverage(query, item)
        var score = cover * 60

        if (cover > 0) {
            // The name without "(Official Video)", "[Lyrics]" and similar extras.
            val coreTitle = OfficialSongMatcher.normalize(item.title)
            val title = normalize(item.title)
            val artist = normalize(artistText(item))
            val exact =
                q == title || q == coreTitle ||
                    (artist.isNotEmpty() && (q == "$coreTitle $artist" || q == "$artist $coreTitle"))
            score +=
                if (exact) {
                    40.0
                } else {
                    // How much of the name is made of typed words: "Kuzu Kuzu" for "tarkan kuzu kuzu"
                    // beats "Kuzu Kuzu (Remix) feat. Someone".
                    val titleWords = coreTitle.split(' ').filter { it.isNotEmpty() }
                    if (titleWords.isEmpty()) 0.0 else 20.0 * titleWords.count { it in typed } / titleWords.size
                }
        }

        score +=
            when (item) {
                is SongItem ->
                    when {
                        item.isEpisode -> -15.0
                        item.isVideoSong -> 3.0
                        else -> 6.0
                    }
                is ArtistItem -> if (item.isProfile) -15.0 else 2.0
                is AlbumItem -> 0.0
                is PlaylistItem -> if (item.isPodcast) -15.0 else -4.0
                is PodcastItem, is EpisodeItem -> -15.0
            }

        score -= minOf(youtubeRank, 40) * 0.3
        return score
    }

    /**
     * [items] in YouTube's order (best first, duplicates allowed). Returns them sorted by relevance,
     * without duplicates and, when at least [minGoodMatches] items match well, without the items
     * that share no word with the query.
     */
    fun rank(
        query: String,
        items: List<YTItem>,
        minGoodMatches: Int = 5,
    ): List<YTItem> {
        val unique = items.distinctBy { it.id }.withIndex().toList()
        val coverages = unique.map { coverage(query, it.value) }
        val dropUnrelated = coverages.count { it >= 0.5 } >= minGoodMatches
        return unique
            .filter { !dropUnrelated || coverages[it.index] > 0.0 }
            .sortedByDescending { score(query, it.value, it.index) }
            .map { it.value }
    }
}
