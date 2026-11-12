/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.HomeSectionOrderKey
import com.metrolist.music.constants.RandomizeHomeOrderKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.ReorderableToggleItem
import com.metrolist.music.ui.component.ReorderableToggleList
import com.metrolist.music.ui.component.ThemedSwitch as Switch
import com.metrolist.music.ui.screens.HomeLayout
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference

/**
 * Content > Edit home screen: hold a section and drag it to change where it shows on Home.
 * Moving a section turns "Randomize home order" off, so the chosen order is the one you see.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLayoutSettings(navController: NavController) {
    val (orderRaw, onOrderChange) = rememberPreference(HomeSectionOrderKey, "")
    val (randomize, onRandomizeChange) = rememberPreference(RandomizeHomeOrderKey, defaultValue = true)
    val order = HomeLayout.parse(orderRaw)

    val titles =
        mapOf(
            HomeLayout.QUICK_PICKS to stringResource(R.string.quick_picks),
            HomeLayout.ALBUMS_FOR_YOU to stringResource(R.string.albums_for_you),
            HomeLayout.RECENTS to stringResource(R.string.home_recents),
            HomeLayout.FORGOTTEN_FAVORITES to stringResource(R.string.forgotten_favorites),
            HomeLayout.FROM_THE_COMMUNITY to stringResource(R.string.from_the_community),
            HomeLayout.DAILY_DISCOVER to stringResource(R.string.your_daily_discover),
            HomeLayout.KEEP_LISTENING to stringResource(R.string.keep_listening),
            HomeLayout.YOUR_MIXES to stringResource(R.string.home_section_your_mixes),
            HomeLayout.SIMILAR to stringResource(R.string.home_section_similar),
            HomeLayout.YOUTUBE to stringResource(R.string.home_section_youtube),
            HomeLayout.MOOD_AND_GENRES to stringResource(R.string.mood_and_genres),
        )
    val icons =
        mapOf(
            HomeLayout.QUICK_PICKS to painterResource(R.drawable.music_note),
            HomeLayout.ALBUMS_FOR_YOU to painterResource(R.drawable.album),
            HomeLayout.RECENTS to painterResource(R.drawable.history),
            HomeLayout.FORGOTTEN_FAVORITES to painterResource(R.drawable.favorite),
            HomeLayout.FROM_THE_COMMUNITY to painterResource(R.drawable.group),
            HomeLayout.DAILY_DISCOVER to painterResource(R.drawable.discover_tune),
            HomeLayout.KEEP_LISTENING to painterResource(R.drawable.restore),
            HomeLayout.YOUR_MIXES to painterResource(R.drawable.queue_music),
            HomeLayout.SIMILAR to painterResource(R.drawable.playlist_play),
            HomeLayout.YOUTUBE to painterResource(R.drawable.explore_outlined),
            HomeLayout.MOOD_AND_GENRES to painterResource(R.drawable.palette),
        )
    val rows =
        order.map { id ->
            ReorderableToggleItem(id = id, title = titles[id] ?: id, icon = icons[id])
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
        // Randomize home order, kept here too because it decides whether the order below is used.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { onRandomizeChange(!randomize) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.shuffle),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.randomize_home_order),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.edit_home_screen_random_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = randomize,
                onCheckedChange = onRandomizeChange,
                thumbContent = {
                    Icon(
                        painter = painterResource(if (randomize) R.drawable.check else R.drawable.close),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.edit_home_screen_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
            )
            ReorderableToggleList(
                items = rows,
                onReorder = { ids ->
                    onOrderChange(HomeLayout.serialize(ids))
                    if (randomize) onRandomizeChange(false)
                },
                onToggle = { _, _ -> },
                rowColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        }

        if (order != HomeLayout.DefaultOrder) {
            TextButton(
                onClick = { onOrderChange("") },
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.edit_home_screen_reset))
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.edit_home_screen)) },
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
