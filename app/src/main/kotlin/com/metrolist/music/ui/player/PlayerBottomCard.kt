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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.animateColorAsState
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.constants.RespectAgentPositioningKey
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import com.metrolist.music.lyrics.lyricsTextLooksSynced
import com.metrolist.music.lyrics.LyricsUtils
import com.metrolist.music.lyrics.LyricsEntry
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState

private const val LyricsPage = 0
private const val UpNextPage = 1
private val OpenSwipeThreshold = 40.dp

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
    contentColor: Color,
    isListenTogetherGuest: Boolean,
    sleepTimerEnabled: Boolean,
    onSleepTimerClick: () -> Unit,
    onLyricsMenuClick: () -> Unit,
    onOpenFullQueue: () -> Unit,
    onOpenFullLyrics: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

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
                    .padding(start = 18.dp, end = 6.dp, top = 2.dp)
                    .height(40.dp),
        ) {
            listOf(R.string.lyrics, R.string.up_next).forEachIndexed { page, label ->
                val selected = pagerState.currentPage == page
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor.copy(alpha = if (selected) 1f else 0.5f),
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { coroutineScope.launch { pagerState.animateScrollToPage(page) } }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                )
                if (page == LyricsPage) Spacer(Modifier.width(8.dp))
            }

            Spacer(Modifier.weight(1f))

            if (pagerState.currentPage == LyricsPage) {
                IconButton(
                    onClick = onLyricsMenuClick,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = contentColor),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = stringResource(R.string.more_options),
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                UpNextActions(
                    contentColor = contentColor,
                    isListenTogetherGuest = isListenTogetherGuest,
                    sleepTimerEnabled = sleepTimerEnabled,
                    onSleepTimerClick = onSleepTimerClick,
                    onMoreClick = onMoreClick,
                )
                Spacer(Modifier.width(10.dp))
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
            // Tapping the lyrics or swiping up on either page opens its full-screen view.
            val openFull = if (page == LyricsPage) onOpenFullLyrics else onOpenFullQueue
            val latestOpenFull by rememberUpdatedState(openFull)
            val pageModifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var dragged = 0f
                        detectVerticalDragGestures(
                            onDragStart = { dragged = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                dragged += dragAmount
                                change.consume()
                            },
                            onDragEnd = {
                                if (dragged < -OpenSwipeThreshold.toPx()) latestOpenFull()
                            },
                        )
                    }
            when (page) {
                LyricsPage ->
                    Box(
                        modifier =
                            pageModifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onOpenFullLyrics,
                            ),
                    ) {
                        InlineLyricsView(
                            mediaMetadata = mediaMetadata,
                            showLyrics = isPlayerExpanded && pagerState.currentPage == LyricsPage,
                            positionProvider = positionProvider,
                            compactColor = contentColor,
                            isPlaying = isPlaying,
                        )
                    }

                else ->
                    Box(modifier = pageModifier) {
                        UpNextList(
                            contentColor = contentColor,
                            isListenTogetherGuest = isListenTogetherGuest,
                        )
                    }
            }
        }
    }
}

@Composable
private fun UpNextActions(
    contentColor: Color,
    isListenTogetherGuest: Boolean,
    sleepTimerEnabled: Boolean,
    onSleepTimerClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerPillButton(
            icon = R.drawable.shuffle,
            contentDescription = stringResource(R.string.shuffle),
            active = shuffleModeEnabled,
            enabled = !isListenTogetherGuest,
            contentColor = contentColor,
            onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
        )
        PlayerPillButton(
            icon = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
            contentDescription = null,
            active = repeatMode != Player.REPEAT_MODE_OFF,
            enabled = !isListenTogetherGuest,
            contentColor = contentColor,
            onClick = { playerConnection.player.toggleRepeatMode() },
        )
        PlayerPillButton(
            icon = R.drawable.bedtime,
            contentDescription = stringResource(R.string.sleep_timer),
            active = sleepTimerEnabled,
            enabled = !isListenTogetherGuest,
            contentColor = contentColor,
            onClick = onSleepTimerClick,
        )
        PlayerPillButton(
            icon = R.drawable.more_horiz,
            contentDescription = stringResource(R.string.more_options),
            active = false,
            enabled = true,
            contentColor = contentColor,
            onClick = onMoreClick,
        )
    }
}

/**
 * Apple Music style toggle: a small rounded pill. Inactive it is a faint tint of [contentColor];
 * active it is filled with [contentColor] and the icon takes the background color.
 */
@Composable
fun PlayerPillButton(
    icon: Int,
    contentDescription: String?,
    active: Boolean,
    enabled: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeIconColor: Color = Color.Black.copy(alpha = 0.75f),
) {
    val background by animateColorAsState(
        targetValue = if (active) contentColor.copy(alpha = 0.92f) else contentColor.copy(alpha = 0.12f),
        label = "pillBackground",
    )
    val iconColor by animateColorAsState(
        targetValue = if (active) activeIconColor else contentColor,
        label = "pillIcon",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(width = 44.dp, height = 32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(background)
                .clickable(enabled = enabled, onClick = onClick)
                .alpha(if (enabled) 1f else 0.4f),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(18.dp),
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
        userScrollEnabled = false,
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

/**
 * Small-screen lyrics for the bottom card: the current line bold and bright, the others dimmed.
 * Word/syllable timed lines light up word by word, and duet agents (v1/v2) align left/right
 * like in the full lyrics view.
 */
@Composable
fun CompactLyrics(
    lyrics: String,
    positionProvider: () -> Long,
    color: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle()
    val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
    val respectAgentPositioning by rememberPreference(RespectAgentPositioningKey, true)
    val synced = remember(lyrics) { lyricsTextLooksSynced(lyrics) }
    val lines =
        remember(lyrics) {
            if (synced) {
                LyricsUtils.parseLyrics(lyrics).filter { it.text.isNotBlank() }
            } else {
                lyrics.lines().filter { it.isNotBlank() }.map { LyricsEntry(0L, it.trim()) }
            }
        }
    val hasWordTimings = remember(lines) { lines.any { !it.words.isNullOrEmpty() } }

    var position by remember { mutableLongStateOf(positionProvider() + lyricsOffset) }
    if (synced) {
        LaunchedEffect(lines, isPlaying, lyricsOffset) {
            while (isActive) {
                position = positionProvider() + lyricsOffset
                if (!isPlaying) break
                delay(if (hasWordTimings) 32L else 150L)
            }
        }
    }
    val currentIndex by remember(lines, synced) {
        derivedStateOf { if (synced) LyricsUtils.findCurrentLineIndex(lines, position) else -1 }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) listState.animateScrollToItem(currentIndex)
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        state = listState,
        userScrollEnabled = false,
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 2.dp, bottom = 16.dp + navBottom),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        itemsIndexed(lines) { index, line ->
            val active = synced && index == currentIndex
            val textAlign =
                when {
                    line.isBackground -> TextAlign.Center
                    respectAgentPositioning && line.agent == "v2" -> TextAlign.End
                    respectAgentPositioning && line.agent == "v1000" -> TextAlign.Center
                    else -> TextAlign.Start
                }
            val baseStyle =
                when {
                    line.isBackground -> MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
                    active -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    else -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                }
            val dimAlpha = if (line.isBackground) 0.4f else 0.5f
            val words = line.words
            if (active && !words.isNullOrEmpty()) {
                val positionSec = position / 1000.0
                Text(
                    text =
                        buildAnnotatedString {
                            words.forEach { word ->
                                // Fades each word in over its own duration, like the full lyrics view.
                                val duration = (word.endTime - word.startTime).coerceAtLeast(0.08)
                                val progress = ((positionSec - word.startTime) / duration).coerceIn(0.0, 1.0).toFloat()
                                withStyle(SpanStyle(color = color.copy(alpha = dimAlpha + (1f - dimAlpha) * progress))) {
                                    append(word.text)
                                }
                                if (word.hasTrailingSpace) append(" ")
                            }
                        },
                    style = baseStyle,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = line.text,
                    style = baseStyle,
                    color = color.copy(alpha = if (active || !synced) 1f else dimAlpha),
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
