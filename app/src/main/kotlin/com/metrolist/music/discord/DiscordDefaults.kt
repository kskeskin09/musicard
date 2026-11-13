package com.metrolist.music.discord

object DiscordDefaults {
    const val YOUTUBE_WATCH_URL = "https://music.youtube.com/watch?v="

    const val BUTTON1_LABEL = "Listen on YouTube Music"
    const val BUTTON1_URL_TEMPLATE = "https://music.youtube.com/watch?v={song.id}"

    const val BUTTON2_LABEL = "Get Musicard"
    const val BUTTON2_URL = "https://kskeskin09.github.io/musicard"

    // Button values saved by builds that still used Metrolist's defaults. They are treated
    // as "default" so existing users get the Musicard button without resetting anything.
    private const val LEGACY_BUTTON2_LABEL = "Visit Metrolist"
    private const val LEGACY_BUTTON2_URL = "https://github.com/MetrolistGroup/Metrolist"

    fun button2Label(saved: String): String =
        if (saved.isBlank() || saved == LEGACY_BUTTON2_LABEL) BUTTON2_LABEL else saved

    fun button2Url(saved: String): String =
        if (saved.isBlank() || saved == LEGACY_BUTTON2_URL) BUTTON2_URL else saved

    const val STATE_TEMPLATE = "{artist.name}"
    const val DETAILS_TEMPLATE = "{song.name}"

    const val ACTIVITY_TYPE = "2"
    const val ACTIVITY_NAME = ""

    const val USER_STATUS = "online"
    const val STATUS_IDLE = "idle"
    const val STATUS_DND = "dnd"

    const val ACTIVITY_TYPE_LISTENING = "2"
    const val ACTIVITY_TYPE_PLAYING = "0"
    const val ACTIVITY_TYPE_WATCHING = "3"
    const val ACTIVITY_TYPE_COMPETING = "5"

    const val UNKNOWN_ARTIST = "Unknown Artist"
    const val UNKNOWN_ALBUM = "Unknown Album"

    const val DISCORD_OAUTH_AUTHORIZE = "https://discord.com/oauth2/authorize"
    const val DISCORD_OAUTH_TOKEN = "https://discord.com/api/v10/oauth2/token"

    const val DISCORD_SCOPES = "openid sdk.social_layer_presence"
}
