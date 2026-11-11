/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.metrolist.music.ui.utils

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable as navComposable

/** The shared-transition area around the app's pages (set around the NavHost). */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** The page's own enter / exit animation, for elements that travel between pages. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Album covers seen on cards, so the album page can show the cover on its first frame. */
object AlbumCoverPreviewCache {
    private val covers = android.util.LruCache<String, String>(300)

    operator fun get(albumId: String): String? = covers.get(albumId)

    fun put(
        albumId: String,
        url: String?,
    ) {
        if (url != null) covers.put(albumId, url)
    }
}

/**
 * Makes an album cover travel: tapped on a card, it grows from the card into its place on the
 * album page (and shrinks back on the way out).
 */
@Composable
fun Modifier.albumCoverSharedElement(
    albumId: String,
    thumbnailUrl: String? = null,
): Modifier {
    AlbumCoverPreviewCache.put(albumId, thumbnailUrl)
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedScope.current ?: return this
    return with(sharedScope) {
        this@albumCoverSharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key = "album_cover_$albumId"),
            animatedVisibilityScope = animatedScope,
        )
    }
}

/** Navigation's composable, also handing the page's animation to [LocalNavAnimatedScope]. */
fun NavGraphBuilder.composable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = enterTransition,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = exitTransition,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    navComposable(
        route = route,
        arguments = arguments,
        deepLinks = deepLinks,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
    ) { entry: NavBackStackEntry ->
        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
            content(entry)
        }
    }
}
