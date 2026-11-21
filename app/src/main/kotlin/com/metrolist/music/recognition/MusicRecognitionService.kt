/**
 * Music Recognition Feature
 * 
 * This feature is based on the original MusicRecognizer project by Aleksey Saenko.
 * Original project: https://github.com/aleksey-saenko/MusicRecognizer
 * 
 * Special thanks to Aleksey Saenko for the music recognition implementation.
 */

package com.metrolist.music.recognition

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.metrolist.shazamkit.Shazam
import com.metrolist.shazamkit.models.RecognitionResult
import com.metrolist.shazamkit.models.RecognitionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder

/**
 * Service for recognizing music using audio fingerprinting.
 * Records audio from the microphone, generates a Shazam-compatible fingerprint,
 * and sends it to the Shazam API for recognition.
 */
object MusicRecognitionService {
    
    // Recording parameters
    private const val RECORDING_SAMPLE_RATE = 44100
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private const val BYTES_PER_SAMPLE = 2
    // Record at most 12 seconds, but ask Shazam at 4 s and 8 s too so that a clear song is
    // found sooner and a hard one still gets the full 12 seconds.
    private const val RECORDING_DURATION_MS = 12000L
    private val ATTEMPT_POINTS_MS = longArrayOf(4000L, 8000L, RECORDING_DURATION_MS)
    private const val TAG = "MusicRecognitionService"

    private val _recognitionStatus = MutableStateFlow<RecognitionStatus>(RecognitionStatus.Ready)
    val recognitionStatus: StateFlow<RecognitionStatus> = _recognitionStatus.asStateFlow()

    // Live microphone loudness while listening, 0f (silence) .. 1f (loud).
    // Drives the sound-wave animation on the recognition screen.
    private val _micLevel = MutableStateFlow(0f)
    val micLevel: StateFlow<Float> = _micLevel.asStateFlow()

    /**
     * Set to true by the widget service after it has already persisted the result to the
     * database, so that [RecognitionScreen] skips the duplicate insert.
     * Reset to false by [reset].
     */
    var resultSavedExternally: Boolean = false
    
    fun hasRecordPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start the music recognition process.
     *
     * Records up to [RECORDING_DURATION_MS] of audio and, while it is still recording, asks
     * Shazam at each of [ATTEMPT_POINTS_MS] with everything heard so far. The first match
     * ends the recording early; a later attempt has more audio, so it can still match a
     * song an earlier one missed.
     */
    @SuppressLint("MissingPermission")
    suspend fun recognize(context: Context): RecognitionStatus = withContext(Dispatchers.IO) {
        if (!hasRecordPermission(context)) {
            Timber.tag(TAG).w("Microphone permission not granted, aborting recognition")
            return@withContext RecognitionStatus.Error("Microphone permission not granted")
        }

        _recognitionStatus.value = RecognitionStatus.Listening
        Timber.tag(TAG).d("Starting music recognition")

        try {
            coroutineScope {
                val recording = ByteArrayOutputStream()
                val recorder = launch(Dispatchers.IO) { recordAudio(context, recording) }

                var lastFailure: RecognitionStatus? = null
                var lastAttemptBytes = -1
                for ((index, pointMs) in ATTEMPT_POINTS_MS.withIndex()) {
                    val isLastAttempt = index == ATTEMPT_POINTS_MS.lastIndex
                    val neededBytes = bytesFor(pointMs)
                    while (recorder.isActive && synchronized(recording) { recording.size() } < neededBytes) {
                        delay(100)
                    }
                    if (isLastAttempt) {
                        recorder.join()
                        _recognitionStatus.value = RecognitionStatus.Processing
                    }
                    val audioData = synchronized(recording) { recording.toByteArray() }
                    Timber.tag(TAG).d("Attempt %d at %dms with %d bytes", index + 1, pointMs, audioData.size)
                    // Skip a repeat of the same audio (recording ended early or a slow request).
                    if (audioData.isEmpty() || audioData.size == lastAttemptBytes) continue
                    lastAttemptBytes = audioData.size

                    when (val status = identify(audioData)) {
                        is RecognitionStatus.Success -> {
                            recorder.cancel()
                            _recognitionStatus.value = status
                            return@coroutineScope
                        }
                        else -> lastFailure = status
                    }
                }
                _recognitionStatus.value = lastFailure
                    ?: RecognitionStatus.Error("No audio was recorded")
            }
            _recognitionStatus.value
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Recognition failed with exception")
            _recognitionStatus.value = RecognitionStatus.Error(e.message ?: "Recognition failed")
            _recognitionStatus.value
        } finally {
            _micLevel.value = 0f
        }
    }

    private fun bytesFor(durationMs: Long): Int =
        (RECORDING_SAMPLE_RATE.toLong() * BYTES_PER_SAMPLE * durationMs / 1000L).toInt()

    /** Resamples [audioData], fingerprints it and asks Shazam. Never throws except on cancel. */
    private suspend fun identify(audioData: ByteArray): RecognitionStatus {
        val decodedAudio = DecodedAudio(
            data = audioData,
            channelCount = 1,
            sampleRate = RECORDING_SAMPLE_RATE,
            pcmEncoding = AUDIO_FORMAT
        )

        val resampledAudio = AudioResampler.resample(
            decodedAudio,
            VibraSignature.REQUIRED_SAMPLE_RATE
        ).getOrElse { error ->
            Timber.tag(TAG).e(error, "Audio resampling failed")
            return RecognitionStatus.Error("Failed to resample audio: ${error.message}")
        }

        val formatOk = resampledAudio.channelCount == 1 &&
            resampledAudio.sampleRate == VibraSignature.REQUIRED_SAMPLE_RATE &&
            resampledAudio.pcmEncoding == AudioFormat.ENCODING_PCM_16BIT &&
            ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN &&
            resampledAudio.data.isNotEmpty() &&
            resampledAudio.data.size % 2 == 0
        if (!formatOk) return RecognitionStatus.Error("Invalid audio format for fingerprint generation")

        val signature = try {
            VibraSignature.fromI16(resampledAudio.data)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Fingerprint generation failed")
            return RecognitionStatus.Error("Failed to generate fingerprint: ${e.message}")
        }

        val sampleDurationMs = (resampledAudio.data.size / 2) * 1000L / VibraSignature.REQUIRED_SAMPLE_RATE
        Timber.tag(TAG).d("Fingerprint generated, sampleDurationMs=%d", sampleDurationMs)

        return try {
            Shazam.recognize(signature, sampleDurationMs).fold(
                onSuccess = { recognitionResult ->
                    Timber.tag(TAG).i("Recognition successful: '%s' by %s", recognitionResult.title, recognitionResult.artist)
                    RecognitionStatus.Success(recognitionResult)
                },
                onFailure = { error ->
                    val message = error.message ?: "Unknown error"
                    Timber.tag(TAG).w(error, "Recognition API returned failure: %s", message)
                    if (message.contains("No match", ignoreCase = true)) {
                        RecognitionStatus.NoMatch("No matches found. Try again with clearer audio.")
                    } else {
                        RecognitionStatus.Error(message)
                    }
                }
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Recognition request failed")
            RecognitionStatus.Error(e.message ?: "Recognition failed")
        }
    }

    /**
     * Opens the microphone with the cleanest source the phone offers. The default MIC source
     * applies noise suppression and automatic gain meant for voice, which distorts music and
     * hurts fingerprint matching; UNPROCESSED (or VOICE_RECOGNITION) skips that processing.
     */
    @SuppressLint("MissingPermission")
    private fun openAudioRecord(context: Context, bufferSize: Int): AudioRecord {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val unprocessedSupported =
            audioManager?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val sources = buildList {
            if (unprocessedSupported) add(MediaRecorder.AudioSource.UNPROCESSED)
            add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            add(MediaRecorder.AudioSource.MIC)
        }
        for (source in sources) {
            val record = try {
                AudioRecord(source, RECORDING_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Audio source %d unavailable", source)
                continue
            }
            if (record.state == AudioRecord.STATE_INITIALIZED) {
                Timber.tag(TAG).d("Recording with audio source %d", source)
                return record
            }
            record.release()
        }
        throw IllegalStateException("Could not open the microphone")
    }

    @SuppressLint("MissingPermission")
    private suspend fun recordAudio(context: Context, output: ByteArrayOutputStream) = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(
            RECORDING_SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        Timber.tag(TAG).d("Recording audio: sampleRate=%d, bufferSize=%d, durationMs=%d", RECORDING_SAMPLE_RATE, bufferSize, RECORDING_DURATION_MS)

        val audioRecord = openAudioRecord(context, bufferSize)
        val buffer = ByteArray(bufferSize)
        val maxBytes = bytesFor(RECORDING_DURATION_MS)

        try {
            audioRecord.startRecording()
            while (isActive && synchronized(output) { output.size() } < maxBytes) {
                val bytesRead = audioRecord.read(buffer, 0, bufferSize)
                if (bytesRead > 0) {
                    synchronized(output) { output.write(buffer, 0, bytesRead) }
                    _micLevel.value = computeLevel(buffer, bytesRead)
                } else if (bytesRead < 0) {
                    Timber.tag(TAG).w("AudioRecord.read failed: %d", bytesRead)
                    break
                }
            }
        } finally {
            _micLevel.value = 0f
            audioRecord.stop()
            audioRecord.release()
        }
        Timber.tag(TAG).d("Audio recording complete: %d bytes collected", synchronized(output) { output.size() })
    }

    /**
     * RMS loudness of a little-endian 16-bit PCM chunk, mapped from -55..-10 dBFS to 0..1.
     */
    private fun computeLevel(buffer: ByteArray, length: Int): Float {
        val samples = length / 2
        if (samples == 0) return 0f
        var sum = 0.0
        var i = 0
        while (i + 1 < length) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort().toDouble()
            sum += sample * sample
            i += 2
        }
        val rms = kotlin.math.sqrt(sum / samples)
        if (rms < 1.0) return 0f
        val db = 20.0 * kotlin.math.log10(rms / 32768.0)
        return ((db + 55.0) / 45.0).coerceIn(0.0, 1.0).toFloat()
    }

    fun reset() {
        Timber.tag(TAG).d("Recognition state reset")
        _recognitionStatus.value = RecognitionStatus.Ready
        _micLevel.value = 0f
        resultSavedExternally = false
    }
}
