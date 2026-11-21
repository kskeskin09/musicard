package com.metrolist.innertube.pages

import com.metrolist.innertube.models.Album
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.MusicResponsiveListItemRenderer
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.Run
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.splitBySeparator
import com.metrolist.innertube.utils.parseTime
import timber.log.Timber

data class PlaylistPage(
    val playlist: PlaylistItem,
    val songs: List<SongItem>,
    val songsContinuation: String?,
    val continuation: String?,
    val unavailableSongs: List<UnavailableSong> = emptyList(),
) {
    companion object {
        /**
         * Same rule as Metrolist: a greyed row with no duration and nothing to play
         * (or no title) cannot be played. Greyed rows that still have a duration play fine.
         */
        fun isUnavailable(renderer: MusicResponsiveListItemRenderer): Boolean {
            if (!renderer.isGreyedOut) return false
            val title = renderer.flexColumns.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text
            if (title.isNullOrBlank()) return true
            val duration = renderer.fixedColumns?.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text?.parseTime()
            if ((duration ?: 0) > 0) return false
            return renderer.playableVideoId.isNullOrBlank()
        }

        /** Splits playlist rows into playable songs and greyed rows kept for display only. */
        fun parseRows(renderers: List<MusicResponsiveListItemRenderer>): Pair<List<SongItem>, List<UnavailableSong>> {
            val songs = mutableListOf<SongItem>()
            val unavailable = mutableListOf<UnavailableSong>()
            for (renderer in renderers) {
                if (isUnavailable(renderer)) {
                    val runs = renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                    val title = runs?.firstOrNull()?.text?.takeIf { it.isNotBlank() } ?: continue
                    unavailable += UnavailableSong(
                        title = title,
                        subtitle = renderer.flexColumns.getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                            ?.joinToString("") { it.text }
                            ?.takeIf { it.isNotBlank() },
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                        afterSongId = songs.lastOrNull()?.id,
                    )
                } else {
                    fromMusicResponsiveListItemRenderer(renderer)?.let { songs += it }
                }
            }
            return songs to unavailable
        }

        fun ownerFromDetailSubtitle(runs: List<Run>?): Artist? = ownerFromByline(runs?.splitBySeparator()?.getOrNull(1))

        fun ownerFromByline(runs: List<Run>?): Artist? =
            PageHelper.extractArtists(runs).firstOrNull()
                ?: runs
                    ?.splitBySeparator()
                    ?.firstOrNull()
                    ?.firstOrNull()
                    ?.takeIf { it.navigationEndpoint == null && it.text.isNotBlank() }
                    ?.let { Artist(it.text.trim(), null) }

        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

            val secondaryLineRuns =
                renderer.flexColumns
                    .getOrNull(1)
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text
                    ?.runs

            val title =
                renderer.flexColumns
                    .firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text
                    ?.runs
                    ?.firstOrNull()
                    ?.text ?: return null

            if (secondaryLineRuns == null) {
                Timber.w("PlaylistPage.fromMusicResponsiveListItemRenderer: Song '$title' - NO SECONDARY LINE (flexColumns[1] is null)")
            }

            val artists = PageHelper.extractArtists(secondaryLineRuns)

            return SongItem(
                id = renderer.videoId ?: return null,
                title = title,
                artists = artists,
                album =
                    renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                        Album(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null,
                        )
                    },
                duration =
                    renderer.fixedColumns
                        ?.firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.firstOrNull()
                        ?.text
                        ?.parseTime(),
                musicVideoType = renderer.musicVideoType,
                thumbnail = renderer.thumbnail?.getThumbnailUrl() ?: return null,
                explicit =
                    renderer.badges?.find {
                        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                    } != null,
                endpoint =
                    renderer.overlay
                        ?.musicItemThumbnailOverlayRenderer
                        ?.content
                        ?.musicPlayButtonRenderer
                        ?.playNavigationEndpoint
                        ?.watchEndpoint,
                setVideoId = renderer.playlistSetVideoId ?: return null,
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken,
                isEpisode = renderer.isEpisode,
            )
        }
    }
}
