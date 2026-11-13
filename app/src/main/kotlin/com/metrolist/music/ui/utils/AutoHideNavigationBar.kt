/**
 * Musicard (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.metrolist.music.constants.AutoHideNavBarDelay
import kotlinx.coroutines.delay

/**
 * Hides Android's navigation bar (gesture pill or buttons) on the full-screen player after the
 * chosen delay. A swipe from the bottom edge brings it back for a moment, and it hides again by
 * itself. Leaving the player shows it again right away.
 */
@Composable
fun AutoHideNavigationBar(
    active: Boolean,
    delayOption: AutoHideNavBarDelay,
) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return
    val controller = WindowCompat.getInsetsController(window, view)
    val enabled = active && delayOption != AutoHideNavBarDelay.OFF

    LaunchedEffect(enabled, delayOption) {
        if (enabled) {
            delay(delayOption.millis)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose { controller.show(WindowInsetsCompat.Type.navigationBars()) }
    }
}
