/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import java.time.LocalDateTime

/** When a listen happened and how long it lasted (ms), without the song. */
data class EventTime(
    val timestamp: LocalDateTime,
    val playTime: Long,
)
