/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.launch

private const val LyricsPage = 0
private const val UpNextPage = 1

/**
 * Card at the bottom of the portrait player. It reaches the bottom edge of the screen.
 * Page 0 shows the synced lyrics; swiping left shows the upcoming songs at the same size.
 */
@Composable
fun PlayerBottomCard(
    mediaMetadata: MediaMetadata?,
    positionProvider: () -> Long,
    isPlayerExpanded: Boolean,
    isPlaying: Boolean,
    lyricsExpanded: Boolean,
    contentColor: Color,
    isListenTogetherGuest: Boolean,
    sleepTimerEnabled: Boolean,
    onSleepTimerClick: () -> Unit,
    onLyricsMenuClick: () -> Unit,
    onOpenFullQueue: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    LaunchedEffect(lyricsExpanded) {
        if (lyricsExpanded) pagerState.animateScrollToPage(LyricsPage)
    }

    Column(
        modifier =
            modifier
                .clip(shape)
                .background(contentColor.copy(alpha = 0.08f))
                .border(1.dp, contentColor.copy(alpha = 0.10f), shape),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp),
        ) {
            listOf(R.string.lyrics, R.string.up_next).forEachIndexed { page, label ->
                val selected = pagerState.currentPage == page
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor.copy(alpha = if (selected) 1f else 0.5f),
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { coroutineScope.launch { pagerState.animateScrollToPage(page) } }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                )
                if (page == LyricsPage) Spacer(Modifier.width(8.dp))
            }

            Spacer(Modifier.weight(1f))

            val iconColors =
                IconButtonDefaults.iconButtonColors(
                    contentColor = contentColor,
                    disabledContentColor = contentColor.copy(alpha = 0.38f),
                )
            if (pagerState.currentPage == LyricsPage) {
                IconButton(onClick = onLyricsMenuClick, colors = iconColors) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = stringResource(R.string.more_options),
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                UpNextActions(
                    iconColors = iconColors,
                    contentColor = contentColor,
                    isListenTogetherGuest = isListenTogetherGuest,
                    sleepTimerEnabled = sleepTimerEnabled,
                    onSleepTimerClick = onSleepTimerClick,
                    onOpenFullQueue = onOpenFullQueue,
                    onMoreClick = onMoreClick,
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) { page ->
            when (page) {
                LyricsPage ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(WindowInsets.navigationBars.asPaddingValues()),
                    ) {
                        InlineLyricsView(
                            mediaMetadata = mediaMetadata,
                            // Also drives keep-screen-on, so only while the lyrics are actually on screen and playing.
                            showLyrics = isPlayerExpanded && isPlaying && pagerState.currentPage == LyricsPage,
                            positionProvider = positionProvider,
                        )
                    }

                else ->
                    UpNextList(
                        contentColor = contentColor,
                        isListenTogetherGuest = isListenTogetherGuest,
                    )
            }
        }
    }
}

@Composable
private fun UpNextActions(
    iconColors: androidx.compose.material3.IconButtonColors,
    contentColor: Color,
    isListenTogetherGuest: Boolean,
    sleepTimerEnabled: Boolean,
    onSleepTimerClick: () -> Unit,
    onOpenFullQueue: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()

    Row(verticalAlignment = Alignment.CenterVertically) {
        CardIconButton(
            icon = R.drawable.shuffle,
            contentDescription = stringResource(R.string.shuffle),
            active = shuffleModeEnabled,
            enabled = !isListenTogetherGuest,
            contentColor = contentColor,
            iconColors = iconColors,
            onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
        )
        CardIconButton(
            icon = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
            contentDescription = null,
            active = repeatMode != Player.REPEAT_MODE_OFF,
            enabled = !isListenTogetherGuest,
            contentColor = contentColor,
            iconColors = iconColors,
            onClick = { playerConnection.player.toggleRepeatMode() },
        )
        CardIconButton(
            icon = R.drawable.bedtime,
            contentDescription = stringResource(R.string.sleep_timer),
            active = sleepTimerEnabled,
            enabled = !isListenTogetherGuest,
            contentColor = contentColor,
            iconColors = iconColors,
            onClick = onSleepTimerClick,
        )
        CardIconButton(
            icon = R.drawable.queue_music,
            contentDescription = stringResource(R.string.open_full_queue),
            active = false,
            enabled = true,
            contentColor = contentColor,
            iconColors = iconColors,
            onClick = onOpenFullQueue,
        )
        CardIconButton(
            icon = R.drawable.more_vert,
            contentDescription = stringResource(R.string.more_options),
            active = false,
            enabled = true,
            contentColor = contentColor,
            iconColors = iconColors,
            onClick = onMoreClick,
        )
    }
}

@Composable
private fun CardIconButton(
    icon: Int,
    contentDescription: String?,
    active: Boolean,
    enabled: Boolean,
    contentColor: Color,
    iconColors: androidx.compose.material3.IconButtonColors,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        colors = iconColors,
        modifier =
            Modifier
                .size(40.dp)
                .padding(2.dp)
                .clip(RoundedCornerShape(50))
                .background(if (active) contentColor.copy(alpha = 0.18f) else Color.Transparent),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun UpNextList(
    contentColor: Color,
    isListenTogetherGuest: Boolean,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()

    val upcoming =
        remember(queueWindows, currentWindowIndex) {
            if (currentWindowIndex in queueWindows.indices) {
                queueWindows.subList(currentWindowIndex + 1, queueWindows.size)
            } else {
                queueWindows
            }
        }

    if (upcoming.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.navigationBars.asPaddingValues()),
        ) {
            Text(
                text = stringResource(R.string.up_next_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.7f),
            )
        }
        return
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp + navBottom),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(
            items = upcoming,
            key = { index, window -> "${window.uid}_$index" },
        ) { _, window ->
            val metadata = window.mediaItem.metadata ?: return@itemsIndexed
            UpNextRow(
                metadata = metadata,
                contentColor = contentColor,
                enabled = !isListenTogetherGuest,
                onClick = {
                    val castHandler = runCatching { playerConnection.service.castConnectionHandler }.getOrNull()
                    if (castHandler != null && castHandler.isCasting.value) {
                        val navigated = castHandler.navigateToMediaIfInQueue(window.mediaItem.mediaId)
                        if (!navigated) playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                    } else {
                        playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                        playerConnection.player.playWhenReady = true
                    }
                },
            )
        }
    }
}

@Composable
private fun UpNextRow(
    metadata: MediaMetadata,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        AsyncImage(
            model = metadata.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metadata.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metadata.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
