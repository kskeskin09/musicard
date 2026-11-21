package com.metrolist.music.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Some LRC files end a line with its end time, "[00:07.28]I love you[00:08.00]", or
 * put timestamps between the words. Those tags are timing and must never be shown.
 */
@RunWith(RobolectricTestRunner::class)
class LyricsEndTimestampTest {

    @Test
    fun `trailing end timestamp is not shown and becomes the line end`() {
        val line = LyricsUtils.parseLyrics("[00:07.28]I love you[00:08.00]\n[00:10.00]Next").first()

        assertEquals("I love you", line.text)
        assertEquals(7280L, line.time)
        assertEquals(8000L, line.endTime)
        assertNull("a single timed part stays a plain line", line.words)
    }

    @Test
    fun `line is no longer active after its end timestamp`() {
        val lines = LyricsUtils.parseLyrics("[00:07.28]I love you[00:08.00]\n[00:10.00]Next")

        assertTrue(0 in LyricsUtils.findActiveLineIndices(lines, 7500L))
        assertFalse(0 in LyricsUtils.findActiveLineIndices(lines, 9000L))
    }

    @Test
    fun `inline timestamps become word timings`() {
        val line = LyricsUtils.parseLyrics("[00:07.00]I [00:07.50]love you[00:08.50]").first()

        assertEquals("I love you", line.text)
        val words = line.words
        assertNotNull(words)
        assertEquals(listOf("I", "love", "you"), words!!.map { it.text })
        assertEquals(7.0, words[0].startTime, 0.001)
        assertEquals(7.5, words[1].startTime, 0.001)
        assertEquals(8.5, words.last().endTime, 0.001)
        assertFalse(words.last().hasTrailingSpace)
    }

    @Test
    fun `repeated lines drop the tag too`() {
        val lines = LyricsUtils.parseLyrics("[00:07.28][00:20.00]I love you[00:08.00]")

        assertEquals(2, lines.size)
        assertTrue(lines.all { it.text == "I love you" })
    }

    @Test
    fun `ordinary lines are unchanged`() {
        val line = LyricsUtils.parseLyrics("[00:07.28]I love you").first()

        assertEquals("I love you", line.text)
        assertNull(line.endTime)
    }
}
