/**
 * Musicard (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.metrolist.music.constants.AutoHideNavBarDelay
import kotlinx.coroutines.delay

/**
 * Hides Android's navigation bar (gesture pill or buttons) and/or the status bar (clock, battery)
 * on the full-screen player after the chosen delay. A swipe from the edge brings a bar back for a
 * moment, and it hides again by itself. Leaving the player shows both again right away.
 */
@Composable
fun AutoHideNavigationBar(
    active: Boolean,
    delayOption: AutoHideNavBarDelay,
    statusDelayOption: AutoHideNavBarDelay = AutoHideNavBarDelay.OFF,
) {
    val view = LocalView.current
    val window = (view.context as? Activity)?.window ?: return
    val controller = WindowCompat.getInsetsController(window, view)

    AutoHideBar(controller, WindowInsetsCompat.Type.navigationBars(), active, delayOption)
    AutoHideBar(controller, WindowInsetsCompat.Type.statusBars(), active, statusDelayOption)

    DisposableEffect(Unit) {
        onDispose {
            controller.show(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.statusBars())
        }
    }
}

@Composable
private fun AutoHideBar(
    controller: WindowInsetsControllerCompat,
    type: Int,
    active: Boolean,
    delayOption: AutoHideNavBarDelay,
) {
    val enabled = active && delayOption != AutoHideNavBarDelay.OFF
    // Only show the bar again if this setting hid it, so another screen's own choice is left alone.
    var hidden by remember { mutableStateOf(false) }
    LaunchedEffect(enabled, delayOption) {
        if (enabled) {
            delay(delayOption.millis)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(type)
            hidden = true
        } else if (hidden) {
            controller.show(type)
            hidden = false
        }
    }
}
