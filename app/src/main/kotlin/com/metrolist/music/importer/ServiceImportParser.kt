/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

/** Where the file a user picked came from, as far as its contents tell. */
enum class ImportSource { SPOTIFY, APPLE_MUSIC, FILE }

data class ImportTrack(
    val title: String,
    val artists: List<String>,
    val album: String? = null,
) {
    val key: String get() = "${title.lowercase()}|${artists.joinToString(",").lowercase()}"
}

data class ImportPlaylist(val name: String, val tracks: List<ImportTrack>)

data class ImportAlbum(val title: String, val artist: String)

data class ImportBundle(
    val source: ImportSource,
    val playlists: List<ImportPlaylist> = emptyList(),
    val likedSongs: List<ImportTrack> = emptyList(),
    val albums: List<ImportAlbum> = emptyList(),
    val artists: List<String> = emptyList(),
    /** Podcast episodes and similar entries that have no song to look for. */
    val skipped: Int = 0,
) {
    val isEmpty: Boolean
        get() = playlists.all { it.tracks.isEmpty() } && likedSongs.isEmpty() && albums.isEmpty() && artists.isEmpty()
}

/**
 * Reads the files people get from other music services and turns them into one [ImportBundle]:
 *
 * - Spotify's "Account data" ZIP (Playlist1.json, Playlist2.json…, YourLibrary.json), or one of those JSON files alone.
 * - Apple's data export (Apple_Media_Services ZIP, also the nested ZIPs inside it): "Apple Music Library Tracks",
 *   "Apple Music Library Playlists" and "Apple Music Likes and Dislikes", as JSON or CSV.
 * - A playlist exported from the Music app or iTunes as a text file (tab separated, often UTF-16).
 * - CSV files from Exportify, TuneMyMusic, Soundiiz and similar tools, when their header names the title and artist columns.
 *
 * Field names are read loosely (case and spacing ignored) because the services change them from time to time.
 */
object ServiceImportParser {
    private const val MAX_ENTRY_BYTES = 64L * 1024 * 1024
    private const val MAX_ZIP_DEPTH = 3

    class UnsupportedFileException : Exception()

    fun parse(context: Context, uri: Uri): ImportBundle {
        val name = queryName(context, uri)
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytesCapped() }
            ?: throw UnsupportedFileException()
        return parseBytes(name, bytes)
    }

    /** The file-format part of [parse], kept free of Android types so it can be checked on the JVM. */
    fun parseBytes(name: String, bytes: ByteArray): ImportBundle {
        val collector = Collector()
        collect(name, bytes, collector, depth = 0)
        val bundle = collector.build()
        if (bundle.isEmpty) throw UnsupportedFileException()
        return bundle
    }

    private fun queryName(context: Context, uri: Uri): String =
        runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull() ?: uri.lastPathSegment.orEmpty()

    // ---------------------------------------------------------------- file walking

    private fun collect(name: String, bytes: ByteArray, out: Collector, depth: Int) {
        if (isZip(bytes)) {
            if (depth >= MAX_ZIP_DEPTH) return
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) {
                        val entryName = entry.name.substringAfterLast('/')
                        if (isInteresting(entryName)) {
                            collect(entryName, zip.readBytesCapped(), out, depth + 1)
                        }
                    }
                    zip.closeEntry()
                }
            }
            return
        }
        val lower = name.lowercase()
        val text = decodeText(bytes)
        when {
            lower.endsWith(".json") || text.trimStart().let { it.startsWith("{") || it.startsWith("[") } ->
                runCatching { parseJson(name, text, out) }
            looksLikeAppleTxt(text) -> parseAppleTxt(name, text, out)
            else -> parseCsvFile(name, text, out)
        }
    }

    private fun isInteresting(entryName: String): Boolean {
        val lower = entryName.lowercase()
        if (lower.endsWith(".zip")) return true
        if (!(lower.endsWith(".json") || lower.endsWith(".csv") || lower.endsWith(".txt"))) return false
        return lower.startsWith("playlist") || lower.startsWith("yourlibrary") ||
            lower.contains("apple music library tracks") || lower.contains("apple music library playlists") ||
            lower.contains("likes and dislikes") || lower.contains("favorites") ||
            // A playlist file someone zipped up themselves.
            lower.endsWith(".csv")
    }

    private fun isZip(bytes: ByteArray) =
        bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() &&
            bytes[2].toInt() == 3 && bytes[3].toInt() == 4

    private fun InputStream.readBytesCapped(): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_ENTRY_BYTES) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun decodeText(bytes: ByteArray): String {
        val charset: Charset = when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> Charsets.UTF_16LE
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> Charsets.UTF_16BE
            // UTF-16 without a byte order mark: every other byte of ASCII text is zero.
            bytes.size >= 4 && bytes[1].toInt() == 0 && bytes[3].toInt() == 0 -> Charsets.UTF_16LE
            else -> Charsets.UTF_8
        }
        return String(bytes, charset).removePrefix("﻿")
    }

    // ---------------------------------------------------------------- JSON

    private fun parseJson(name: String, text: String, out: Collector) {
        when (val root = JSONTokener(text).nextValue()) {
            is JSONObject -> {
                when {
                    root.has("playlists") -> parseSpotifyPlaylists(root.optJSONArray("playlists"), out)
                    root.has("tracks") || root.has("albums") || root.has("artists") -> parseSpotifyLibrary(root, out)
                }
            }
            is JSONArray -> parseAppleRows(name, (0 until root.length()).mapNotNull { root.optJSONObject(it)?.toRow() }, out)
        }
    }

    private fun parseSpotifyPlaylists(array: JSONArray?, out: Collector) {
        array ?: return
        out.source(ImportSource.SPOTIFY)
        for (i in 0 until array.length()) {
            val playlist = array.optJSONObject(i) ?: continue
            val tracks = mutableListOf<ImportTrack>()
            val items = playlist.optJSONArray("items") ?: JSONArray()
            for (j in 0 until items.length()) {
                val item = items.optJSONObject(j) ?: continue
                val track = item.optJSONObject("track")
                val local = item.optJSONObject("localTrack")
                when {
                    track != null -> spotifyTrack(
                        track.str("trackName", "name", "track"),
                        track.str("artistName", "artist"),
                        track.str("albumName", "album"),
                    )?.let(tracks::add)
                    local != null -> spotifyLocalTrack(local.str("uri"))?.let(tracks::add) ?: out.skip()
                    else -> out.skip()
                }
            }
            val title = playlist.str("name") ?: "Spotify ${i + 1}"
            out.playlist(ImportPlaylist(title, tracks))
        }
    }

    private fun parseSpotifyLibrary(root: JSONObject, out: Collector) {
        out.source(ImportSource.SPOTIFY)
        root.optJSONArray("tracks")?.let { tracks ->
            for (i in 0 until tracks.length()) {
                val t = tracks.optJSONObject(i) ?: continue
                spotifyTrack(t.str("track", "trackName", "name"), t.str("artist", "artistName"), t.str("album", "albumName"))
                    ?.let(out::liked)
            }
        }
        root.optJSONArray("albums")?.let { albums ->
            for (i in 0 until albums.length()) {
                val a = albums.optJSONObject(i) ?: continue
                val title = a.str("album", "albumName", "name") ?: continue
                out.album(ImportAlbum(title, a.str("artist", "artistName").orEmpty()))
            }
        }
        root.optJSONArray("artists")?.let { artists ->
            for (i in 0 until artists.length()) {
                when (val value = artists.opt(i)) {
                    is JSONObject -> value.str("name", "artist")?.let(out::artist)
                    is String -> value.takeIf { it.isNotBlank() }?.let(out::artist)
                }
            }
        }
    }

    private fun spotifyTrack(title: String?, artist: String?, album: String?): ImportTrack? {
        if (title.isNullOrBlank()) return null
        return ImportTrack(title.trim(), splitArtists(artist), album?.takeIf { it.isNotBlank() })
    }

    /** "spotify:local:Artist:Album:Title:123" with "+" for spaces. */
    private fun spotifyLocalTrack(uri: String?): ImportTrack? {
        val parts = uri?.removePrefix("spotify:local:")?.split(':') ?: return null
        if (parts.size < 3) return null
        fun dec(s: String) = runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)
        val title = dec(parts[2]).ifBlank { return null }
        return ImportTrack(title, splitArtists(dec(parts[0])), dec(parts[1]).ifBlank { null })
    }

    private fun JSONObject.toRow(): Map<String, String> {
        val row = HashMap<String, String>()
        keys().forEach { key ->
            val value = opt(key)
            row[norm(key)] = when (value) {
                is JSONArray -> (0 until value.length()).joinToString(",") { value.opt(it).toString() }
                null, JSONObject.NULL -> ""
                else -> value.toString()
            }
        }
        return row
    }

    private fun JSONObject.str(vararg keys: String): String? {
        for (key in keys) {
            val value = optString(key, "")
            if (value.isNotBlank() && value != "null") return value
        }
        return null
    }

    // ---------------------------------------------------------------- Apple Music

    /** Rows of one Apple export file, with keys run through [norm]. */
    private fun parseAppleRows(name: String, rows: List<Map<String, String>>, out: Collector) {
        if (rows.isEmpty()) return
        val lower = name.lowercase()
        val keys = rows.first().keys
        when {
            lower.contains("playlists") || keys.any { it.contains("itemidentifiers") } -> {
                out.source(ImportSource.APPLE_MUSIC)
                out.applePlaylists += rows
            }
            lower.contains("likes and dislikes") || "itemdescription" in keys -> {
                out.source(ImportSource.APPLE_MUSIC)
                rows.forEach { row ->
                    val preference = row["preference"].orEmpty().uppercase()
                    if (preference.isNotEmpty() && preference != "LOVE" && preference != "LIKE") return@forEach
                    val desc = row["itemdescription"].orEmpty()
                    val artist = desc.substringBefore(" - ", "")
                    val title = desc.substringAfter(" - ", desc)
                    if (title.isNotBlank()) out.liked(ImportTrack(title.trim(), splitArtists(artist)))
                }
            }
            "trackidentifier" in keys || lower.contains("library tracks") -> {
                out.source(ImportSource.APPLE_MUSIC)
                rows.forEach { row ->
                    val title = row["title"].orEmpty().ifBlank { row["name"].orEmpty() }
                    if (title.isBlank()) return@forEach
                    val track = ImportTrack(title.trim(), splitArtists(row["artist"]), row["album"]?.ifBlank { null })
                    // Playlists point at "Track Identifier"; the other ids only fill gaps and never replace it.
                    row["trackidentifier"]?.takeIf { it.isNotBlank() }?.let { out.appleTracks[it] = track }
                    listOf("applemusictrackidentifier", "purchasedtrackidentifier", "tagmatchedtrackidentifier")
                        .mapNotNull { row[it]?.takeIf { id -> id.isNotBlank() } }
                        .forEach { out.appleTracks.putIfAbsent(it, track) }
                    out.appleLibrary.add(track)
                    val rating = row["tracklikerating"].orEmpty().uppercase()
                    if (rating == "LOVE" || rating == "LIKE") out.liked(track)
                }
            }
            else -> parseGenericRows(name, rows, out)
        }
    }

    private fun looksLikeAppleTxt(text: String): Boolean {
        val header = text.lineSequence().firstOrNull().orEmpty()
        return header.contains('\t') && header.split('\t').map(::norm).let { "name" in it && "artist" in it }
    }

    /** A playlist exported from the Music app or iTunes as "Text". */
    private fun parseAppleTxt(name: String, text: String, out: Collector) {
        val lines = text.lines().filter { it.isNotBlank() }
        val header = lines.first().split('\t').map(::norm)
        val rows = lines.drop(1).map { line ->
            val cells = line.split('\t')
            header.withIndex().associate { (i, key) -> key to cells.getOrElse(i) { "" } }
        }
        out.source(ImportSource.APPLE_MUSIC)
        val tracks = rows.mapNotNull { row ->
            val title = row["name"].orEmpty().trim()
            if (title.isEmpty()) null else ImportTrack(title, splitArtists(row["artist"]), row["album"]?.ifBlank { null })
        }
        out.playlist(ImportPlaylist(fileTitle(name), tracks))
    }

    // ---------------------------------------------------------------- CSV (Exportify, TuneMyMusic…)

    private fun parseCsvFile(name: String, text: String, out: Collector) {
        val rows = parseCsv(text)
        if (rows.size < 2) return
        val header = rows.first().map(::norm)
        val maps = rows.drop(1).map { cells -> header.withIndex().associate { (i, key) -> key to cells.getOrElse(i) { "" } } }
        if ("trackidentifier" in header || "itemdescription" in header || header.any { it.contains("itemidentifiers") }) {
            parseAppleRows(name, maps, out)
        } else {
            parseGenericRows(name, maps, out)
        }
    }

    private const val APPLE_LIBRARY_NAME = "Apple Music Library"
    private val LIKED_NAMES = setOf("likedsongs", "yourlikedsongs", "begenilensarkilar", "beğenilenşarkılar", "favoritesongs", "lovedtracks")

    private val TITLE_KEYS = listOf("trackname", "tracktitle", "title", "songname", "song", "name", "track")
    private val ARTIST_KEYS = listOf("artistnames", "artistname", "artists", "artist", "albumartistnames", "albumartist")
    private val ALBUM_KEYS = listOf("albumname", "album", "albumtitle")
    private val PLAYLIST_KEYS = listOf("playlistname", "playlist")

    private fun parseGenericRows(name: String, rows: List<Map<String, String>>, out: Collector) {
        val keys = rows.firstOrNull()?.keys ?: return
        val titleKey = TITLE_KEYS.firstOrNull { it in keys } ?: return
        val artistKey = ARTIST_KEYS.firstOrNull { it in keys } ?: return
        val albumKey = ALBUM_KEYS.firstOrNull { it in keys }
        val playlistKey = PLAYLIST_KEYS.firstOrNull { it in keys }
        if ("trackuri" in keys || "spotifyid" in keys) out.source(ImportSource.SPOTIFY)

        val grouped = LinkedHashMap<String, MutableList<ImportTrack>>()
        rows.forEach { row ->
            val title = row[titleKey].orEmpty().trim()
            if (title.isEmpty()) return@forEach
            val track = ImportTrack(title, splitArtists(row[artistKey]), albumKey?.let { row[it] }?.ifBlank { null })
            val list = playlistKey?.let { row[it] }?.ifBlank { null } ?: fileTitle(name)
            grouped.getOrPut(list) { mutableListOf() }.add(track)
        }
        grouped.forEach { (title, tracks) ->
            // Exportify and others name the liked songs file "Liked Songs" / "Liked_Songs".
            if (norm(title) in LIKED_NAMES) tracks.forEach(out::liked) else out.playlist(ImportPlaylist(title, tracks))
        }
    }

    /** RFC 4180 CSV: quoted fields, doubled quotes and line breaks inside quotes. Also reads ";" and tab separated files. */
    private fun parseCsv(text: String): List<List<String>> {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        val separator = listOf(',', ';', '\t').maxBy { sep -> firstLine.count { it == sep } }
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && text.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == separator -> { row.add(cell.toString()); cell.clear() }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    row.add(cell.toString()); cell.clear()
                    if (row.any { it.isNotBlank() }) rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        row.add(cell.toString())
        if (row.any { it.isNotBlank() }) rows.add(row)
        return rows
    }

    // ---------------------------------------------------------------- helpers

    private fun norm(key: String) = key.lowercase().filter { it.isLetterOrDigit() }

    private fun splitArtists(raw: String?): List<String> =
        raw.orEmpty().split(";", ",", " & ", " feat. ", " ft. ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    private fun fileTitle(name: String) =
        name.substringAfterLast('/').substringBeforeLast('.').replace('_', ' ').trim().ifBlank { "Imported playlist" }

    private class Collector {
        private var source: ImportSource? = null
        private val playlists = mutableListOf<ImportPlaylist>()
        private val liked = LinkedHashMap<String, ImportTrack>()
        private val albums = LinkedHashMap<String, ImportAlbum>()
        private val artists = LinkedHashMap<String, String>()
        private var skipped = 0

        val appleTracks = HashMap<String, ImportTrack>()
        val appleLibrary = mutableListOf<ImportTrack>()
        val applePlaylists = mutableListOf<Map<String, String>>()

        fun source(s: ImportSource) { if (source == null) source = s }
        fun playlist(p: ImportPlaylist) { if (p.tracks.isNotEmpty()) playlists.add(p) }
        fun liked(t: ImportTrack) { liked.putIfAbsent(t.key, t) }
        fun album(a: ImportAlbum) { albums.putIfAbsent("${a.title}|${a.artist}".lowercase(), a) }
        fun artist(name: String) { artists.putIfAbsent(name.lowercase(), name.trim()) }
        fun skip() { skipped++ }

        fun build(): ImportBundle {
            // Apple lists playlists by track ids, which the library file resolves, so join them last.
            applePlaylists.forEachIndexed { index, row ->
                val title = row["title"].orEmpty().ifBlank { row["name"].orEmpty() }.ifBlank { row["containername"].orEmpty() }
                val ids = row.entries.firstOrNull { it.key.contains("itemidentifiers") }?.value.orEmpty()
                    .split(',', ' ', '[', ']').map { it.trim() }.filter { it.isNotEmpty() }
                val tracks = ids.mapNotNull { appleTracks[it] ?: appleTracks[it.substringBefore('.')] }
                skipped += ids.size - tracks.size
                playlist(ImportPlaylist(title.ifBlank { "Apple Music ${index + 1}" }, tracks))
            }
            // Songs added to the Apple Music library are what liked songs are on Spotify; keep them as one playlist.
            appleLibrary.distinctBy { it.key }.takeIf { it.isNotEmpty() }?.let {
                playlists.add(0, ImportPlaylist(APPLE_LIBRARY_NAME, it))
            }
            return ImportBundle(
                source = source ?: ImportSource.FILE,
                playlists = playlists,
                likedSongs = liked.values.toList(),
                albums = albums.values.toList(),
                artists = artists.values.toList(),
                skipped = skipped,
            )
        }
    }
}
