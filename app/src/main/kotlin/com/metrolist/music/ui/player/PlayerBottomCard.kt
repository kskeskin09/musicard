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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.wrapContentWidth
import com.metrolist.music.ui.component.InterludeDots
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.ui.utils.fadingEdge
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.launch
import com.metrolist.music.lyrics.WordTimestamp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.animateColorAsState
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.constants.RespectAgentPositioningKey
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import com.metrolist.music.lyrics.lyricsTextLooksSynced
import com.metrolist.music.lyrics.LyricsUtils
import com.metrolist.music.lyrics.LyricsEntry
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.metrolist.music.constants.LyricsTextPositionKey
import com.metrolist.music.constants.PlayerHorizontalPadding
import com.metrolist.music.ui.screens.settings.LyricsPosition
import com.metrolist.music.utils.rememberEnumPreference
import kotlin.math.abs
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.interaction.DragInteraction
import kotlinx.coroutines.flow.first

internal const val LyricsPage = 0
internal const val UpNextPage = 1
private val CardBottomFade = 72.dp

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
    // Reports the shown page (LyricsPage or UpNextPage), e.g. for dragging the card open.
    onPageChanged: (Int) -> Unit = {},
    // How far the card is opened toward full screen (0..1); its corners straighten out with it.
    expandFraction: () -> Float = { 0f },
    // Room kept free at the bottom of the fully opened card for the playback controls.
    openBottomPadding: Dp = 0.dp,
    // Height (px) at the bottom of the opened card covered by the controls panel; the lyrics and
    // the list fade out above it instead of showing behind the controls.
    openBottomCoverPx: () -> Float = { 0f },
    // Pulling down at the top of the opened lyrics / list goes back to the player.
    onPullDownClose: () -> Unit = {},
    // Opacity of the Lyrics / Up next row and its buttons; they hide with the playback controls.
    headerAlpha: () -> Float = { 1f },
    // Whether the opened lyrics stopped following the song after a manual scroll.
    followState: LyricsFollowState? = null,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val playerConnection = LocalPlayerConnection.current
    val currentLyrics = playerConnection?.currentLyrics?.collectAsStateWithLifecycle()?.value
    var showSourceDialog by remember { mutableStateOf(false) }
    if (showSourceDialog && mediaMetadata != null) {
        com.metrolist.music.ui.menu.LyricsSourceDialog(
            mediaMetadata = mediaMetadata,
            currentProvider = currentLyrics?.provider,
            onDismiss = { showSourceDialog = false },
        )
    }
    val latestOnPageChanged by rememberUpdatedState(onPageChanged)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { latestOnPageChanged(it) }
    }
    val coroutineScope = rememberCoroutineScope()
    val corner = 28.dp * (1f - expandFraction().coerceIn(0f, 1f))
    val shape = RoundedCornerShape(topStart = corner, topEnd = corner)

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
                    // Opened, the row moves down below the art and title header and stays usable.
                    .padding(top = (statusTop + OpenHeaderHeight) * expandFraction().coerceIn(0f, 1f))
                    .padding(start = 18.dp, end = 16.dp, top = CardHeaderTopPadding)
                    .height(CardHeaderRowHeight)
                    .graphicsLayer {
                        val a = headerAlpha()
                        alpha = a
                        // Hidden, the row is moved away so it doesn't catch touches.
                        if (a == 0f) translationY = -100_000f
                    },
        ) {
            listOf(R.string.lyrics, R.string.up_next).forEachIndexed { page, label ->
                val selected = pagerState.currentPage == page
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor.copy(alpha = if (selected) 1f else 0.5f),
                    maxLines = 1,
                    softWrap = false,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { coroutineScope.launch { pagerState.animateScrollToPage(page) } }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                )
                if (page == LyricsPage) Spacer(Modifier.width(8.dp))
            }

            Spacer(Modifier.weight(1f))

            val opened = expandFraction() >= 1f
            if (pagerState.currentPage == LyricsPage) {
                val provider = currentLyrics?.provider
                if (opened && provider != null && provider != "Unknown") {
                    // Where the lyrics come from; tap to pick another source.
                    Text(
                        text = provider,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .padding(end = 6.dp)
                                .widthIn(max = 140.dp)
                                .clip(RoundedCornerShape(50))
                                .background(contentColor.copy(alpha = 0.12f))
                                .clickable { showSourceDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                PlayerPillButton(
                    icon = R.drawable.player_more,
                    contentDescription = stringResource(R.string.more_options),
                    active = false,
                    enabled = true,
                    contentColor = contentColor,
                    onClick = onLyricsMenuClick,
                )
            } else {
                UpNextActions(
                    contentColor = contentColor,
                    isListenTogetherGuest = isListenTogetherGuest,
                    sleepTimerEnabled = sleepTimerEnabled,
                    onSleepTimerClick = onSleepTimerClick,
                    onMoreClick = onMoreClick,
                )
            }
        }

        val pullDownConnection = rememberPullDownToClose(enabled = { expandFraction() >= 1f }, onClose = onPullDownClose)
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) { page ->
            // Tapping the lyrics or swiping up on either page opens its full-screen view.
            // Dragging the card up is handled by the player around the card, which stays put
            // while the card grows (see BottomSheetPlayer).
            val pageModifier =
                Modifier
                    .fillMaxSize()
                    // Lyrics and the up-next list fade out toward the bottom of the card (and, once
                    // opened, under the header at the top). This is a single gradient mask drawn
                    // with the page, much cheaper than a real blur.
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        // Fades out toward the bottom: over the card's last 72 dp when small, and
                        // just above the controls panel once opened.
                        val cover = openBottomCoverPx()
                        val fadePx = CardBottomFade.toPx()
                        val end = size.height - cover
                        drawRect(
                            brush =
                                Brush.verticalGradient(
                                    0f to Color.Black,
                                    1f to Color.Transparent,
                                    startY = end - fadePx,
                                    endY = end,
                                ),
                            blendMode = BlendMode.DstIn,
                        )
                    }.nestedScroll(pullDownConnection)
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
                            expandFraction = expandFraction,
                            openBottomPadding = openBottomPadding,
                            followState = followState,
                        )
                    }

                else ->
                    Box(modifier = pageModifier) {
                        UpNextList(
                            contentColor = contentColor,
                            isListenTogetherGuest = isListenTogetherGuest,
                            expandFraction = expandFraction,
                            openBottomPadding = openBottomPadding,
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
            icon = R.drawable.player_shuffle,
            contentDescription = stringResource(R.string.shuffle),
            active = shuffleModeEnabled,
            enabled = !isListenTogetherGuest,
            contentColor = contentColor,
            onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
        )
        PlayerPillButton(
            icon = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.player_repeat_one else R.drawable.player_repeat,
            contentDescription = null,
            active = repeatMode != Player.REPEAT_MODE_OFF,
            enabled = !isListenTogetherGuest,
            contentColor = contentColor,
            onClick = { playerConnection.player.toggleRepeatMode() },
        )
        PlayerPillButton(
            icon = R.drawable.player_sleep,
            contentDescription = stringResource(R.string.sleep_timer),
            active = sleepTimerEnabled,
            enabled = !isListenTogetherGuest,
            contentColor = contentColor,
            onClick = onSleepTimerClick,
        )
        PlayerPillButton(
            icon = R.drawable.player_more,
            contentDescription = stringResource(R.string.more_options),
            active = false,
            enabled = true,
            contentColor = contentColor,
            onClick = onMoreClick,
        )
    }
}

/**
 * Small round toggle. Inactive it is a faint tint of [contentColor]; active it is filled with
 * [contentColor] and the icon takes the background color.
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
    size: Dp = 34.dp,
    iconSize: Dp = 18.dp,
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
                .size(size)
                .clip(CircleShape)
                .background(background)
                .clickable(enabled = enabled, onClick = onClick)
                .alpha(if (enabled) 1f else 0.4f),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun UpNextList(
    contentColor: Color,
    isListenTogetherGuest: Boolean,
    expandFraction: () -> Float,
    openBottomPadding: Dp,
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
    val fraction = expandFraction().coerceIn(0f, 1f)
    LazyColumn(
        // Scrollable only once the card is fully opened; in the small card it just shows what's next.
        userScrollEnabled = fraction >= 1f,
        contentPadding =
            PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 4.dp,
                bottom = lerp(12.dp + navBottom, openBottomPadding, fraction),
            ),
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
 *
 * While the card is opened toward full screen ([expandFraction] 0..1) the text grows and moves
 * toward the size, spacing and position it has in the full-screen lyrics, so the two views
 * line up when one fades into the other.
 */
@Composable
fun CompactLyrics(
    lyrics: String,
    positionProvider: () -> Long,
    color: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    expandFraction: () -> Float = { 0f },
    openBottomPadding: Dp = 0.dp,
    followState: LyricsFollowState? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle()
    val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0
    val respectAgentPositioning by rememberPreference(RespectAgentPositioningKey, true)
    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val synced = remember(lyrics) { lyricsTextLooksSynced(lyrics) }
    val allLines =
        remember(lyrics) {
            if (synced) {
                LyricsUtils.parseLyrics(lyrics)
            } else {
                lyrics.lines().filter { it.isNotBlank() }.map { LyricsEntry(0L, it.trim()) }
            }
        }
    val lines = remember(allLines) { allLines.filter { it.text.isNotBlank() } }
    // Instrumental breaks, found the same way as in the full lyrics view.
    val gaps = remember(allLines, synced) { if (synced) findInterludes(allLines) else emptyList() }
    val cardItems =
        remember(lines, gaps) {
            buildList {
                lines.indices.forEach { index ->
                    add(CompactItem.Line(index))
                    gaps.filter { it.afterLine == index }.forEach { add(CompactItem.Dots(it)) }
                }
            }
        }
    val hasWordTimings = remember(lines) { lines.any { !it.words.isNullOrEmpty() } }

    var position by remember { mutableLongStateOf(positionProvider() + lyricsOffset) }
    val activeGap by remember(gaps) {
        derivedStateOf { gaps.firstOrNull { position >= it.startMs && position <= it.endMs - InterludeEndLeadMs } }
    }
    if (synced) {
        LaunchedEffect(lines, isPlaying, lyricsOffset) {
            while (isActive) {
                position = positionProvider() + lyricsOffset
                if (!isPlaying) break
                // Fast updates only while something moves smoothly (word sweep or the dots).
                delay(if (hasWordTimings || activeGap != null) 32L else 150L)
            }
        }
    }
    val currentIndex by remember(lines, synced) {
        derivedStateOf { if (synced) LyricsUtils.findCurrentLineIndex(lines, position) else -1 }
    }

    // During a break the dots move to the top, otherwise the current line does.
    val scrollTarget by remember(cardItems) {
        derivedStateOf {
            val gap = activeGap
            if (gap != null) {
                cardItems.indexOfFirst { it is CompactItem.Dots && it.gap == gap }
            } else {
                cardItems.indexOfFirst { it is CompactItem.Line && it.index == currentIndex }
            }
        }
    }

    val fraction = expandFraction().coerceIn(0f, 1f)
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val windowHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    // Where the full-screen lyrics put their lines, measured from the top of this list once the
    // card covers the screen: the first line near the top, the current line 35% down.
    // Opened, the list starts below the art header and the Lyrics / Up next row; the current
    // line sits about a third of the way down the visible lyrics.
    val fullLyricsHeight = windowHeight - statusTop - OpenHeaderHeight - CardHeaderHeight - openBottomPadding
    val fullAnchorDrop = (fullLyricsHeight * FullLyricsAnchorRatio).coerceAtLeast(0.dp)
    val topPadding = lerp(2.dp, 8.dp, fraction)
    val sidePadding = lerp(24.dp, PlayerHorizontalPadding, fraction)
    // How far below the top of the list the current line sits (0 in the small card).
    val anchorPx = with(density) { (fullAnchorDrop * fraction).roundToPx() }

    val listState = rememberLazyListState()
    // Fully opened, the lyrics can be scrolled and tapped. While the user scrolls, following the
    // song pauses until the re-sync button is tapped.
    val interactive = fraction >= 1f
    // Shared with the player, which shows the re-sync button above the controls.
    val follow = followState ?: remember { LyricsFollowState() }
    var followPaused by follow::paused
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect {
            if (it is DragInteraction.Start) followPaused = true
        }
    }
    val appliedAnchor = remember { intArrayOf(0) }
    if (anchorPx != appliedAnchor[0]) {
        // Applied before this frame's layout, so the lines follow the finger without lagging.
        SideEffect {
            appliedAnchor[0] = anchorPx
            if (scrollTarget >= 0) listState.requestScrollToItem(scrollTarget, -anchorPx)
        }
    }
    LaunchedEffect(interactive) {
        if (!interactive) followPaused = false
    }
    LaunchedEffect(scrollTarget, followPaused) {
        if (scrollTarget < 0 || followPaused) return@LaunchedEffect
        val visible = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == scrollTarget }
        if (visible != null) {
            // A visible slide (a little slower than the default), like the full lyrics view.
            listState.animateScrollBy(
                (visible.offset - appliedAnchor[0]).toFloat(),
                tween(LineScrollMillis, easing = FastOutSlowInEasing),
            )
        } else {
            listState.animateScrollToItem(scrollTarget, -appliedAnchor[0])
        }
        // The card may have been dragged meanwhile; make sure the line ends where it belongs.
        val settled = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == scrollTarget }
        if (settled == null || settled.offset != appliedAnchor[0]) {
            listState.requestScrollToItem(scrollTarget, -appliedAnchor[0])
        }
    }

    val typography = MaterialTheme.typography
    val compactStyles =
        remember(typography) {
            val shared =
                TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle =
                        LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both,
                        ),
                )
            CompactStyles(
                active = typography.titleLarge.copy(fontWeight = FontWeight.Bold).merge(shared),
                inactive = typography.bodyLarge.copy(fontWeight = FontWeight.Medium).merge(shared),
                background = typography.bodyMedium.copy(fontStyle = FontStyle.Italic).merge(shared),
            )
        }
    val preferredBias =
        when (lyricsTextPosition) {
            LyricsPosition.LEFT -> -1f
            LyricsPosition.CENTER -> 0f
            LyricsPosition.RIGHT -> 1f
        }

    Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        userScrollEnabled = interactive,
        contentPadding =
            PaddingValues(
                start = sidePadding,
                end = sidePadding,
                top = topPadding,
                bottom = lerp(16.dp + navBottom, openBottomPadding, fraction),
            ),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(cardItems) { item ->
            when (item) {
                is CompactItem.Dots -> {
                    val gap = item.gap
                    val agent = gap.nextAgent.takeIf { respectAgentPositioning }
                    val compactBias =
                        when (agent) {
                            "v2" -> 1f
                            "v1000" -> 0f
                            else -> -1f
                        }
                    val fullBias =
                        when (agent) {
                            "v1" -> -1f
                            "v2" -> 1f
                            "v1000" -> 0f
                            else -> preferredBias
                        }
                    AnimatedVisibility(
                        visible = activeGap == gap,
                        enter = expandVertically() + fadeIn(),
                        exit = fadeOut() + shrinkVertically(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val progress =
                            ((position - gap.startMs).toFloat() / (gap.endMs - InterludeEndLeadMs - gap.startMs).coerceAtLeast(1L))
                                .coerceIn(0f, 1f)
                        InterludeDots(
                            progress = progress,
                            positionMs = position,
                            color = color,
                            dotSize = lerp(10.dp, 14.dp, fraction),
                            modifier =
                                Modifier
                                    .padding(top = lerp(8.dp, 34.dp, fraction), bottom = lerp(8.dp, 18.dp, fraction))
                                    .fillMaxWidth()
                                    .wrapContentWidth(BiasAlignment.Horizontal(lerp(compactBias, fullBias, fraction))),
                        )
                    }
                }

                is CompactItem.Line -> {
                    val index = item.index
                    val line = lines[index]
                    val active = synced && index == currentIndex && activeGap == null
                    val agent = line.agent.takeIf { respectAgentPositioning }
                    val compactBias =
                        when {
                            line.isBackground -> 0f
                            agent == "v2" -> 1f
                            agent == "v1000" -> 0f
                            else -> -1f
                        }
                    val fullBias =
                        when {
                            agent == "v1" -> -1f
                            agent == "v2" -> 1f
                            agent == "v1000" -> 0f
                            line.isBackground -> 0f
                            else -> preferredBias
                        }
                    val bias = lerp(compactBias, fullBias, fraction)
                    val textAlign =
                        when (if (fraction < 0.5f) compactBias else fullBias) {
                            -1f -> TextAlign.Start
                            1f -> TextAlign.End
                            else -> TextAlign.Center
                        }
                    val compactStyle =
                        when {
                            line.isBackground -> compactStyles.background
                            active -> compactStyles.active
                            else -> compactStyles.inactive
                        }
                    val style =
                        if (fraction == 0f) {
                            compactStyle
                        } else {
                            // The full-screen lyrics draw every line bold at this size.
                            val fullSize = if (line.isBackground) FullLyricsTextSize * 0.7f else FullLyricsTextSize
                            lerp(
                                compactStyle,
                                compactStyle.copy(
                                    fontSize = fullSize.sp,
                                    lineHeight = (fullSize * FullLyricsLineSpacing).sp,
                                    letterSpacing = (-0.5).sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                fraction,
                            )
                        }
                    val compactDim = if (line.isBackground) 0.4f else 0.5f
                    val fullDim =
                        when {
                            line.isBackground -> 0.5f
                            currentIndex < 0 -> 0.2f
                            else ->
                                when (abs(index - currentIndex)) {
                                    0, 1, 2 -> 0.2f
                                    3 -> 0.15f
                                    4 -> 0.1f
                                    else -> 0.08f
                                }
                        }
                    val dimAlpha = lerp(compactDim, fullDim, fraction)
                    val words = line.words
                    val textModifier = Modifier.fillMaxWidth().wrapContentWidth(BiasAlignment.Horizontal(bias))
                    Box(
                        modifier =
                            Modifier
                                .then(
                                    if (interactive && synced) {
                                        // Tapping a line jumps the song there.
                                        Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                playerConnection.player.seekTo((line.time - lyricsOffset).coerceAtLeast(0L))
                                                followPaused = false
                                            }
                                    } else {
                                        Modifier
                                    },
                                ).padding(
                                    top = lerp(5.dp, if (line.isBackground) 0.dp else FullLineGap + 12.dp, fraction),
                                    bottom = lerp(5.dp, if (line.isBackground) 2.dp else 12.dp, fraction),
                                ),
                    ) {
                        if (active && !words.isNullOrEmpty()) {
                            SweepingWordsLine(
                                words = words,
                                positionMsProvider = { position },
                                color = color,
                                dimAlpha = dimAlpha,
                                style = style,
                                textAlign = textAlign,
                                modifier = textModifier,
                            )
                        } else {
                            Text(
                                text = line.text,
                                style = style,
                                color = color.copy(alpha = if (active || !synced) 1f else dimAlpha),
                                textAlign = textAlign,
                                modifier = textModifier,
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

/** Whether the opened lyrics stopped following the song because the user scrolled them. */
@Stable
class LyricsFollowState {
    var paused by mutableStateOf(false)
}

private class CompactStyles(
    val active: TextStyle,
    val inactive: TextStyle,
    val background: TextStyle,
)

private val CardHeaderTopPadding = 2.dp
private val CardHeaderRowHeight = 40.dp
private val CardHeaderHeight = CardHeaderTopPadding + CardHeaderRowHeight

// Layout of the full-screen lyrics (FullScreenLyrics in Player.kt and ExperimentalLyrics), which the
// card lyrics grow into: its header takes 60 dp under the status bar, the first line starts 40 dp
// below that, the current line sits 35% down, and lines are 36 sp with 16 dp between them.
private val FullLyricsTop = 60.dp
private val FullLyricsTopPadding = 40.dp
private val FullLineGap = 16.dp
private const val FullLyricsAnchorRatio = 0.35f
private const val FullLyricsTextSize = 36f
private const val FullLyricsLineSpacing = 1.3f

private data class Interlude(
    val afterLine: Int,
    val startMs: Long,
    val endMs: Long,
    val nextAgent: String?,
)

private sealed interface CompactItem {
    data class Line(val index: Int) : CompactItem

    data class Dots(val gap: Interlude) : CompactItem
}

/**
 * Finds breaks longer than [InterludeMinMs] between lines. [lines] includes blank lines; the
 * returned [Interlude.afterLine] counts only the non-blank lines (the ones shown on the card).
 */
private fun findInterludes(lines: List<LyricsEntry>): List<Interlude> {
    val result = mutableListOf<Interlude>()
    var shownIndex = -1
    lines.forEachIndexed { i, entry ->
        if (entry.text.isNotBlank()) shownIndex++
        if (i >= lines.size - 1 || shownIndex < 0) return@forEachIndexed
        val nextStart = lines[i + 1].time
        val currentEnd =
            when {
                !entry.words.isNullOrEmpty() -> (entry.words.last().endTime * 1000).toLong()
                entry.text.isBlank() -> entry.time
                else -> null
            }
        if (currentEnd != null && nextStart - currentEnd > InterludeMinMs) {
            result.add(Interlude(shownIndex, currentEnd, nextStart, lines[i + 1].agent))
        }
    }
    return result
}

private const val InterludeMinMs = 4000L
private const val InterludeEndLeadMs = 650L
private const val LineScrollMillis = 650

// Height of the opened card's header (art and title) below the status bar.
private val OpenHeaderHeight = 64.dp

/**
 * Pulling down past the top of a list (by hand) calls [onClose] once the pull is long enough.
 */
@Composable
private fun rememberPullDownToClose(
    enabled: () -> Boolean,
    onClose: () -> Unit,
): NestedScrollConnection {
    val latestEnabled by rememberUpdatedState(enabled)
    val latestOnClose by rememberUpdatedState(onClose)
    val thresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    return remember {
        object : NestedScrollConnection {
            var pulled = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (latestEnabled() && source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > thresholdPx) {
                        pulled = Float.NEGATIVE_INFINITY
                        latestOnClose()
                    }
                } else if (consumed.y != 0f && pulled >= 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }
}

/**
 * One word-timed line. The dim text is drawn first, then the bright text is revealed across
 * each word from left to right in step with the word's own duration, with a soft leading edge
 * (the same sweep the full-screen lyrics use). The position is read only while drawing, so
 * playback progress redraws the line without recomposing it.
 */
@Composable
private fun SweepingWordsLine(
    words: List<WordTimestamp>,
    positionMsProvider: () -> Long,
    color: Color,
    dimAlpha: Float,
    style: TextStyle,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
) {
    val built =
        remember(words) {
            val sb = StringBuilder()
            val ranges =
                words.map { word ->
                    val start = sb.length
                    sb.append(word.text)
                    val end = sb.length
                    if (word.hasTrailingSpace) sb.append(' ')
                    start until end
                }
            sb.toString() to ranges
        }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val edgeWidthPx = with(LocalDensity.current) { SweepEdgeWidth.toPx() }

    Text(
        text = built.first,
        style = style,
        color = color.copy(alpha = dimAlpha),
        textAlign = textAlign,
        onTextLayout = { layout = it },
        modifier =
            modifier
                .drawWithContent {
                    drawContent()
                    val textLayout = layout ?: return@drawWithContent
                    val positionSec = positionMsProvider() / 1000.0
                    val sungPath = Path()
                    words.forEachIndexed { index, word ->
                        val range = built.second[index]
                        if (range.isEmpty()) return@forEachIndexed
                        val duration = (word.endTime - word.startTime).coerceAtLeast(0.05)
                        val progress = ((positionSec - word.startTime) / duration).coerceIn(0.0, 1.0).toFloat()
                        if (progress <= 0f) return@forEachIndexed
                        val firstLine = textLayout.getLineForOffset(range.first)
                        val lastLine = textLayout.getLineForOffset(range.last)
                        if (progress >= 1f || firstLine != lastLine) {
                            // Fully sung (or wrapped across lines): reveal character by character.
                            val revealed = if (progress >= 1f) range.count() else (range.count() * progress).toInt()
                            for (offset in range.first until range.first + revealed) {
                                val box = textLayout.getBoundingBox(offset)
                                val line = textLayout.getLineForOffset(offset)
                                sungPath.addRect(
                                    Rect(box.left, textLayout.getLineTop(line), box.right, textLayout.getLineBottom(line)),
                                )
                            }
                        } else {
                            val left = textLayout.getBoundingBox(range.first).left
                            val right = textLayout.getBoundingBox(range.last).right
                            val fillX = left + (right - left) * progress
                            val wordRect =
                                Rect(left, textLayout.getLineTop(firstLine), right, textLayout.getLineBottom(firstLine))
                            clipRect(wordRect.left, wordRect.top, wordRect.right, wordRect.bottom) {
                                drawText(
                                    textLayoutResult = textLayout,
                                    brush =
                                        Brush.horizontalGradient(
                                            0f to color,
                                            1f to color.copy(alpha = 0f),
                                            startX = fillX - edgeWidthPx / 2f,
                                            endX = fillX + edgeWidthPx / 2f,
                                        ),
                                )
                            }
                        }
                    }
                    clipPath(sungPath) {
                        drawText(textLayoutResult = textLayout, color = color)
                    }
                },
    )
}

private val SweepEdgeWidth = 14.dp
