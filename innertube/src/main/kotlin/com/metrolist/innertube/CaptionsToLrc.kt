package com.metrolist.innertube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.util.Locale

/**
 * YouTube timed text (fmt=json3) as the app's lyrics text: one line per caption event, and when
 * the event's pieces are timed on their own (auto-generated captions), word timing in the
 * rich format `[mm:ss.xxx]<mm:ss.xxx>word <mm:ss.xxx>word<mm:ss.xxx>`.
 */
internal object CaptionsToLrc {
    private val soundLabel = Regex("""\[[^\]]*\]""")
    private val whitespace = Regex("""\s+""")

    private fun time(ms: Long): String {
        val safe = ms.coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d.%03d", safe / 60_000, (safe / 1000) % 60, safe % 1000)
    }

    private fun clean(text: String) =
        text.replace(soundLabel, " ").replace('♪', ' ').replace(whitespace, " ")

    fun convert(json: String): String {
        val events = runCatching { Json.parseToJsonElement(json).jsonObject["events"]?.jsonArray }.getOrNull()
            ?: return ""
        val out = StringBuilder()
        var previousText: String? = null
        for (element in events) {
            val event = element as? JsonObject ?: continue
            val start = event["tStartMs"]?.jsonPrimitive?.longOrNull ?: continue
            val end = start + (event["dDurationMs"]?.jsonPrimitive?.longOrNull ?: 0L)
            val segs = event["segs"]?.jsonArray ?: continue
            // Each piece as (start, text); a seg with no offset of its own belongs to the piece before it.
            val pieces = ArrayList<Pair<Long, StringBuilder>>()
            for (seg in segs) {
                val segObject = seg as? JsonObject ?: continue
                val text = segObject["utf8"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val offset = segObject["tOffsetMs"]?.jsonPrimitive?.longOrNull
                if (offset != null || pieces.isEmpty()) pieces += (start + (offset ?: 0L)) to StringBuilder(text)
                else pieces.last().second.append(text)
            }
            val cleaned = pieces.map { (pieceStart, raw) -> pieceStart to clean(raw.toString()) }
                .filter { it.second.isNotBlank() }
            if (cleaned.isEmpty()) continue
            val lineText = cleaned.joinToString("") { it.second }.replace(whitespace, " ").trim()
            // A rolling caption repeats its line as the next one scrolls in.
            if (lineText.isEmpty() || lineText == previousText) continue
            previousText = lineText

            out.append('[').append(time(cleaned.first().first)).append(']')
            if (cleaned.size > 1) {
                cleaned.forEachIndexed { index, (pieceStart, text) ->
                    // Auto captions put the space before a word; the app reads it after the word.
                    val next = cleaned.getOrNull(index + 1)?.second
                    val spaceAfter = next != null && (text.last().isWhitespace() || next.first().isWhitespace())
                    val piece = text.trim() + if (spaceAfter) " " else ""
                    out.append('<').append(time(pieceStart)).append('>')
                    out.append(piece.replace('<', '‹').replace('>', '›'))
                }
                out.append('<').append(time(maxOf(end, cleaned.last().first))).append('>')
            } else {
                out.append(lineText)
            }
            out.append('\n')
        }
        return out.toString().trimEnd()
    }
}
