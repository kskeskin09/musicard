/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

/**
 * Keeps a horizontal slider's drag to itself. Once the finger has moved more sideways than up or
 * down, every later move of that touch is marked as used, so a sheet, pager or list around the
 * slider does not also slide while the slider is being dragged, however much the finger drifts
 * up or down. A touch that starts mostly vertical is left alone, so the page can still scroll.
 *
 * Put it first in the slider's modifier chain: it then sees each move after the slider has
 * handled it, and before any parent does.
 */
fun Modifier.lockSliderDrag(enabled: Boolean = true): Modifier =
    if (!enabled) {
        this
    } else {
        this.pointerInput(Unit) {
            // Decide well before a parent's own touch slop is reached.
            val decideDistance = viewConfiguration.touchSlop / 2f
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                var totalX = 0f
                var totalY = 0f
                var owned = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.changes.fastAll { !it.pressed }) break
                    if (owned) {
                        event.changes.fastForEach { if (it.positionChange() != Offset.Zero) it.consume() }
                        continue
                    }
                    event.changes.fastForEach {
                        val delta = it.positionChange()
                        totalX += delta.x
                        totalY += delta.y
                    }
                    if (abs(totalX) >= decideDistance || abs(totalY) >= decideDistance) {
                        if (abs(totalX) >= abs(totalY)) {
                            owned = true
                            event.changes.fastForEach { it.consume() }
                        } else {
                            // Mostly vertical: let the page scroll or the sheet move.
                            break
                        }
                    }
                }
            }
        }
    }
