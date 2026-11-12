/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import java.time.LocalDate

/**
 * Daily listening streak from the days with plays ([days] as days since 1970, any order).
 * [current] counts back from today, or from yesterday while today has no play yet (so the
 * streak isn't lost before the day is over); [listenedToday] tells which of the two it is.
 */
data class ListeningStreak(
    val current: Int,
    val listenedToday: Boolean,
    val longest: Int,
) {
    companion object {
        fun from(days: List<Long>, today: Long = LocalDate.now().toEpochDay()): ListeningStreak {
            if (days.isEmpty()) return ListeningStreak(0, false, 0)
            val set = days.toHashSet()
            val listenedToday = today in set
            var day = if (listenedToday) today else today - 1
            var current = 0
            while (day in set) {
                current++
                day--
            }
            var longest = 0
            var run = 0
            var previous = Long.MIN_VALUE
            for (d in days.sorted()) {
                run = if (d == previous + 1) run + 1 else 1
                if (run > longest) longest = run
                previous = d
            }
            return ListeningStreak(current, listenedToday, longest)
        }
    }
}
