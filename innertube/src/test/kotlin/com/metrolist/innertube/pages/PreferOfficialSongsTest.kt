package com.metrolist.innertube.pages

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferOfficialSongsTest {
    private fun song(id: String, title: String, artist: String, type: String) =
        SongItem(id = id, title = title, artists = listOf(Artist(artist, null)), musicVideoType = type, thumbnail = "")

    @Test
    fun `video top result swaps with official song of same title`() {
        val video = song("v", "İz Bırakan Yaralar", "Ezgi Erdoğan", "MUSIC_VIDEO_TYPE_UGC")
        val official = song("s", "İz Bırakan Yaralar", "Ezgi Erdoğan", MUSIC_VIDEO_TYPE_ATV)
        val other = song("o", "Pembe Mezarlık", "Model", MUSIC_VIDEO_TYPE_ATV)
        val page =
            SearchSummaryPage(
                listOf(SearchSummary("Top result", listOf(video)), SearchSummary("Songs", listOf(official, other))),
            ).preferOfficialSongs()
        assertEquals("s", page.summaries[0].items[0].id)
        assertEquals(listOf("v", "o"), page.summaries[1].items.map { it.id })
    }

    @Test
    fun `artist dash title official video matches`() {
        val video = song("v", "Ezgi Erdoğan - İz Bırakan Yaralar (Official Video)", "gedikmüzik", "MUSIC_VIDEO_TYPE_UGC")
        val official = song("s", "Iz Birakan Yaralar", "Ezgi Erdoğan", MUSIC_VIDEO_TYPE_ATV)
        val page =
            SearchSummaryPage(
                listOf(SearchSummary("Top result", listOf(video)), SearchSummary("Songs", listOf(official))),
            ).preferOfficialSongs()
        assertEquals("s", page.summaries[0].items[0].id)
    }

    @Test
    fun `different song or non-song top result is left alone`() {
        val video = song("v", "Gülmüyor Hiç Yüzüm", "Şaban Gedik", "MUSIC_VIDEO_TYPE_UGC")
        val official = song("s", "İz Bırakan Yaralar", "Ezgi Erdoğan", MUSIC_VIDEO_TYPE_ATV)
        val page1 =
            SearchSummaryPage(
                listOf(SearchSummary("Top result", listOf(video)), SearchSummary("Songs", listOf(official))),
            ).preferOfficialSongs()
        assertEquals("v", page1.summaries[0].items[0].id)

        val album = AlbumItem(browseId = "a", playlistId = "p", title = "İz Bırakan Yaralar", artists = null, year = null, thumbnail = "")
        val page2 =
            SearchSummaryPage(
                listOf(SearchSummary("Top result", listOf(album)), SearchSummary("Songs", listOf(official))),
            ).preferOfficialSongs()
        assertEquals("a", page2.summaries[0].items[0].id)
    }

    @Test
    fun `disabled keeps order`() {
        val video = song("v", "X", "A", "MUSIC_VIDEO_TYPE_OMV")
        val official = song("s", "X", "A", MUSIC_VIDEO_TYPE_ATV)
        val page =
            SearchSummaryPage(
                listOf(SearchSummary("Top result", listOf(video)), SearchSummary("Songs", listOf(official))),
            ).preferOfficialSongs(enabled = false)
        assertEquals("v", page.summaries[0].items[0].id)
    }
}
