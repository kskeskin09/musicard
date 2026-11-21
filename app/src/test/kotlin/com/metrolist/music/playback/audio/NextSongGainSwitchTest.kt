/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("DEPRECATION")
class NextSongGainSwitchTest {
    @Test
    fun `next song gain switches at the new stream's first sample`() {
        val processor = configured()
        processor.setTargetGain(0)
        processor.enabled = true
        processor.setNextSongGain(-602) // about half volume

        assertEquals(1000, firstSampleOut(processor)) // still the current song

        processor.onNewStreamConfigured()
        processor.flush() // the next song's audio starts here
        assertNear(500, firstSampleOut(processor), 2)
    }

    @Test
    fun `a flush without a new stream keeps the current gain`() {
        val processor = configured()
        processor.enabled = true
        processor.setNextSongGain(-602)

        processor.flush() // a seek inside the current song
        assertEquals(1000, firstSampleOut(processor))
    }

    @Test
    fun `cleared next song gain never switches`() {
        val processor = configured()
        processor.enabled = true
        processor.setNextSongGain(-602)
        processor.clearNextSongGain()

        processor.onNewStreamConfigured()
        processor.flush()
        assertEquals(1000, firstSampleOut(processor))
    }

    private fun configured() =
        VolumeNormalizationAudioProcessor().apply {
            configure(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
            flush()
        }

    private fun firstSampleOut(processor: VolumeNormalizationAudioProcessor): Int {
        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(1000)
            putShort(1000)
            flip()
        }
        processor.queueInput(input)
        return processor.output.order(ByteOrder.LITTLE_ENDIAN).getShort().toInt()
    }

    private fun assertNear(expected: Int, actual: Int, tolerance: Int) {
        assert(kotlin.math.abs(expected - actual) <= tolerance) { "expected $expected±$tolerance, got $actual" }
    }
}
