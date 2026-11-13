/**
 * Musicard (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.metrolist.music.R
import kotlin.math.abs

/** The times the sleep timer slider stops at, in minutes. */
internal val SleepTimerSteps = listOf(5, 10, 15, 20, 30, 45, 60, 75, 90, 120, 150, 180, 240, 300)

/** Slider position of the step closest to [minutes] (an older default may sit between steps). */
internal fun sleepTimerStepIndex(minutes: Float): Int =
    SleepTimerSteps.indices.minBy { abs(SleepTimerSteps[it] - minutes) }

internal fun sleepTimerStepMinutes(position: Float): Float =
    SleepTimerSteps[position.let { kotlin.math.round(it).toInt() }.coerceIn(SleepTimerSteps.indices)].toFloat()

/** "45 minutes", "2 hours" or "1 hour 30 minutes". */
@Composable
internal fun sleepTimerDurationText(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    val hourText = if (hours > 0) pluralStringResource(R.plurals.sleep_timer_hours, hours, hours) else null
    val minuteText = if (rest > 0 || hours == 0) pluralStringResource(R.plurals.minute, rest, rest) else null
    return listOfNotNull(hourText, minuteText).joinToString(" ")
}
