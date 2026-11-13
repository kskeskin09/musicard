package com.metrolist.music.playback

import androidx.media3.exoplayer.offline.Download
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadUtilTest {
    @Test
    fun `completed downloads are not prepared again`() {
        assertFalse(shouldPrepareDownload(Download.STATE_COMPLETED))
        assertTrue(shouldPrepareDownload(Download.STATE_FAILED))
        assertTrue(shouldPrepareDownload(null))
    }

    @Test
    fun `downloadArtworkUrls keeps distinct song and album covers`() {
        assertEquals(
            listOf("song-cover", "album-cover"),
            downloadArtworkUrls("song-cover", "album-cover"),
        )
        assertEquals(
            listOf("song-cover"),
            downloadArtworkUrls("song-cover", "song-cover"),
        )
        assertEquals(emptyList<String>(), downloadArtworkUrls("", null))
    }

    private val done = Download.STATE_COMPLETED to Download.STOP_REASON_NONE
    private val queued = Download.STATE_QUEUED to Download.STOP_REASON_NONE
    private val paused = Download.STATE_STOPPED to AUTO_DOWNLOAD_PAUSED

    @Test
    fun `library songs without a download are started when allowed`() {
        val plan = planLibraryAutoDownload(true, true, setOf("a", "b", "c"), setOf("b"), mapOf("b" to done, "c" to done), AUTO_DOWNLOAD_PAUSED)
        assertEquals(listOf("a"), plan.start)
        assertTrue(plan.remove.isEmpty())
    }

    @Test
    fun `automatic downloads pause off Wi-Fi and resume later`() {
        val pausePlan = planLibraryAutoDownload(true, false, setOf("a", "b"), setOf("a", "b"), mapOf("a" to queued, "b" to done), AUTO_DOWNLOAD_PAUSED)
        assertEquals(listOf("a"), pausePlan.pause)
        assertTrue(pausePlan.start.isEmpty())
        val resumePlan = planLibraryAutoDownload(true, true, setOf("a"), setOf("a"), mapOf("a" to paused), AUTO_DOWNLOAD_PAUSED)
        assertEquals(listOf("a"), resumePlan.resume)
    }

    @Test
    fun `button downloads are never touched`() {
        val plan = planLibraryAutoDownload(false, false, setOf("x"), emptySet(), mapOf("x" to queued, "y" to done), AUTO_DOWNLOAD_PAUSED)
        assertTrue(plan.remove.isEmpty())
        assertTrue(plan.pause.isEmpty())
    }

    @Test
    fun `turning off drops unfinished automatic downloads only`() {
        val plan = planLibraryAutoDownload(false, false, setOf("a", "b"), setOf("a", "b"), mapOf("a" to queued, "b" to done), AUTO_DOWNLOAD_PAUSED)
        assertEquals(listOf("a"), plan.remove)
        assertEquals(setOf("a"), plan.forget)
    }

    @Test
    fun `songs that left the library lose their automatic download`() {
        val plan = planLibraryAutoDownload(true, true, setOf("b"), setOf("a"), mapOf("a" to done, "b" to done), AUTO_DOWNLOAD_PAUSED)
        assertEquals(listOf("a"), plan.remove)
        // An empty library (still loading) removes nothing.
        val empty = planLibraryAutoDownload(true, true, emptySet(), setOf("a"), mapOf("a" to done), AUTO_DOWNLOAD_PAUSED)
        assertTrue(empty.remove.isEmpty())
    }
}
