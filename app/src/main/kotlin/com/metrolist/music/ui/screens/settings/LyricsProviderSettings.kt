/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.EnableBetterLyricsKey
import com.metrolist.music.constants.EnableKugouKey
import com.metrolist.music.constants.EnableLrcLibKey
import com.metrolist.music.constants.EnableLyricsPlus
import com.metrolist.music.constants.EnablePaxsenixKey
import com.metrolist.music.constants.EnableZemerKey
import com.metrolist.music.constants.LyricsProviderOrderKey
import com.metrolist.music.lyrics.LyricsProviderRegistry
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.ReorderableToggleItem
import com.metrolist.music.ui.component.ReorderableToggleList
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference

private val ProviderDisplayNames =
    mapOf(
        "BetterLyrics" to "Better Lyrics",
        "Paxsenix" to "Paxsenix",
        "LrcLib" to "LrcLib",
        "KuGou" to "KuGou",
        "LyricsPlus" to "LyricsPlus",
        "Zemer" to "Zemer",
    )

/**
 * The lyrics providers on their own page: switch each on or off, and hold one to move it.
 * Higher ones are tried first. Same box and rows as the Android Auto sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsProviderSettings(navController: NavController) {
    val (enableZemer, onEnableZemerChange) = rememberPreference(key = EnableZemerKey, defaultValue = true)
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (enableBetterLyrics, onEnableBetterLyricsChange) = rememberPreference(key = EnableBetterLyricsKey, defaultValue = true)
    val (enablePaxsenix, onEnablePaxsenixChange) = rememberPreference(key = EnablePaxsenixKey, defaultValue = true)
    val (enableLyricsPlus, onEnableLyricsPlusChange) = rememberPreference(key = EnableLyricsPlus, defaultValue = true)
    val (lyricsProviderOrder, onLyricsProviderOrderChange) =
        rememberPreference(
            key = LyricsProviderOrderKey,
            defaultValue = LyricsProviderRegistry.serializeProviderOrder(LyricsProviderRegistry.getDefaultProviderOrder()),
        )

    val currentOrder = LyricsProviderRegistry.deserializeProviderOrder(lyricsProviderOrder)
    val defaultOrder = LyricsProviderRegistry.getDefaultProviderOrder()
    val normalizedOrder = currentOrder.filter { it in defaultOrder } + defaultOrder.filter { it !in currentOrder }
    val switches =
        mapOf(
            "LrcLib" to (enableLrclib to onEnableLrclibChange),
            "KuGou" to (enableKugou to onEnableKugouChange),
            "BetterLyrics" to (enableBetterLyrics to onEnableBetterLyricsChange),
            "Paxsenix" to (enablePaxsenix to onEnablePaxsenixChange),
            "LyricsPlus" to (enableLyricsPlus to onEnableLyricsPlusChange),
            "Zemer" to (enableZemer to onEnableZemerChange),
        )
    val lyricsIcon = painterResource(R.drawable.lyrics)
    val rows =
        normalizedOrder
            .filter { it in switches }
            .map { name ->
                ReorderableToggleItem(
                    id = name,
                    title = ProviderDisplayNames[name] ?: name,
                    icon = lyricsIcon,
                    enabled = switches.getValue(name).first,
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
        Column(
            modifier =
                Modifier
                    .padding(top = 8.dp)
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
            )
        }
        Text(
            text = stringResource(R.string.youtube_music_lyrics_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
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
