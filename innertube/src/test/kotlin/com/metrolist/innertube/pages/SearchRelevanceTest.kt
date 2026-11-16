package com.metrolist.innertube.pages

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.SearchRelevance
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SearchRelevanceTest {
    private fun song(id: String, title: String, artist: String, type: String = MUSIC_VIDEO_TYPE_ATV) =
        SongItem(id = id, title = title, artists = listOf(Artist(artist, null)), musicVideoType = type, thumbnail = "")

    @Test
    fun `exact song moves above loosely related shelf items`() {
        val episode = EpisodeItem(id = "e", title = "Tarkan hakkında her şey", author = Artist("Podcast", null), thumbnail = "")
        val album = AlbumItem(browseId = "a", playlistId = "p", title = "Karma", artists = listOf(Artist("Tarkan", null)), thumbnail = "")
        val remix = song("r", "Kuzu Kuzu (Remix) feat. Someone", "Tarkan")
        val exact = song("s", "Kuzu Kuzu", "Tarkan")
        val ranked = SearchRelevance.rank("tarkan kuzu kuzu", listOf(episode, album, remix, exact))
        assertEquals("s", ranked.first().id)
        assertEquals("e", ranked.last().id)
    }

    @Test
    fun `unrelated items are dropped when enough items match`() {
        val good = (1..5).map { song("g$it", "Kuzu Kuzu $it", "Tarkan") }
        val unrelated = song("u", "Pembe Mezarlık", "Model")
        val ranked = SearchRelevance.rank("tarkan kuzu", listOf(unrelated) + good)
        assertFalse(ranked.any { it.id == "u" })
    }

    @Test
    fun `nothing is dropped when few items match`() {
        val ranked = SearchRelevance.rank("tarkn kuzx", listOf(song("a", "Kuzu Kuzu", "Tarkan"), song("b", "Şımarık", "Tarkan")))
        assertEquals(2, ranked.size)
    }

    @Test
    fun `official song gets a mild boost over the same video`() {
        val video = song("v", "Kuzu Kuzu", "Tarkan", "MUSIC_VIDEO_TYPE_OMV")
        val official = song("s", "Kuzu Kuzu", "Tarkan")
        assertEquals(listOf("s", "v"), SearchRelevance.rank("kuzu kuzu", listOf(video, official)).map { it.id })
    }

    @Test
    fun `turkish letters and partial words match`() {
        assertEquals(1.0, SearchRelevance.coverage("simarik", song("a", "Şımarık", "Tarkan")), 0.0)
        assertEquals(0.8, SearchRelevance.coverage("tark", song("a", "Şımarık", "Tarkan")), 0.0)
    }

    @Test
    fun `apostrophes are dropped and an unrelated top result is replaced`() {
        val unrelated = song("k", "KOMM, SUSSER TOD (M-10 Director's Edit Version)", "Arianne")
        val wanted = song("t", "THANATOS -IF I CAN'T BE YOURS-", "LOREN & MASH")
        val query = "thanatos if i cant be yours"
        assertEquals(1.0, SearchRelevance.coverage(query, wanted), 0.0)
        val ranked = SearchRelevance.rank(query, listOf(unrelated, wanted))
        assertEquals("t", SearchRelevance.pickTop(query, unrelated, ranked)?.id)
        assertEquals("t", SearchRelevance.pickTop(query, wanted, ranked)?.id)
    }
}
