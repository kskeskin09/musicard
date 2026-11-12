/**
 * Musicard (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.constants

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.metrolist.music.R

/** A button the user can place on the playback notification. */
enum class NotificationButton(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    PREVIOUS(R.string.previous, R.drawable.skip_previous),
    NEXT(R.string.next, R.drawable.skip_next),
    LIKE(R.string.like, R.drawable.ic_heart_outline),
    REPEAT(R.string.repeat, R.drawable.repeat),
    SHUFFLE(R.string.shuffle, R.drawable.shuffle),
    SEEK_BACK_5(R.string.seek_back_5, R.drawable.replay_5),
    SEEK_FORWARD_5(R.string.seek_forward_5, R.drawable.forward_5),
    START_RADIO(R.string.start_radio, R.drawable.radio),
    ADD_TO_PLAYLIST(R.string.android_auto_target_playlist, R.drawable.playlist_add),
}

/**
 * The four configurable slots of the notification, around the fixed play/pause button:
 * index 0 is left of play (where "previous" normally is), 1 is right of play (where "next" normally is),
 * 2 and 3 are the two extra buttons after it.
 */
object NotificationButtons {
    const val SLOT_COUNT = 4

    val Default = listOf(
        NotificationButton.PREVIOUS,
        NotificationButton.NEXT,
        NotificationButton.LIKE,
        NotificationButton.REPEAT,
    )

    fun parse(raw: String?): List<NotificationButton> {
        if (raw.isNullOrBlank()) return Default
        val parsed = raw.split(',').mapNotNull { name ->
            NotificationButton.entries.firstOrNull { it.name == name.trim() }
        }
        // Reject anything malformed: wrong size or duplicates.
        return if (parsed.size == SLOT_COUNT && parsed.toSet().size == SLOT_COUNT) parsed else Default
    }

    fun serialize(buttons: List<NotificationButton>): String = buttons.joinToString(",") { it.name }
}
