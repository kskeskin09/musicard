/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Performance optimized MiniPlayer - prevents unnecessary recomposition
 */

package com.metrolist.music.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.SwipeSensitivityKey
import com.metrolist.music.constants.SwipeThumbnailKey
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.listentogether.ListenTogetherManager
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.CastConnectionHandler
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.joinToArtistString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.metrolist.music.ui.component.Icon as MIcon
import androidx.compose.ui.draw.blur
import com.metrolist.music.constants.MiniPlayerBackgroundStyle
import com.metrolist.music.constants.MiniPlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerCardBackground
import com.metrolist.music.constants.PlayerCardBackgroundKey
import com.metrolist.music.ui.theme.albumBackgroundColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.first
import com.metrolist.music.extensions.metadata
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.metrolist.music.ui.theme.PlayerColorExtractor
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.metrolist.music.LocalNavController

/**
 * Stable wrapper for progress state - reads values only during draw phase
 * This prevents recomposition when position/duration change
 */
@Stable
class ProgressState(
    private val positionState: MutableLongState,
    private val durationState: MutableLongState,
) {
    val progress: Float
        get() {
            val duration = durationState.longValue
            return if (duration > 0) (positionState.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
        }
}

@Composable
fun MiniPlayer(
    positionState: MutableLongState,
    durationState: MutableLongState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    // Create stable progress state - doesn't cause recomposition on position changes
    val progressState = remember { ProgressState(positionState, durationState) }

    NewMiniPlayer(
        progressState = progressState,
        modifier = modifier,
        onClick = onClick,
    )
}

// ============================================================================
// NEW MINI PLAYER DESIGN
// ============================================================================

@Composable
private fun NewMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return

    // Theme settings - these rarely change
    val miniPlayerBackground by rememberEnumPreference(
        MiniPlayerBackgroundStyleKey,
        defaultValue = MiniPlayerBackgroundStyle.DEFAULT,
    )
    // Follow background (stored as BLUR): the mini player takes the player's own background, the
    // glass cover or the album color. Older choices (transparent, gradient, black) follow the theme.
    val playerCardBackground by rememberEnumPreference(
        PlayerCardBackgroundKey,
        defaultValue = PlayerCardBackground.CLASSIC,
    )
    val followBackground = miniPlayerBackground == MiniPlayerBackgroundStyle.BLUR
    val context = LocalContext.current
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme =
        remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

    // Player states - only collect what's needed at this level
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()

    // Cast state - safely access castConnectionHandler to prevent crashes during service lifecycle changes
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    // Swipe settings
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)

    // Disable swipe for Listen Together guests
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest

    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    val windowInfo = LocalWindowInfo.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isTabletLandscape =
        remember(windowInfo.containerSize.width, configuration.orientation) {
            (windowInfo.containerSize.width / density.density) >= 600f && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

    // Swipe animation state
    val offsetXAnimatable = remember { Animatable(0f) }
    // Width of the sliding part, and the songs on either side shown while swiping.
    var swipeWidthPx by remember { mutableFloatStateOf(0f) }
    val currentMediaId by rememberUpdatedState(mediaMetadata?.id)
    val previousMetadata =
        remember(mediaMetadata?.id) {
            runCatching {
                val player = playerConnection.player
                player.previousMediaItemIndex.takeIf { it != -1 }?.let { player.getMediaItemAt(it).metadata }
            }.getOrNull()
        }
    val nextMetadata =
        remember(mediaMetadata?.id) {
            runCatching {
                val player = playerConnection.player
                player.nextMediaItemIndex.takeIf { it != -1 }?.let { player.getMediaItemAt(it).metadata }
            }.getOrNull()
        }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec =
        remember {
            spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        }

    val autoSwipeThreshold =
        remember(swipeSensitivity) {
            (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
        }

    LaunchedEffect(mediaMetadata?.id, miniPlayerBackground, playerCardBackground) {
        gradientColors = emptyList()
        if (miniPlayerBackground == MiniPlayerBackgroundStyle.BLUR && playerCardBackground == PlayerCardBackground.CLASSIC) {
            val url = mediaMetadata?.thumbnailUrl
            if (url != null) {
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .size(100, 100)
                        .allowHardware(false)
                        .build()
                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    val bitmap = result?.image?.toBitmap()
                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.Default) {
                            Palette.from(bitmap)
                                .maximumColorCount(8)
                                .resizeBitmapArea(100 * 100)
                                .generate()
                        }
                        val extracted = PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = 0xFF000000.toInt(),
                        )
                        withContext(Dispatchers.Main) {
                            gradientColors = extracted
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            gradientColors = emptyList()
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    // Memoize colors
    val appDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val albumColor by androidx.compose.animation.animateColorAsState(
        targetValue = gradientColors.firstOrNull()?.let { albumBackgroundColor(it, appDarkTheme) } ?: DefaultAlbumBackground,
        animationSpec = androidx.compose.animation.core.tween(800),
        label = "miniAlbumColor",
    )
    val themePillColor by androidx.compose.animation.animateColorAsState(
        targetValue =
            com.metrolist.music.ui.theme.PageFadeColors.current
                ?.let { com.metrolist.music.ui.theme.raisedPageColor(it) }
                ?: MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "miniThemePill",
    )
    val backgroundColor =
        when {
            // On a cover-tinted page (stats, playlists, albums) the pill takes the page's raised color.
            !followBackground -> themePillColor
            playerCardBackground == PlayerCardBackground.GLASS -> DefaultAlbumBackground
            else -> albumColor
        }
    // Both player backgrounds are mid-dark, so the text and buttons on them are white.
    val forceLightColors = followBackground && !useDarkTheme

    val primaryColor = if (forceLightColors) Color.White else MaterialTheme.colorScheme.primary
    val outlineColor = if (forceLightColors) Color.White else MaterialTheme.colorScheme.outline
    val onSurfaceColor = if (forceLightColors) Color.White else MaterialTheme.colorScheme.onSurface
    val errorColor = if (forceLightColors) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.error

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 12.dp)
                .let { baseModifier ->
                    if (swipeThumbnail) {
                        baseModifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    dragStartTime = System.currentTimeMillis()
                                    totalDragDistance = 0f
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        offsetXAnimatable.animateTo(0f, animationSpec)
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    val adjustedDragAmount =
                                        if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                    val tryingToSwipeRight = adjustedDragAmount > 0
                                    val tryingToSwipeLeft = adjustedDragAmount < 0
                                    val allowLeft = tryingToSwipeLeft && canSkipNext
                                    val allowRight = tryingToSwipeRight && canSkipPrevious

                                    val canReturnToCenter =
                                        (tryingToSwipeRight && !canSkipPrevious && offsetXAnimatable.value < 0) ||
                                            (tryingToSwipeLeft && !canSkipNext && offsetXAnimatable.value > 0)

                                    if (allowLeft || allowRight || canReturnToCenter) {
                                        totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                        coroutineScope.launch {
                                            offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    val dragDuration = System.currentTimeMillis() - dragStartTime
                                    val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                    val currentOffset = offsetXAnimatable.value
                                    val minDistanceThreshold = 50f
                                    val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                    val shouldChangeSong =
                                        (kotlin.math.abs(currentOffset) > minDistanceThreshold && velocity > velocityThreshold) ||
                                            (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                    val goPrevious = shouldChangeSong && currentOffset > 0 && canSkipPrevious
                                    val goNext = shouldChangeSong && currentOffset <= 0 && canSkipNext
                                    coroutineScope.launch {
                                        if ((goPrevious || goNext) && swipeWidthPx > 0f) {
                                            // The other song slides all the way in, then becomes the current
                                            // one; the offset goes back to 0 once it is showing, so nothing jumps.
                                            val oldId = currentMediaId
                                            offsetXAnimatable.animateTo(
                                                if (goPrevious) swipeWidthPx else -swipeWidthPx,
                                                androidx.compose.animation.core.tween(160),
                                            )
                                            if (goPrevious) {
                                                playerConnection.player.seekToPreviousMediaItem()
                                            } else {
                                                playerConnection.player.seekToNext()
                                            }
                                            kotlinx.coroutines.withTimeoutOrNull(600) {
                                                androidx.compose.runtime.snapshotFlow { currentMediaId }.first { it != oldId }
                                            }
                                            offsetXAnimatable.snapTo(0f)
                                        } else {
                                            if (goPrevious) playerConnection.player.seekToPreviousMediaItem()
                                            if (goNext) playerConnection.player.seekToNext()
                                            offsetXAnimatable.animateTo(0f, animationSpec)
                                        }
                                    }
                                },
                            )
                        }
                    } else {
                        baseModifier
                    }
                },
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier =
                Modifier
                    .then(if (isTabletLandscape) Modifier.width(500.dp).align(Alignment.Center) else Modifier.fillMaxWidth())
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(color = backgroundColor)
                    .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    )
                    // Thin, display-only progress line hugging the bottom edge of the pill. No track
                    // is drawn, so at 0 the pill looks plain. Progress is read while drawing only.
                    .drawWithContent {
                        drawContent()
                        val progress = progressState.progress
                        if (progress <= 0f) return@drawWithContent
                        val inset = 32.dp.toPx()
                        val stroke = 2.5.dp.toPx()
                        val end = size.width - inset
                        if (end <= inset) return@drawWithContent
                        val y = size.height - stroke / 2f
                        drawLine(
                            color = onSurfaceColor.copy(alpha = 0.85f),
                            start = Offset(inset, y),
                            end = Offset(inset + (end - inset) * progress, y),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                    },
        ) {
            if (followBackground) {
                if (playerCardBackground == PlayerCardBackground.GLASS) {
                    MiniGlassBackground(mediaMetadata?.thumbnailUrl)
                } else {
                    Box(Modifier.fillMaxSize().background(albumBackgroundBrush(albumColor)))
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            ) {
                // Swiping moves only the cover and the song info, both ways; the pill, the progress
                // line and the buttons stay put and switch to the new song once it changes. The
                // next (or previous) song's cover and info slide in from the other side.
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clipToBounds()
                            .onSizeChanged { swipeWidthPx = it.width.toFloat() },
                ) {
                    MiniSwipePreview(
                        metadata = previousMetadata,
                        onSurfaceColor = onSurfaceColor,
                        modifier =
                            Modifier.graphicsLayer {
                                val offset = offsetXAnimatable.value
                                translationX = offset - size.width
                                alpha = if (offset > 0f) 1f else 0f
                            },
                    )
                    MiniSwipePreview(
                        metadata = nextMetadata,
                        onSurfaceColor = onSurfaceColor,
                        modifier =
                            Modifier.graphicsLayer {
                                val offset = offsetXAnimatable.value
                                translationX = offset + size.width
                                alpha = if (offset < 0f) 1f else 0f
                            },
                    )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationX = offsetXAnimatable.value },
                ) {
                // Album art: tap to play / pause - isolated composable
                NewMiniPlayerPlayButton(
                    progressState = progressState,
                    playbackState = playbackState,
                    isCasting = isCasting,
                    castHandler = castHandler,
                    playerConnection = playerConnection,
                    mediaMetadata = mediaMetadata,
                    primaryColor = primaryColor,
                    outlineColor = outlineColor,
                    listenTogetherManager = listenTogetherManager,
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Song info - isolated composable
                NewMiniPlayerSongInfo(
                    mediaMetadata = mediaMetadata,
                    onSurfaceColor = onSurfaceColor,
                    errorColor = errorColor,
                    modifier = Modifier.weight(1f),
                )
                }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Cast indicator
                if (isCasting) {
                    Icon(
                        painter = painterResource(R.drawable.cast_connected),
                        contentDescription = "Casting",
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

// Artist button: opens the artist's page
                mediaMetadata?.artists?.firstOrNull { it.id != null }?.id?.let { artistId ->
                    ArtistButton(
                        artistId = artistId,
                        onSurfaceColor = onSurfaceColor,
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

// Favorite button - isolated composable
                mediaMetadata?.let { FavoriteButton(
                    songId = it.id,
                    errorColor = errorColor,
                    outlineColor = outlineColor,
                    onSurfaceColor = onSurfaceColor,
                )
                }
            }
        }
    }
}

/**
 * Play button with circular progress indicator
 * Uses drawWithContent to update progress without recomposition
 */
@Composable
private fun NewMiniPlayerPlayButton(
    progressState: ProgressState,
    playbackState: Int,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    mediaMetadata: MediaMetadata?,
    primaryColor: Color,
    outlineColor: Color,
    listenTogetherManager: ListenTogetherManager?,
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    val artShape = RoundedCornerShape(10.dp)
    // The art shrinks a little while the song is paused.
    val pausedLook = isListenTogetherGuest && isMuted ||
        (!isListenTogetherGuest && (!effectiveIsPlaying || playbackState == Player.STATE_ENDED))
    val artScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pausedLook) 0.88f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "miniArtScale",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(46.dp)
                .graphicsLayer {
                    scaleX = artScale
                    scaleY = artScale
                },
    ) {
        // Thumbnail with play/pause overlay
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(artShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isListenTogetherGuest) {
                            playerConnection.toggleMute()
                            return@clickable
                        }
                        if (isCasting) {
                            if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                        } else if (playbackState == Player.STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.togglePlayPause()
                        }
                    },
        ) {
            mediaMetadata?.let { metadata ->
                val thumbnailUrl =
                    remember(metadata.thumbnailUrl) {
                        metadata.thumbnailUrl?.resize(120, 120)
                    }
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(artShape),
                )
            }

            // Paused (or muted guest): the art dims and shows the play icon
            if (isListenTogetherGuest && isMuted ||
                (!isListenTogetherGuest && (!effectiveIsPlaying || playbackState == Player.STATE_ENDED))
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f), artShape),
                )
                Icon(
                    painter =
                        painterResource(
                            if (isListenTogetherGuest) {
                                if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                            } else if (playbackState == Player.STATE_ENDED) {
                                R.drawable.replay
                            } else {
                                R.drawable.player_play
                            },
                        ),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * Song info display - title and artist
 */
@Composable
private fun NewMiniPlayerSongInfo(
    mediaMetadata: MediaMetadata?,
    onSurfaceColor: Color,
    errorColor: Color,
    modifier: Modifier = Modifier,
) {
    val error by LocalPlayerConnection.current?.error?.collectAsState() ?: remember { mutableStateOf(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        mediaMetadata?.let { metadata ->
            Text(
                text = metadata.title,
                color = onSurfaceColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (metadata.explicit) MIcon.Explicit()
                 if (metadata.artists.any { it.name.isNotBlank() }) {
                     Text(
                         text = metadata.artists.joinToArtistString(" ${stringResource(R.string.and)} ") { it.name },
                         color = onSurfaceColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                    )
                }
            }

            AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(R.string.error_playing),
                    color = errorColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ============================================================================
// ISOLATED BUTTON COMPOSABLES - Prevent parent recomposition
// ============================================================================

@Composable
private fun FavoriteButton(
    songId: String,
    errorColor: Color,
    outlineColor: Color,
    onSurfaceColor: Color,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(songId).collectAsStateWithLifecycle(initialValue = null)
    // For episodes, show saved state (inLibrary); for songs, show liked state
    val isEpisode = librarySong?.song?.isEpisode == true
    val isLiked = if (isEpisode) librarySong?.song?.inLibrary != null else librarySong?.song?.liked == true

    val view = androidx.compose.ui.platform.LocalView.current
    // Same as the player's heart: liking by tap makes it grow a little, wobble once and settle.
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    var tapped by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    val unlikeGuard = com.metrolist.music.ui.component.rememberUnlikeGuard(isLiked)
    val crackShake = com.metrolist.music.ui.component.rememberCrackShake(unlikeGuard)
    com.metrolist.music.ui.menu.AddToPlaylistDialog(
        isVisible = showAddToPlaylist,
        onGetSong = { listOf(songId) },
        onGetSongIds = { listOf(songId) },
        onDismiss = { showAddToPlaylist = false },
    )
    LaunchedEffect(isLiked) {
        if (!isLiked && tapped) {
            // The like is removed: the heart pulls back in and settles.
            scale.animateTo(0.7f, androidx.compose.animation.core.tween(120))
            scale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 400f))
        }
        if (isLiked && tapped) {
            launch {
                scale.animateTo(1.2f, androidx.compose.animation.core.tween(160, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                scale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 300f))
            }
            rotation.animateTo(
                0f,
                androidx.compose.animation.core.keyframes {
                    durationMillis = 520
                    -9f at 110
                    8f at 240
                    -4f at 360
                    2f at 450
                },
            )
        }
        tapped = false
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .combinedClickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onLongClick = {
                        // Holding the heart opens "Add to playlist".
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        showAddToPlaylist = true
                    },
                ) {
                    if (unlikeGuard.onTap(isLiked)) {
                        tapped = true
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                        playerConnection.service.toggleLike()
                    } else {
                        // Re-Like Protection: the first tap only cracks the heart.
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    }
                },
    ) {
        // Liked songs show a filled heart in the text color, like in the player.
        Icon(
            painter = painterResource(unlikeGuard.icon(isLiked)),
            contentDescription = null,
            tint = if (isLiked) com.metrolist.music.ui.theme.likedTint(onSurfaceColor) else onSurfaceColor.copy(alpha = 0.8f),
            modifier =
                Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        rotationZ = rotation.value + crackShake.value
                    },
        )
    }
}

@Composable
private fun ArtistButton(
    artistId: String,
    onSurfaceColor: Color,
) {
    val navController = LocalNavController.current
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable { navController.navigate("artist/$artistId") { launchSingleTop = true } },
    ) {
        Icon(
            painter = painterResource(R.drawable.artist),
            contentDescription = stringResource(R.string.open_artist),
            tint = onSurfaceColor.copy(alpha = 0.8f),
            modifier = Modifier.size(26.dp),
        )
    }
}

/** The glass player background (the blurred, darkened cover) at mini player size. */
@Composable
private fun MiniGlassBackground(thumbnailUrl: String?) {
    val context = LocalContext.current
    // Modifier.blur needs Android 12; older versions stretch a tiny image instead.
    val canBlur = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    androidx.compose.animation.Crossfade(targetState = thumbnailUrl, animationSpec = androidx.compose.animation.core.tween(800), label = "miniGlass") { url ->
        Box(Modifier.fillMaxSize()) {
            if (url != null) {
                val imageSize = if (canBlur) 96 else 6
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(context)
                            .data(url)
                            .size(imageSize, imageSize)
                            .allowHardware(false)
                            .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.35f
                                scaleY = 1.35f
                            }.blur(40.dp),
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.36f)))
            }
        }
    }
}

/** The cover and title of the song next to the current one, shown while the mini player is swiped. */
@Composable
private fun MiniSwipePreview(
    metadata: MediaMetadata?,
    onSurfaceColor: Color,
    modifier: Modifier = Modifier,
) {
    if (metadata == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxSize(),
    ) {
        AsyncImage(
            model = metadata.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
            Text(
                text = metadata.title,
                color = onSurfaceColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            if (metadata.artists.any { it.name.isNotBlank() }) {
                Text(
                    text = metadata.artists.joinToString { it.name },
                    color = onSurfaceColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}
