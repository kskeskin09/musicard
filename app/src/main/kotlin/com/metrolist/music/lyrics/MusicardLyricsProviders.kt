/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import com.metrolist.innertube.YouTube
import com.metrolist.music.constants.EnableLrcRedLyricsKey
import com.metrolist.music.constants.EnableMusixmatchLyricsKey
import com.metrolist.music.constants.EnableNetEaseLyricsKey
import com.metrolist.music.constants.EnablePortatoLyricsKey
import com.metrolist.music.constants.EnableQQMusicLyricsKey
import com.metrolist.music.constants.EnableSimpMusicLyricsKey
import com.metrolist.music.constants.EnableSpotifyLyricsKey
import com.metrolist.music.constants.EnableUnisonLyricsKey
import com.metrolist.music.constants.EnableYouTubeAutoCaptionsKey
import com.metrolist.music.lyrics.sources.CommunityTtmlLyrics
import com.metrolist.music.lyrics.sources.NetEaseLyrics
import com.metrolist.music.lyrics.sources.PaxsenixExtraLyrics
import com.metrolist.music.lyrics.sources.QQMusicLyrics
import com.metrolist.music.lyrics.sources.SimpMusicLyrics
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A source with an on/off switch, run off the main thread, that never throws. */
abstract class SwitchableLyricsProvider(
    override val name: String,
    private val enableKey: Preferences.Key<Boolean>,
    private val enabledByDefault: Boolean = true,
) : LyricsProvider {
    override fun isEnabled(context: Context): Boolean = context.dataStore[enableKey] ?: enabledByDefault

    protected abstract suspend fun fetch(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String>

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            fetch(id, title, artist, duration, album)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

object SimpMusicLyricsProvider : SwitchableLyricsProvider("SimpMusic", EnableSimpMusicLyricsKey) {
    override suspend fun fetch(id: String, title: String, artist: String, duration: Int, album: String?) =
        SimpMusicLyrics.getLyrics(id, title, artist, duration)
}

object NetEaseLyricsProvider : SwitchableLyricsProvider("NetEase", EnableNetEaseLyricsKey) {
    override suspend fun fetch(id: String, title: String, artist: String, duration: Int, album: String?) =
        NetEaseLyrics.getLyrics(title, artist, duration)
}

object QQMusicLyricsProvider : SwitchableLyricsProvider("QQ Music", EnableQQMusicLyricsKey) {
    override suspend fun fetch(id: String, title: String, artist: String, duration: Int, album: String?) =
        QQMusicLyrics.getLyrics(title, artist, duration)
}

object MusixmatchLyricsProvider : SwitchableLyricsProvider("Musixmatch", EnableMusixmatchLyricsKey) {
    override suspend fun fetch(id: String, title: String, artist: String, duration: Int, album: String?) =
        PaxsenixExtraLyrics.musixmatch(title, artist, duration)
}

object SpotifyLyricsProvider : SwitchableLyricsProvider("Spotify", EnableSpotifyLyricsKey) {
    override suspend fun fetch(id: String, title: String, artist: String, duration: Int, album: String?) =
        PaxsenixExtraLyrics.spotify(title, artist, duration)
}

object UnisonLyricsProvider : SwitchableLyricsProvider("Unison", EnableUnisonLyricsKey) {
    override suspend fun fetch(id: String, title: String, artist: String, duration: Int, album: String?) =
        CommunityTtmlLyrics.unison(title, artist, duration, album)
}

object PortatoLyricsProvider : SwitchableLyricsProvider("BetterLyrics Portato", EnablePortatoLyricsKey) {
    override suspend fun fetch(id: String, title: String, artist: String, duration: Int, album: String?) =
        CommunityTtmlLyrics.portato(title, artist, duration, album)
}

object LrcRedLyricsProvider : SwitchableLyricsProvider("lrc.red", EnableLrcRedLyricsKey) {
    override suspend fun fetch(id: String, title: String, artist: String, duration: Int, album: String?) =
        CommunityTtmlLyrics.lrcRed(title, artist, duration)
}

/** Off by default: speech-recognition captions are a last resort when nothing else has the song. */
object YouTubeAutoCaptionsLyricsProvider :
    SwitchableLyricsProvider("YouTube Auto Captions", EnableYouTubeAutoCaptionsKey, enabledByDefault = false) {
    override suspend fun fetch(id: String, title: String, artist: String, duration: Int, album: String?) =
        YouTube.autoCaptions(id)
}
