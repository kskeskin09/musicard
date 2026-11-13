/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import androidx.annotation.StringRes
import androidx.datastore.preferences.core.Preferences
import com.metrolist.music.R
import com.metrolist.music.constants.EnableBetterLyricsKey
import com.metrolist.music.constants.EnableKugouKey
import com.metrolist.music.constants.EnableLrcLibKey
import com.metrolist.music.constants.EnableLrcRedLyricsKey
import com.metrolist.music.constants.EnableLyricsPlus
import com.metrolist.music.constants.EnableMusixmatchLyricsKey
import com.metrolist.music.constants.EnableNetEaseLyricsKey
import com.metrolist.music.constants.EnablePaxsenixKey
import com.metrolist.music.constants.EnablePortatoLyricsKey
import com.metrolist.music.constants.EnableQQMusicLyricsKey
import com.metrolist.music.constants.EnableSimpMusicLyricsKey
import com.metrolist.music.constants.EnableSpotifyLyricsKey
import com.metrolist.music.constants.EnableUnisonLyricsKey
import com.metrolist.music.constants.EnableYouTubeAutoCaptionsKey
import com.metrolist.music.constants.EnableYouTubeMusicLyricsKey
import com.metrolist.music.constants.EnableYouTubeSubtitleLyricsKey
import com.metrolist.music.constants.EnableZemerKey

/** What the Lyrics providers page shows for one source. */
data class LyricsProviderInfo(
    val id: String,
    val provider: LyricsProvider,
    val displayName: String,
    @StringRes val description: Int,
    val enableKey: Preferences.Key<Boolean>,
    val enabledByDefault: Boolean = true,
)

object LyricsProviderRegistry {
    /**
     * Every source, in the default order: the most reliable and best-timed for English (and
     * other Western) songs first, Asian-focused sources after them, YouTube's own text last.
     */
    val providers: List<LyricsProviderInfo> = listOf(
        LyricsProviderInfo("BetterLyrics", BetterLyricsProvider, "Better Lyrics", R.string.lyrics_src_desc_betterlyrics, EnableBetterLyricsKey),
        LyricsProviderInfo("Paxsenix", PaxsenixLyricsProvider, "Apple Music (Paxsenix)", R.string.lyrics_src_desc_paxsenix, EnablePaxsenixKey),
        LyricsProviderInfo("Musixmatch", MusixmatchLyricsProvider, "Musixmatch (Paxsenix)", R.string.lyrics_src_desc_musixmatch, EnableMusixmatchLyricsKey),
        LyricsProviderInfo("LyricsPlus", LyricsPlusProvider, "LyricsPlus", R.string.lyrics_src_desc_lyricsplus, EnableLyricsPlus),
        LyricsProviderInfo("LrcRed", LrcRedLyricsProvider, "lrc.red", R.string.lyrics_src_desc_lrcred, EnableLrcRedLyricsKey),
        LyricsProviderInfo("Unison", UnisonLyricsProvider, "Unison", R.string.lyrics_src_desc_unison, EnableUnisonLyricsKey),
        LyricsProviderInfo("Spotify", SpotifyLyricsProvider, "Spotify (Paxsenix)", R.string.lyrics_src_desc_spotify, EnableSpotifyLyricsKey),
        LyricsProviderInfo("LrcLib", LrcLibLyricsProvider, "LrcLib", R.string.lyrics_src_desc_lrclib, EnableLrcLibKey),
        LyricsProviderInfo("SimpMusicLyrics", SimpMusicLyricsProvider, "SimpMusic", R.string.lyrics_src_desc_simpmusic, EnableSimpMusicLyricsKey),
        LyricsProviderInfo("NetEase", NetEaseLyricsProvider, "NetEase", R.string.lyrics_src_desc_netease, EnableNetEaseLyricsKey),
        LyricsProviderInfo("Portato", PortatoLyricsProvider, "BetterLyrics Portato", R.string.lyrics_src_desc_portato, EnablePortatoLyricsKey),
        LyricsProviderInfo("KuGou", KuGouLyricsProvider, "KuGou", R.string.lyrics_src_desc_kugou, EnableKugouKey),
        LyricsProviderInfo("QQMusic", QQMusicLyricsProvider, "QQ Music", R.string.lyrics_src_desc_qqmusic, EnableQQMusicLyricsKey),
        LyricsProviderInfo("Zemer", ZemerLyricsProvider, "Zemer", R.string.lyrics_src_desc_zemer, EnableZemerKey),
        LyricsProviderInfo("YouTube", YouTubeLyricsProvider, "YouTube Music", R.string.lyrics_src_desc_youtube, EnableYouTubeMusicLyricsKey),
        LyricsProviderInfo("YouTubeSubtitle", YouTubeSubtitleLyricsProvider, "YouTube Subtitle", R.string.lyrics_src_desc_youtube_subtitle, EnableYouTubeSubtitleLyricsKey),
        LyricsProviderInfo(
            "YouTubeAutoCaptions",
            YouTubeAutoCaptionsLyricsProvider,
            "YouTube Auto Captions",
            R.string.lyrics_src_desc_youtube_auto,
            EnableYouTubeAutoCaptionsKey,
            enabledByDefault = false,
        ),
    )

    private val providerMap = providers.associate { it.id to it.provider }
    private val infoMap = providers.associateBy { it.id }

    val providerNames = providerMap.keys.toList()

    fun getProviderByName(name: String): LyricsProvider? = providerMap[name]

    fun getInfo(name: String): LyricsProviderInfo? = infoMap[name]

    fun getProviderName(provider: LyricsProvider): String? =
        providerMap.entries.find { it.value == provider }?.key

    fun deserializeProviderOrder(orderString: String): List<String> {
        if (orderString.isBlank()) {
            return getDefaultProviderOrder()
        }
        val saved = orderString.split(",").map { it.trim() }.filter { it in providerNames }.distinct()
        return saved + getDefaultProviderOrder().filter { it !in saved }
    }

    fun serializeProviderOrder(providers: List<String>): String {
        return providers.filter { it in providerNames }.joinToString(",")
    }

    fun getDefaultProviderOrder(): List<String> = providers.map { it.id }

    fun getOrderedProviders(orderString: String): List<LyricsProvider> {
        val order = deserializeProviderOrder(orderString)
        return order.mapNotNull { getProviderByName(it) }
    }
}
