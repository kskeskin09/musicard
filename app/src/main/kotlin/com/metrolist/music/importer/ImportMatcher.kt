/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import java.text.Normalizer

/** Scores a search result against the song being imported. Kept free of Android types so it can be checked on the JVM. */
object ImportMatcher {
    /** A score at or above this counts as a sure match. */
    const val SURE = 3

    // Versions that are a different recording; a "(Remastered)" tag is not one of them.
    private val VARIANT_WORDS = listOf(
        "live", "canli", "remix", "cover", "karaoke", "instrumental", "acoustic", "akustik",
        "sped up", "slowed", "nightcore", "8d", "reverb", "piano version",
    )

    /**
     * 0–4: up to 2 points for the title and up to 2 for the artist,
     * minus 2 when the result is a live, remix, cover… version the source song is not.
     */
    fun score(wantTitle: String, wantArtists: List<String>, gotTitle: String, gotArtists: List<String>): Int {
        var score = 0
        val wt = clean(wantTitle)
        val gt = clean(gotTitle)
        when {
            wt.isEmpty() || gt.isEmpty() -> Unit
            wt == gt -> score += 2
            gt.contains(wt) || wt.contains(gt) -> score += 1
        }
        val want = wantArtists.map(::clean).filter { it.isNotEmpty() }
        val got = gotArtists.map(::clean).filter { it.isNotEmpty() }
        if (want.isEmpty()) {
            score += 1
        } else {
            val hits = want.count { w -> got.any { g -> g == w || g.contains(w) || w.contains(g) } }
            // YouTube often lists only the main artist, so the first one matching is enough for full points.
            if (hits > 0) score += if (hits == want.size || got.firstOrNull() in want) 2 else 1
        }
        if (variantOf(gotTitle).any { it !in variantOf(wantTitle) }) score -= 2
        return score
    }

    private fun variantOf(title: String): Set<String> {
        val lower = fold(title)
        return VARIANT_WORDS.filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lower) }.toSet()
    }

    private fun fold(text: String): String =
        Normalizer.normalize(text.lowercase().replace('ı', 'i'), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")

    fun clean(text: String): String =
        fold(text)
            // "Song (Remastered 2011)", "Song - Live" and "feat." parts don't decide whether the title matches.
            .replace(Regex("\\s*[(\\[].*?[)\\]]"), "")
            .replace(Regex("\\s+-\\s+.*$"), "")
            .replace(Regex("\\b(feat|ft)\\.?\\s.*$"), "")
            .filter { it.isLetterOrDigit() }
}
