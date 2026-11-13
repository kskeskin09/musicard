/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.metrolist.music.ui.theme.pageRaisedColor

/**
 * The round pull-to-refresh indicator in the app's own colors: the same raised color as the
 * round buttons on the page, with the spinner in the text color, instead of Material's tinted
 * defaults.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    PullToRefreshDefaults.Indicator(
        state = state,
        isRefreshing = isRefreshing,
        modifier = modifier,
        containerColor = pageRaisedColor(),
        color = MaterialTheme.colorScheme.onBackground,
    )
}
