/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

@Immutable
data class Playlist(
    @Embedded
    val playlist: PlaylistEntity,
    val songCount: Int,
    @Relation(
        entity = SongEntity::class,
        entityColumn = "id",
        parentColumn = "id",
        projection = ["thumbnailUrl"],
        associateBy =
        Junction(
            value = PlaylistSongMapPreview::class,
            parentColumn = "playlistId",
            entityColumn = "songId",
        ),
    )
    val songThumbnails: List<String?>,
) : LocalItem() {
    override val id: String
        get() = playlist.id
    override val title: String
        get() = playlist.name
    override val thumbnailUrl: String?
        get() = null
    
    /**
     * The playlist's own cover, when it has one. On the user's own playlists the picture YouTube
     * gives a synced playlist (one of its songs' covers) is not counted, so they show the
     * four-song collage instead; a cover the user chose (a photo, a link, or a cover uploaded on
     * YouTube) still counts.
     */
    val coverUrl: String?
        get() =
            playlist.thumbnailUrl?.takeIf { url ->
                !playlist.isEditable || isChosenPlaylistCover(url) || songThumbnails.all { it == null }
            }

    val thumbnails: List<String>
        get() = coverUrl?.let { listOf(it) } ?: songThumbnails.filterNotNull()
}

/** Whether [url] is a cover the user picked rather than a song cover YouTube filled in. */
fun isChosenPlaylistCover(url: String): Boolean =
    url.contains("studio_square_thumbnail") ||
        !(url.contains("i.ytimg.com") || url.contains("googleusercontent.com") || url.contains("ggpht.com"))
