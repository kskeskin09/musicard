package com.metrolist.music.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A background line carries the same timestamp as the line it accompanies. The player's
 * lyrics card used to pick the background line as the current one, so the main line stayed
 * dim and never got its word-by-word sweep.
 */
@RunWith(RobolectricTestRunner::class)
class LyricsCurrentLineTest {

    private val lyrics = """
        [00:06.97]{agent:v2}Get the vibe, it's gonna be lit tonight
        <Get:6.977:7.406|the:7.406:7.689|vibe,:7.689:8.075|it's:8.075:8.231|gonna:8.231:8.485|be:8.485:8.641|lit:8.641:9.14|tonight:9.14:9.786>
        [00:10.75]{agent:v1}Baby girl, yuh ah carry ten ton a phatness gimme some a dat
        <Baby:10.756:11.161|girl,:11.161:11.327|yuh:11.327:11.427|ah:11.427:11.527|carry:11.527:11.797|ten:11.797:12.083|ton:12.083:12.226|a:12.226:12.398|phatness:12.398:12.969|gimme:12.969:13.261|some:13.261:13.433|a:13.433:13.588|dat:13.588:13.743>
        [00:10.75]{bg}(Gimme dat)
        <(Gimme dat):13.896:14.496>
    """.trimIndent()

    @Test
    fun `main line stays current when a background line shares its timestamp`() {
        val lines = LyricsUtils.parseLyrics(lyrics)
        assertEquals(3, lines.size)
        val main = 1
        val background = 2
        assertEquals("v1", lines[main].agent)
        assertEquals(13, lines[main].words?.size)
        assertTrue(lines[background].isBackground)

        for (position in listOf(10_800L, 12_000L, 13_700L, 14_200L)) {
            assertEquals("at $position ms", main, LyricsUtils.findCurrentMainLineIndex(lines, position))
        }
        assertEquals(0, LyricsUtils.findCurrentMainLineIndex(lines, 8_000L))
    }

    @Test
    fun `background line lights up together with its main line`() {
        val lines = LyricsUtils.parseLyrics(lyrics)
        assertTrue(LyricsUtils.isPairedBackground(lines, index = 2, mainIndex = 1))
        assertFalse(LyricsUtils.isPairedBackground(lines, index = 2, mainIndex = 0))
        assertFalse(LyricsUtils.isPairedBackground(lines, index = 1, mainIndex = 0))
    }
}
