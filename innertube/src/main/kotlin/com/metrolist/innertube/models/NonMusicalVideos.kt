package com.metrolist.innertube.models

/**
 * "Hide non-musical videos": official music videos stay, other videos (vlogs, trailers,
 * interviews, fan uploads) are left out. Applied wherever YouTube Shorts are filtered, so search,
 * home and artist pages follow it without each screen having to read the setting.
 */
object NonMusicalVideos {
    @Volatile
    var hide: Boolean = false

    private val NON_MUSICAL_TYPES =
        setOf(
            "MUSIC_VIDEO_TYPE_UGC",
            "MUSIC_VIDEO_TYPE_VIDEO",
            "MUSIC_VIDEO_TYPE_LIVE",
        )

    fun isNonMusical(item: YTItem): Boolean =
        item is SongItem && !item.isEpisode && item.musicVideoType in NON_MUSICAL_TYPES
}
