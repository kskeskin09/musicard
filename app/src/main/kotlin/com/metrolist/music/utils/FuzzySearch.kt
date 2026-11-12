/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import java.text.Normalizer

/**
 * Forgiving text search: case, accents and Turkish dotless i don't matter, words can come in
 * any order, and small typos still match. Results are ranked best first.
 */
object FuzzySearch {
    private val marks = Regex("\\p{Mn}+")
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    fun normalize(text: String): String =
        Normalizer
            .normalize(text.lowercase().replace('ı', 'i'), Normalizer.Form.NFD)
            .replace(marks, "")

    private fun words(text: String) = normalize(text).split(separators).filter { it.isNotEmpty() }

    /** How well [text] matches [query]; higher is better, null when it doesn't match. */
    fun score(
        query: String,
        text: String,
    ): Int? {
        val q = normalize(query).trim()
        if (q.isEmpty()) return 0
        val t = normalize(text)
        if (t == q) return 1000
        if (t.startsWith(q)) return 900 - t.length.coerceAtMost(100)
        if (t.contains(q)) return 800 - t.length.coerceAtMost(100)
        val textWords = words(text)
        if (textWords.isEmpty()) return null
        var total = 0
        for (queryWord in q.split(separators).filter { it.isNotEmpty() }) {
            val best = textWords.maxOf { wordScore(queryWord, it) }
            if (best <= 0) return null
            total += best
        }
        return total
    }

    fun matches(
        query: String,
        text: String,
    ): Boolean = score(query, text) != null

    /** [items] that match [query], best first; [texts] gives the strings to search in. */
    fun <T> rank(
        items: List<T>,
        query: String,
        texts: (T) -> List<String?>,
    ): List<T> {
        if (query.isBlank()) return items
        return items
            .mapNotNull { item ->
                texts(item).filterNotNull().mapNotNull { score(query, it) }.maxOrNull()?.let { item to it }
            }.sortedByDescending { it.second }
            .map { it.first }
    }

    private fun wordScore(
        queryWord: String,
        word: String,
    ): Int {
        if (word.startsWith(queryWord)) return 100
        if (word.contains(queryWord)) return 80
        // Small typos: one wrong letter in short words, two in longer ones.
        val allowed = if (queryWord.length <= 4) 1 else 2
        val prefix = word.take(queryWord.length + 1)
        val distance = minOf(editDistance(queryWord, word), editDistance(queryWord, prefix))
        if (queryWord.length >= 3 && distance <= allowed) return 60 - distance * 10
        return 0
    }

    private fun editDistance(
        a: String,
        b: String,
    ): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }
}
