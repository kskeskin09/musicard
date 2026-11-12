/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import java.util.Locale

/**
 * A quick, offline guess of the language of some lyrics, used to skip automatic translation
 * when the lyrics are already in the target language. Scripts decide most languages; Latin-script
 * languages are told apart by their most common short words. Returns null when unsure.
 */
object LyricsLanguageGuess {
    private val commonWords =
        mapOf(
            "en" to setOf("the", "and", "you", "i", "to", "me", "my", "is", "it", "in", "of", "your", "that", "for", "on", "be", "we", "don't", "i'm", "love"),
            "tr" to setOf("ve", "bir", "bu", "ben", "sen", "ne", "da", "de", "gibi", "için", "çok", "var", "yok", "beni", "seni", "ama", "her", "mi", "olsun", "kadar"),
            "es" to setOf("el", "la", "que", "de", "y", "en", "un", "una", "no", "me", "te", "mi", "tu", "por", "con", "se", "lo", "amor", "yo", "es"),
            "pt" to setOf("o", "a", "que", "de", "e", "não", "um", "uma", "eu", "você", "meu", "minha", "com", "se", "é", "do", "da", "pra", "te", "em"),
            "de" to setOf("der", "die", "das", "und", "ich", "du", "nicht", "ist", "ein", "eine", "mich", "dich", "mit", "zu", "es", "wir", "auf", "sie", "mein", "so"),
            "fr" to setOf("le", "la", "les", "et", "je", "tu", "de", "un", "une", "pas", "que", "est", "moi", "toi", "mon", "ma", "dans", "pour", "qui", "on"),
            "it" to setOf("il", "la", "che", "e", "di", "un", "una", "non", "io", "tu", "mi", "ti", "per", "con", "sei", "sono", "è", "del", "amore", "ma"),
            "nl" to setOf("de", "het", "een", "en", "ik", "je", "niet", "is", "dat", "van", "mijn", "jij", "wij", "maar", "met", "op", "voor", "zijn", "er", "ook"),
            "id" to setOf("aku", "kau", "dan", "yang", "di", "tak", "ini", "itu", "kamu", "dengan", "untuk", "cinta", "tidak", "ku", "ada", "akan", "saja", "dari", "dia", "kita"),
            "pl" to setOf("i", "w", "nie", "na", "się", "że", "to", "jest", "ja", "ty", "mnie", "cię", "z", "do", "jak", "co", "mi", "tak", "już", "moja"),
            "sv" to setOf("och", "jag", "du", "det", "att", "en", "är", "inte", "som", "på", "mig", "dig", "med", "har", "min", "vi", "för", "så", "till", "om"),
        )

    fun guess(text: String): String? {
        var latin = 0
        var total = 0
        val scriptCounts = HashMap<String, Int>()
        for (ch in text) {
            if (!ch.isLetter()) continue
            total++
            val script =
                when (Character.UnicodeScript.of(ch.code)) {
                    Character.UnicodeScript.HANGUL -> "ko"
                    Character.UnicodeScript.HIRAGANA, Character.UnicodeScript.KATAKANA -> "ja"
                    Character.UnicodeScript.HAN -> "zh"
                    Character.UnicodeScript.CYRILLIC -> "ru"
                    Character.UnicodeScript.ARABIC -> "ar"
                    Character.UnicodeScript.DEVANAGARI -> "hi"
                    Character.UnicodeScript.BENGALI -> "bn"
                    Character.UnicodeScript.GURMUKHI -> "pa"
                    Character.UnicodeScript.THAI -> "th"
                    Character.UnicodeScript.GREEK -> "el"
                    Character.UnicodeScript.HEBREW -> "he"
                    Character.UnicodeScript.LATIN -> {
                        latin++
                        null
                    }
                    else -> null
                } ?: continue
            scriptCounts[script] = (scriptCounts[script] ?: 0) + 1
        }
        if (total == 0) return null
        // Any kana means Japanese, even when most characters are kanji.
        if ((scriptCounts["ja"] ?: 0) > 0) return "ja"
        val topScript = scriptCounts.maxByOrNull { it.value }
        if (topScript != null && topScript.value > latin) {
            if (topScript.key == "ru" && LyricsUtils.isUkrainian(text)) return "uk"
            return topScript.key
        }
        // Latin script: count each language's common words.
        val words =
            text
                .lowercase(Locale.ROOT)
                .split(Regex("[^\\p{L}']+"))
                .filter { it.isNotBlank() }
        if (words.size < 5) return null
        val scores = commonWords.mapValues { (_, set) -> words.count { it in set } }
        val sorted = scores.entries.sortedByDescending { it.value }
        val best = sorted.first()
        val second = sorted.getOrNull(1)?.value ?: 0
        return if (best.value >= 4 && best.value >= second * 1.5) best.key else null
    }
}
