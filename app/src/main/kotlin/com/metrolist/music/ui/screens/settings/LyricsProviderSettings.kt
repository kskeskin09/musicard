/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.LyricsProviderOrderV2Key
import com.metrolist.music.constants.SearchBestSyncedLyricsKey
import com.metrolist.music.lyrics.LyricsProviderRegistry
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.ReorderableToggleItem
import com.metrolist.music.ui.component.ReorderableToggleList
import com.metrolist.music.ui.component.ThemedSwitch
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference

/**
 * The lyrics providers on their own page: switch each on or off, and hold one to move it.
 * Higher ones are tried first. Same box and rows as the Android Auto sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsProviderSettings(navController: NavController) {
    val (lyricsProviderOrder, onLyricsProviderOrderChange) =
        rememberPreference(
            key = LyricsProviderOrderV2Key,
            defaultValue = LyricsProviderRegistry.serializeProviderOrder(LyricsProviderRegistry.getDefaultProviderOrder()),
        )
    val (searchBestSynced, onSearchBestSyncedChange) = rememberPreference(key = SearchBestSyncedLyricsKey, defaultValue = true)

    val normalizedOrder = LyricsProviderRegistry.deserializeProviderOrder(lyricsProviderOrder)
    // Every source has a switch, the YouTube ones included.
    val switches =
        LyricsProviderRegistry.providers.associate { info ->
            val (on, onChange) = rememberPreference(key = info.enableKey, defaultValue = info.enabledByDefault)
            info.id to (on to onChange)
        }
    val lyricsIcon = painterResource(R.drawable.lyrics)
    val rows =
        normalizedOrder
            .filter { it in switches }
            .map { name ->
                val info = LyricsProviderRegistry.getInfo(name)
                ReorderableToggleItem(
                    id = name,
                    title = info?.displayName ?: name,
                    icon = lyricsIcon,
                    enabled = switches.getValue(name).first,
                    subtitle = info?.let { stringResource(it.description) },
                )
            }

    Column(
        modifier =
            Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                .verticalScroll(rememberScrollState())
                // Inside the scroll, so the page runs on under the mini player.
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { onSearchBestSyncedChange(!searchBestSynced) }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = stringResource(R.string.lyrics_search_best_synced),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.lyrics_search_best_synced_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ThemedSwitch(
                checked = searchBestSynced,
                onCheckedChange = onSearchBestSyncedChange,
                thumbContent = {
                    Icon(
                        painter = painterResource(if (searchBestSynced) R.drawable.check else R.drawable.close),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
        }
        Column(
            modifier =
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.lyrics_provider_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
            )
            ReorderableToggleList(
                items = rows,
                onReorder = { ids ->
                    // Providers without a switch keep their place after the others.
                    val others = normalizedOrder.filter { it !in switches }
                    onLyricsProviderOrderChange(LyricsProviderRegistry.serializeProviderOrder(ids + others))
                },
                onToggle = { id, on -> switches[id]?.second?.invoke(on) },
                rowColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                rowHeight = 88.dp,
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.lyrics_provider)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
