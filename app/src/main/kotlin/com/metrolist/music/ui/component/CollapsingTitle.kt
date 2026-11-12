/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Whether a page's big title has scrolled up under the top bar. The page marks its title with
 * [collapsingTitleAnchor]; the top bar shows [CollapsingTopBarTitle], which appears only then.
 */
@Stable
class CollapsingTitleState(
    private val listState: LazyListState?,
    private val headerIndex: Int,
) {
    internal var anchorHidden by mutableStateOf(false)

    val titleHidden: Boolean
        get() = anchorHidden || (listState != null && listState.firstVisibleItemIndex > headerIndex)
}

val LocalCollapsingTitle = compositionLocalOf<CollapsingTitleState?> { null }

@Composable
fun rememberCollapsingTitleState(
    listState: LazyListState?,
    headerIndex: Int = 0,
): CollapsingTitleState = remember(listState, headerIndex) { CollapsingTitleState(listState, headerIndex) }

/** Gives the page's title-tracking [state] to everything inside [content]. */
@Composable
fun ProvideCollapsingTitle(
    state: CollapsingTitleState,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCollapsingTitle provides state, content = content)
}

/** Marks the page's big title, so the top bar knows when it has scrolled out of sight. */
@Composable
fun Modifier.collapsingTitleAnchor(state: CollapsingTitleState? = LocalCollapsingTitle.current): Modifier {
    state ?: return this
    val density = LocalDensity.current
    val barBottom = WindowInsets.statusBars.getTop(density) + with(density) { 64.dp.toPx() }
    return this.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        // Hidden once its bottom edge has gone above the bottom of the top bar.
        state.anchorHidden = bounds.bottom <= barBottom
    }
}

/** The page's title in the top bar, shown (next to the back arrow) once the big title is gone. */
@Composable
fun CollapsingTopBarTitle(
    text: String,
    state: CollapsingTitleState? = LocalCollapsingTitle.current,
) {
    AnimatedVisibility(
        visible = state?.titleHidden ?: true,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
