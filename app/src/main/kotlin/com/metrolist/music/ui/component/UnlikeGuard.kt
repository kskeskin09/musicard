/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.metrolist.music.R
import com.metrolist.music.constants.ReLikeProtectionKey
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.delay

/**
 * Re-Like Protection: removing a like takes two taps. The first tap cracks and shakes the heart;
 * a second tap within three seconds removes the like, otherwise the heart heals and the song stays
 * liked.
 */
@Stable
class UnlikeGuard internal constructor(
    private val enabled: () -> Boolean,
) {
    /** The heart is cracked and waiting for the second tap. */
    var armed by mutableStateOf(false)
        internal set

    /** Call on a tap; returns true when the like should really be toggled. */
    fun onTap(liked: Boolean): Boolean {
        if (liked && enabled() && !armed) {
            armed = true
            return false
        }
        armed = false
        return true
    }

    /** The heart icon to show for a liked / not liked song. */
    fun icon(liked: Boolean): Int =
        when {
            liked && armed -> R.drawable.heart_broken
            liked -> R.drawable.favorite
            else -> R.drawable.favorite_border
        }
}

@Composable
fun rememberUnlikeGuard(liked: Boolean): UnlikeGuard {
    val protection by rememberPreference(ReLikeProtectionKey, defaultValue = true)
    val guard = remember { UnlikeGuard { protection } }
    LaunchedEffect(guard.armed) {
        if (guard.armed) {
            delay(UnlikeConfirmMillis)
            guard.armed = false
        }
    }
    LaunchedEffect(liked) {
        if (!liked) guard.armed = false
    }
    return guard
}

/** Rotation (degrees) of a short shake played when the heart cracks. */
@Composable
fun rememberCrackShake(guard: UnlikeGuard): Animatable<Float, *> {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(guard.armed) {
        if (guard.armed) {
            shake.animateTo(
                0f,
                keyframes {
                    durationMillis = 420
                    -14f at 60
                    12f at 140
                    -9f at 220
                    6f at 300
                    -3f at 370
                },
            )
        }
    }
    return shake
}

private const val UnlikeConfirmMillis = 3000L
