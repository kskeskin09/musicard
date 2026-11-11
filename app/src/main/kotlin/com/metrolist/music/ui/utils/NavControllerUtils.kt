/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.navigation.NavController
import com.metrolist.music.ui.screens.Screens

fun NavController.backToMain() {
    val mainRoutes = Screens.MainScreens.map { it.route }

    while (previousBackStackEntry != null &&
        currentBackStackEntry?.destination?.route !in mainRoutes
    ) {
        popBackStack()
    }
}

/** This entry's route with its arguments filled in, e.g. "album/MPREb_x" for "album/{albumId}". */
fun androidx.navigation.NavBackStackEntry.resolvedRoute(): String? =
    destination.route?.let { pattern ->
        Regex("""\{(\w+)\}""").replace(pattern) { match ->
            @Suppress("DEPRECATION")
            arguments?.get(match.groupValues[1])?.toString().orEmpty()
        }
    }
