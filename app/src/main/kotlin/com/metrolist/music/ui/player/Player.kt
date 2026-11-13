/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.activity.compose.BackHandler
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.WindowManager
import android.widget.Toast
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.produceState
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import com.metrolist.music.LocalNavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.HidePlayerThumbnailKey
import com.metrolist.music.constants.HideStatusBarOnFullscreenKey
import com.metrolist.music.ui.utils.drawBehindCutout
import com.metrolist.music.constants.KeepScreenOn
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerCardBackground
import com.metrolist.music.constants.PlayerCardBackgroundKey
import com.metrolist.music.constants.PlayerButtonsStyle
import com.metrolist.music.constants.PlayerButtonsStyleKey
import com.metrolist.music.constants.PlayerHorizontalPadding
import com.metrolist.music.constants.QueuePeekHeight
import com.metrolist.music.constants.SleepTimerDefaultKey
import com.metrolist.music.constants.SleepTimerFadeOutKey
import com.metrolist.music.constants.SleepTimerStopAfterCurrentSongKey
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.constants.UseNewPlayerDesignKey
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.listentogether.RoomRole
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.rememberUnlikeGuard
import com.metrolist.music.ui.component.rememberCrackShake
import com.metrolist.music.ui.component.BottomSheet
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.PlayerSliderTrack
import com.metrolist.music.ui.component.ResizableIconButton
import com.metrolist.music.ui.component.StyledSlider
import com.metrolist.music.ui.component.rememberBottomSheetState
import com.metrolist.music.ui.menu.AddToPlaylistDialog
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.theme.MusicardDarkColorScheme
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.ui.theme.albumBackgroundColor
import com.metrolist.music.ui.theme.PlayerSliderColors
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.ui.utils.ShowOffsetDialog
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.safeDataStoreEdit
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.math.roundToInt
import com.metrolist.music.ui.component.Icon as MIcon
import com.metrolist.music.constants.SleepTimerDefaultKey
import com.metrolist.music.constants.SleepTimerFadeOutKey
import com.metrolist.music.constants.SleepTimerStopAfterCurrentSongKey


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val sleepTimerDefaultSetTemplate = stringResource(R.string.sleep_timer_default_set)
    val copiedTitleStr = stringResource(R.string.copied_title)
    val copiedArtistStr = stringResource(R.string.copied_artist)
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) =
        rememberPreference(
            UseNewPlayerDesignKey,
            defaultValue = true,
        )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val (hideStatusBarOnLyrics) = rememberPreference(HideStatusBarOnFullscreenKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, true)
    val edgeToEdgeArtPref by rememberPreference(com.metrolist.music.constants.EdgeToEdgeAlbumArtKey, false)
    val edgeToEdgeArt = edgeToEdgeArtPref && !hidePlayerThumbnail
    val edgeToEdgeArtEdge by rememberEnumPreference(
        com.metrolist.music.constants.EdgeToEdgeArtEdgeKey,
        com.metrolist.music.constants.MiniPlayerEdgeEffect.BLACKOUT,
    )
    val showPlayingFrom by rememberPreference(com.metrolist.music.constants.ShowPlayingFromKey, true)

    var showInlineLyrics by rememberSaveable {
        mutableStateOf(false)
    }

    var isFullScreen by rememberSaveable {
        mutableStateOf(false)
    }

    // Portrait uses the card layout: lyrics / up next live in a card at the bottom, and the
    // full queue sheet is hidden off-screen until it is opened from that card.
    val useCardLayout = LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE

    val playerBackgroundPreference by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT,
    )
    // The card layout always uses a soft color taken from the album art (light text on top),
    // so it runs through the GRADIENT code paths for colors and palette extraction.
    // Battery optimization swaps the blurred backgrounds for the plain album color.
    val batterySaverBlurOff = com.metrolist.music.utils.LocalBatterySaver.current.blur
    val playerBackground =
        if (useCardLayout || (batterySaverBlurOff && playerBackgroundPreference == PlayerBackgroundStyle.BLUR)) {
            PlayerBackgroundStyle.GRADIENT
        } else {
            playerBackgroundPreference
        }
    val playerCardBackgroundPreference by rememberEnumPreference(
        key = PlayerCardBackgroundKey,
        defaultValue = PlayerCardBackground.CLASSIC,
    )
    val playerCardBackground =
        if (batterySaverBlurOff) PlayerCardBackground.CLASSIC else playerCardBackgroundPreference
    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT,
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme =
        remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

    val shouldUseDarkButtonColors =
        remember(playerBackground, useDarkTheme) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> true
                PlayerBackgroundStyle.DEFAULT -> useDarkTheme
            }
        }

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val isKeepScreenOn by rememberPreference(KeepScreenOn, false)
    DisposableEffect(playerBackground, state.isExpanded, useDarkTheme) {
        val window = (context as? android.app.Activity)?.window
        if (window != null && state.isExpanded) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            when (playerBackground) {
                PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> {
                    insetsController.isAppearanceLightStatusBars = false
                }

                PlayerBackgroundStyle.DEFAULT -> {
                    insetsController.isAppearanceLightStatusBars = !useDarkTheme
                }
            }

        }

        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    BackHandler(enabled = state.isExpanded) {
        state.collapseSoft()
    }

    val onBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurface
        }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val automix by playerConnection.service.automixItems.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)

    // Listen Together state (reactive)
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

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
    val castPosition by castHandler?.castPosition?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isExpanded) {
        if (state.isExpanded) {
            delay(100)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore if focus request fails
            }
        }
    }

    // Use Cast state when casting, otherwise local player
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    // Use State objects for position/duration to pass to MiniPlayer without causing recomposition
    // These states persist across playback state changes to ensure continuous progress updates.
    // Seed from the player's current values so re-entering composition on resume shows the real
    // time immediately instead of flashing 0:00 until the first poll fires. runCatching guards the
    // player-not-ready race; the poll loop corrects duration if it isn't known yet.
    val positionState = remember { mutableLongStateOf(runCatching { playerConnection.player.currentPosition }.getOrDefault(0L)) }
    val durationState = remember {
        mutableLongStateOf(
            (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
                ?: runCatching { playerConnection.player.duration }.getOrDefault(0L).coerceAtLeast(0L),
        )
    }

    // Convenience accessors for local use
    var position by positionState
    var duration by durationState

    val effectivePosition by remember {
        derivedStateOf {
            if (isCasting) {
                castPosition
            } else {
                position
            }
        }
    }

    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }
    // Track when we last manually set position to avoid Cast overwriting it
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val defaultGradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            val currentMetadata = mediaMetadata
            if (currentMetadata != null && currentMetadata.thumbnailUrl != null) {
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                    return@LaunchedEffect
                }
                withContext(Dispatchers.IO) {
                    val request =
                        ImageRequest
                            .Builder(context)
                            .data(currentMetadata.thumbnailUrl)
                            .size(100, 100)
                            .allowHardware(false)
                            .memoryCacheKey("gradient_${currentMetadata.id}")
                            .build()

                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    if (result != null) {
                        val bitmap = result.image?.toBitmap()
                        if (bitmap != null) {
                            val palette =
                                withContext(Dispatchers.Default) {
                                    Palette
                                        .from(bitmap)
                                        .maximumColorCount(8)
                                        .resizeBitmapArea(100 * 100)
                                        .generate()
                                }
                            val extractedColors =
                                PlayerColorExtractor.extractGradientColors(
                                    palette = palette,
                                    fallbackColor = fallbackColor,
                                )
                            gradientColorsCache[currentMetadata.id] = extractedColors
                            withContext(Dispatchers.Main) { gradientColors = extractedColors }
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val TextBackgroundColor by animateColorAsState(
        targetValue =
            when (playerBackground) {
                PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
                PlayerBackgroundStyle.BLUR -> Color.White
                PlayerBackgroundStyle.GRADIENT -> Color.White
            },
        label = "TextBackgroundColor",
    )

    val icBackgroundColor by animateColorAsState(
        targetValue =
            when (playerBackground) {
                PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
                PlayerBackgroundStyle.BLUR -> Color.Black
                PlayerBackgroundStyle.GRADIENT -> Color.Black
            },
        label = "icBackgroundColor",
    )

    // "Background color" buttons take the song's color (the old Primary / Tertiary choices count
    // as this one). Until the color is read, they look like Default.
    // The song's color comes from the gradient background when that is used, otherwise straight
    // from the cover, so the buttons follow the song with every background style.
    val coverButtonColor = com.metrolist.music.ui.theme.rememberCoverColor(mediaMetadata?.thumbnailUrl)
    val accentBase = gradientColors.firstOrNull() ?: coverButtonColor
    val accentButtons = playerButtonsStyle != PlayerButtonsStyle.DEFAULT && accentBase != null
    val accentPair =
        remember(accentBase, shouldUseDarkButtonColors) {
            accentBase?.let { com.metrolist.music.ui.theme.playerButtonAccent(it, shouldUseDarkButtonColors) }
        }
    // On a dark player the buttons are a light shade of the song's color, on a light player a
    // dark shade, so they always stand out from the background.
    val accentLight by animateColorAsState(
        accentPair?.first ?: Color.White,
        tween(600),
        label = "buttonAccentLight",
    )
    val accentDeep by animateColorAsState(
        accentPair?.second ?: Color.Black,
        tween(600),
        label = "buttonAccentDeep",
    )

    val (textButtonColor, iconButtonColor) =
        if (accentButtons) {
            Pair(accentLight, accentDeep)
        } else when {
            playerBackground == PlayerBackgroundStyle.BLUR ||
                playerBackground == PlayerBackgroundStyle.GRADIENT -> {
                when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> {
                        Pair(Color.White, Color.Black)
                    }

                    PlayerButtonsStyle.PRIMARY -> {
                        Pair(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    PlayerButtonsStyle.TERTIARY -> {
                        Pair(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                }
            }

            else -> {
                when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> {
                        if (useDarkTheme) {
                            Pair(Color.White, Color.Black)
                        } else {
                            Pair(Color.Black, Color.White)
                        }
                    }

                    PlayerButtonsStyle.PRIMARY -> {
                        Pair(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    PlayerButtonsStyle.TERTIARY -> {
                        Pair(
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                }
            }
        }

    // Separate colors for Previous/Next buttons in PRIMARY/TERTIARY modes
    val (sideButtonContainerColor, sideButtonContentColor) =
        if (accentButtons) {
            Pair(accentLight.copy(alpha = 0.22f), accentLight)
        } else when {
            playerBackground == PlayerBackgroundStyle.BLUR ||
                playerBackground == PlayerBackgroundStyle.GRADIENT -> {
                when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> {
                        Pair(
                            Color.White.copy(alpha = 0.2f),
                            Color.White,
                        )
                    }

                    PlayerButtonsStyle.PRIMARY -> {
                        Pair(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    PlayerButtonsStyle.TERTIARY -> {
                        Pair(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            else -> {
                when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> {
                        Pair(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    PlayerButtonsStyle.PRIMARY -> {
                        Pair(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    PlayerButtonsStyle.TERTIARY -> {
                        Pair(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }

    val download by LocalDownloadUtil.current
        .getDownload(mediaMetadata?.id ?: "")
        .collectAsStateWithLifecycle(initialValue = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer?.triggerTime,
            playerConnection.service.sleepTimer?.pauseWhenSongEnd,
        ) {
            playerConnection.service.sleepTimer?.isActive ?: false
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer?.pauseWhenSongEnd == true) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        (playerConnection.service.sleepTimer?.triggerTime ?: 0L) - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    val scope = rememberCoroutineScope()
    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    var sleepTimerValue by remember { mutableFloatStateOf(sleepTimerDefault) }
    val isAtDefault by remember {
        derivedStateOf { sleepTimerValue.roundToInt() == sleepTimerDefault.roundToInt() }
    }
    LaunchedEffect(sleepTimerDefault) { sleepTimerValue = sleepTimerDefault }
    val sleepTimerStopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val sleepTimerFadeOut by rememberPreference(SleepTimerFadeOutKey, false)


    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.player_sleep),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp),
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = com.metrolist.music.ui.theme.pageRaisedColor(),
            confirmButton = {
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer?.start(
                            minute = sleepTimerValue.roundToInt(),
                            stopAfterCurrentSong = sleepTimerStopAfterCurrentSong,
                            fadeOut = sleepTimerFadeOut,
                        )
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.minute,
                                sleepTimerValue.roundToInt(),
                                sleepTimerValue.roundToInt(),
                            ),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(Modifier.height(8.dp))

                    StyledSlider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                    )

                    Spacer(Modifier.height(12.dp))

                    // Two boxes side by side: Set as default on the left (it reads "Already
                    // default" when the time shown is the default; the box keeps its width), End
                    // of song on the right.
                    val saveDefault: () -> Unit = {
                        scope.launch {
                            context.safeDataStoreEdit { settings ->
                                settings[SleepTimerDefaultKey] = sleepTimerValue
                            }
                        }
                        Toast.makeText(
                            context,
                            String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    ) {
                        SleepTimerChoiceBox(
                            onClick = saveDefault,
                            enabled = !isAtDefault,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) {
                            com.metrolist.music.ui.component.SteadyText(
                                text = stringResource(if (isAtDefault) R.string.already_default else R.string.set_as_default),
                                variants = listOf(stringResource(R.string.set_as_default), stringResource(R.string.already_default)),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        SleepTimerChoiceBox(
                            onClick = {
                                showSleepTimerDialog = false
                                // End of song keeps the fade-out setting too.
                                playerConnection.service.sleepTimer?.start(
                                    minute = -1,
                                    stopAfterCurrentSong = false,
                                    fadeOut = sleepTimerFadeOut,
                                )
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) {
                            Text(
                                text = stringResource(R.string.end_of_song),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // Long-pressing the heart in the card layout opens "Add to playlist" for the current song.
    val database = LocalDatabase.current
    mediaMetadata?.let { metadata ->
        AddToPlaylistDialog(
            isVisible = showChoosePlaylistDialog,
            onGetSong = {
                database.withTransaction { insert(metadata) }
                listOf(metadata.id)
            },
            onGetSongIds = { listOf(metadata.id) },
            onDismiss = { showChoosePlaylistDialog = false },
        )
    }

    // Position update - only for local playback
    // When casting, we use castPosition directly to avoid sync issues
    // Use isPlaying instead of playbackState to ensure continuous updates during playback
    LaunchedEffect(isPlaying, isCasting) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(100) // Update more frequently for smoother progress bar
                if (sliderPosition == null) { // Only update if user isn't dragging
                    position = playerConnection.player.currentPosition
                    // Don't clobber a valid (metadata-derived) duration with 0/UNSET mid-resolve.
                    playerConnection.player.duration.takeIf { it > 0 }?.let { duration = it }
                }
            }
        }
    }

    // Also update position when playback state changes (e.g., song change, seek)
    LaunchedEffect(playbackState, mediaMetadata?.id) {
        if (!isCasting) {
            position = playerConnection.player.currentPosition
            // Prefer the song's known duration (from metadata, available instantly from the restored
            // queue) so the slider range is right even when restored paused / before the stream
            // resolves; fall back to the player's duration once it is known.
            duration = (mediaMetadata?.duration?.takeIf { it > 0 }?.toLong()?.times(1000L))
                ?: playerConnection.player.duration
        }
    }

    // Auto-switch from repeat one to repeat all when song ends naturally
    var previousMediaId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(playbackState, mediaMetadata?.id) {
        val currentId = mediaMetadata?.id

        // Only switch from REPEAT_ONE to REPEAT_ALL when playback naturally ended
        // (i.e., the player transitioned to ENDED state and then moved to next track).
        // Do NOT switch on manual skips.
        if (currentId != null &&
            currentId != previousMediaId &&
            previousMediaId != null &&
            playbackState == Player.STATE_ENDED &&
            repeatMode == Player.REPEAT_MODE_ONE &&
            !isListenTogetherGuest) {
            playerConnection.player.setRepeatMode(Player.REPEAT_MODE_ALL)
        }

        previousMediaId = currentId
    }

    // When casting, use Cast position/duration directly
    // But wait a bit after manual seeks to let Cast catch up
    LaunchedEffect(isCasting, castPosition, castDuration) {
        if (isCasting && sliderPosition == null) {
            val timeSinceManualSeek = System.currentTimeMillis() - lastManualSeekTime
            if (timeSinceManualSeek > 1500) {
                // Only update from Cast if we haven't manually seeked recently
                position = castPosition
                if (castDuration > 0) duration = castDuration
            }
        }
    }

    LaunchedEffect(useCardLayout) {
        if (useCardLayout) {
            isFullScreen = false
            showInlineLyrics = false
        }
    }

    val dismissedBound =
        if (useCardLayout) {
            0.dp
        } else {
            QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
        }

    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = dismissedBound,
            expandedBound = state.expandedBound,
            collapsedBound = if (useCardLayout) dismissedBound else dismissedBound + 1.dp,
            initialAnchor = 1,
        )

    val bottomSheetBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> {
                MaterialTheme.colorScheme.surfaceContainer
            }

            else -> {
                if (useBlackBackground) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            }
        }

    val backgroundAlpha = state.progress.coerceIn(0f, 1f)

    val appDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardAlbumColor by animateColorAsState(
        targetValue = gradientColors.firstOrNull()?.let { albumBackgroundColor(it, appDarkTheme) } ?: DefaultAlbumBackground,
        animationSpec = tween(800),
        label = "albumBackground",
    )
    // Card layout: how far the lyrics / up-next card is opened toward full screen (0..1). It
    // follows the finger while the card is dragged up. Fully opened, the card itself is the
    // full-screen lyrics / up-next view (no separate page is swapped in), so opening and closing
    // is one continuous movement.
    var cardExpand by remember { mutableFloatStateOf(0f) }
    var cardOpen by remember { mutableStateOf(false) }
    // Distance from the card's top to the top of the screen, i.e. the drag that fully opens it.
    var cardTravelPx by remember { mutableFloatStateOf(1f) }
    var cardPage by remember { mutableIntStateOf(LyricsPage) }
    val cardExpandScope = rememberCoroutineScope()
    // Kept at 1: the full queue sheet is no longer opened from the card.
    val queueFade = remember { Animatable(1f) }
    var cardExpandJob by remember { mutableStateOf<Job?>(null) }
    // Bumped on every touch while the card is open; shows the auto-hidden controls again.
    var cardTouchTick by remember { mutableIntStateOf(0) }
    var cardControlsShown by remember { mutableStateOf(true) }
    // The opened lyrics stop following the song after a manual scroll, until re-sync is tapped.
    val lyricsFollow = remember { LyricsFollowState() }
    val animateCardExpand: suspend (Float, Int) -> Unit = { target, millis ->
        animate(cardExpand, target, animationSpec = tween(millis, easing = FastOutSlowInEasing)) { value, _ ->
            cardExpand = value
        }
    }
    val openCardFull: (Int) -> Unit = { _ ->
        cardExpandJob?.cancel()
        cardExpandJob =
            cardExpandScope.launch {
                animateCardExpand(1f, (CardExpandMillis * (1f - cardExpand)).toInt().coerceAtLeast(120))
                cardOpen = true
            }
    }
    val closeCardFull: () -> Unit = {
        cardExpandJob?.cancel()
        cardExpandJob =
            cardExpandScope.launch {
                cardOpen = false
                animateCardExpand(0f, (CardExpandMillis * cardExpand).toInt().coerceAtLeast(120))
            }
    }
    val onCardDrag: (Int, Float) -> Unit = { _, deltaPx ->
        cardExpandJob?.cancel()
        cardExpand = (cardExpand - deltaPx / cardTravelPx).coerceIn(0f, 1f)
    }
    val onCardDragEnd: (Int, Float) -> Unit = { page, velocity ->
        val open =
            if (cardOpen) {
                // Opened: a short pull down is enough to close it again.
                !(velocity > CardFlingVelocity || (velocity > -CardFlingVelocity && cardExpand < 0.8f))
            } else {
                velocity < -CardFlingVelocity || (velocity < CardFlingVelocity && cardExpand > 0.4f)
            }
        if (open) {
            openCardFull(page)
        } else if (cardExpand > 0f || cardOpen) {
            closeCardFull()
        }
    }

    // Lyrics fill the screen without the controls after a few seconds; any touch brings them back.
    LaunchedEffect(cardOpen, cardPage, cardTouchTick) {
        cardControlsShown = true
        if (cardOpen && cardPage == LyricsPage) {
            delay(ControlsAutoHideMillis)
            // The controls hide and the lyrics find the current line again at the same moment.
            cardControlsShown = false
            lyricsFollow.paused = false
        }
    }

    // "Keep screen awake in lyrics mode": while the lyrics are opened (full screen, or enlarged
    // with the lyrics button) and music plays, the screen doesn't turn off.
    val lyricsMode = state.isExpanded && cardPage == LyricsPage && (cardOpen || showInlineLyrics)
    val keepAwake = isKeepScreenOn && isPlaying && lyricsMode
    DisposableEffect(keepAwake) {
        val window = (context as? android.app.Activity)?.window
        if (keepAwake) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // "Hide status bar on lyrics mode": once the opened lyrics follow the song and the controls
    // have slid away, the status bar hides too; it comes back with the controls.
    val hideStatusBar =
        hideStatusBarOnLyrics && state.isExpanded && cardOpen && cardPage == LyricsPage &&
            !cardControlsShown && !lyricsFollow.paused
    DisposableEffect(hideStatusBar) {
        val window = (context as? android.app.Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (hideStatusBar) {
            // Keeps the camera cutout strip showing the player instead of turning black.
            window?.drawBehindCutout(true)
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            if (hideStatusBar) {
                insetsController?.show(WindowInsetsCompat.Type.statusBars())
                window?.drawBehindCutout(false)
            }
        }
    }

    LaunchedEffect(state.isExpanded) {
        if (!state.isExpanded) {
            cardOpen = false
            cardExpandJob?.cancel()
            cardExpand = 0f
        }
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(bottomSheetBackgroundColor),
            ) {
                when (playerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        AnimatedContent(
                            targetState = mediaMetadata?.thumbnailUrl,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "blurBackground",
                        ) { thumbnailUrl ->
                            if (thumbnailUrl != null) {
                                Box(modifier = Modifier.alpha(backgroundAlpha)) {
                                    AsyncImage(
                                        model =
                                            ImageRequest
                                                .Builder(context)
                                                .data(thumbnailUrl)
                                                .size(100, 100)
                                                .allowHardware(false)
                                                .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .blur(if (useDarkTheme) 150.dp else 100.dp),
                                    )
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f)),
                                    )
                                }
                            }
                        }
                    }

                    PlayerBackgroundStyle.GRADIENT -> if (useCardLayout) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .alpha(backgroundAlpha)
                                .background(albumBackgroundBrush(cardAlbumColor)),
                        )
                        if (playerCardBackground == PlayerCardBackground.GLASS) {
                            GlassCoverBackground(
                                thumbnailUrl = mediaMetadata?.thumbnailUrl,
                                modifier = Modifier.alpha(backgroundAlpha),
                            )
                        }
                    } else {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "gradientBackground",
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                val gradientColorStops =
                                    if (colors.size >= 3) {
                                        arrayOf(
                                            0.0f to colors[0],
                                            0.5f to colors[1],
                                            1.0f to colors[2],
                                        )
                                    } else {
                                        arrayOf(
                                            0.0f to colors[0],
                                            0.6f to colors[0].copy(alpha = 0.7f),
                                            1.0f to Color.Black,
                                        )
                                    }
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                        .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                )
                            }
                        }
                    }

                    else -> {
                        PlayerBackgroundStyle.DEFAULT
                    }
                }
            }
        },
        onDismiss =
            if (!isListenTogetherGuest) {
                {
                    playerConnection.service.clearAutomix()
                    playerConnection.player.stop()
                    playerConnection.player.clearMediaItems()
                }
            } else {
                null
            },
        collapsedContent = {
            MiniPlayer(
                positionState = positionState,
                durationState = durationState,
                onClick = { state.expandSoft() },
            )
        },
    ) {
        val onPlayPauseClick: () -> Unit = {
            if (isListenTogetherGuest) {
                playerConnection.toggleMute()
            } else if (isCasting) {
                if (castIsPlaying) castHandler?.pause() else castHandler?.play()
            } else if (playbackState == STATE_ENDED) {
                playerConnection.player.seekTo(0, 0)
                playerConnection.player.playWhenReady = true
            } else {
                playerConnection.togglePlayPause()
            }
        }

        // Title row (title, artist, heart) and the playback part (progress bar, buttons) are separate
        // so the card layout can move them apart while the lyrics card is dragged open.
        val titleRowContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
            ) {
                AnimatedContent(
                    targetState = showInlineLyrics,
                    label = "ThumbnailAnimation",
                ) { showLyrics ->
                    if (showLyrics) {
                        val showThumbnailOverlay =
                            isFullScreen &&
                                (
                                    (isListenTogetherGuest && isMuted) ||
                                        (!isListenTogetherGuest && (!effectiveIsPlaying || playbackState == STATE_ENDED))
                                )
                        val thumbnailActionLabel =
                            if (isListenTogetherGuest) {
                                if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute)
                            } else if (effectiveIsPlaying) {
                                stringResource(R.string.pause)
                            } else {
                                stringResource(R.string.play)
                            }
                        Row {
                            Box(
                                modifier =
                                    Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                        .then(
                                            if (isFullScreen) {
                                                Modifier.semantics { contentDescription = thumbnailActionLabel }
                                            } else {
                                                Modifier.clearAndSetSemantics {}
                                            },
                                        )
                                        .clickable(
                                            enabled = isFullScreen,
                                            role = Role.Button,
                                            onClickLabel = thumbnailActionLabel,
                                        ) {
                                            if (isListenTogetherGuest) {
                                                playerConnection.toggleMute()
                                            } else if (isCasting) {
                                                if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                                            } else if (playbackState == STATE_ENDED) {
                                                playerConnection.player.seekTo(0, 0)
                                                playerConnection.player.playWhenReady = true
                                            } else {
                                                playerConnection.togglePlayPause()
                                            }
                                        },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (hidePlayerThumbnail) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.small_icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = textButtonColor.copy(alpha = 0.7f),
                                        )
                                    }
                                } else {
                                    AsyncImage(
                                        model = mediaMetadata.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                if (showThumbnailOverlay) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                    )
                                    Icon(
                                        painter =
                                            painterResource(
                                                when {
                                                    isListenTogetherGuest ->
                                                        if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                                    playbackState == STATE_ENDED -> R.drawable.replay
                                                    else -> R.drawable.play
                                                },
                                            ),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(0.dp))
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    AnimatedContent(
                        targetState = mediaMetadata.title,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "",
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = TextBackgroundColor,
                            modifier =
                                Modifier
                                    .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                    .combinedClickable(
                                        enabled = true,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = {
                                            val albumId = mediaMetadata.album?.id
                                                ?: currentSong?.album?.id
                                                ?: currentSong?.song?.albumId
                                            if (albumId != null) {
                                                navController.navigate("album/$albumId")
                                                state.collapseSoft()
                                            }
                                        },
                                        onLongClick = {
                                            val clip = ClipData.newPlainText(copiedTitleStr, title)
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast
                                                .makeText(context, copiedTitleStr, Toast.LENGTH_SHORT)
                                                .show()
                                        },
                                    ),
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (mediaMetadata.explicit) MIcon.Explicit()

                        if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                            val annotatedString =
                                buildAnnotatedString {
                                    mediaMetadata.artists.forEachIndexed { index, artist ->
                                        val tag = "artist_${artist.id.orEmpty()}"
                                        pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                                        withStyle(SpanStyle(color = TextBackgroundColor, fontSize = 16.sp)) {
                                            append(artist.name)
                                        }
                                        pop()
                                        if (index != mediaMetadata.artists.lastIndex) append(", ")
                                    }
                                }

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                                        .padding(end = 12.dp),
                            ) {
                                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                var clickOffset by remember { mutableStateOf<Offset?>(null) }
                                Text(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextBackgroundColor),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { layoutResult = it },
                                    modifier =
                                        Modifier
                                            .pointerInput(Unit) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val tapPosition = event.changes.firstOrNull()?.position
                                                        if (tapPosition != null) {
                                                            clickOffset = tapPosition
                                                        }
                                                    }
                                                }
                                            }.combinedClickable(
                                                enabled = true,
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() },
                                                onClick = {
                                                    val tapPosition = clickOffset
                                                    val layout = layoutResult
                                                    if (tapPosition != null && layout != null) {
                                                        val offset = layout.getOffsetForPosition(tapPosition)
                                                        annotatedString
                                                            .getStringAnnotations(offset, offset)
                                                            .firstOrNull()
                                                            ?.let { ann ->
                                                                val artistId = ann.item
                                                                if (artistId.isNotBlank()) {
                                                                    navController.navigate("artist/$artistId")
                                                                    state.collapseSoft()
                                                                }
                                                            }
                                                    }
                                                },
                                                onLongClick = {
                                                    val clip =
                                                        ClipData.newPlainText(
                                                            copiedArtistStr,
                                                            annotatedString,
                                                        )
                                                    clipboardManager.setPrimaryClip(clip)
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            copiedArtistStr,
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                },
                                            ),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (useCardLayout) {
                    // For episodes, show saved state (inLibrary); for songs, show liked state
                    val isEpisode = currentSong?.song?.isEpisode == true
                    val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                    PlayerLikeButton(
                        liked = isFavorite,
                        contentColor = TextBackgroundColor,
                        onClick = playerConnection::toggleLike,
                        onLongClick = { showChoosePlaylistDialog = true },
                        // Fades out quickly while the card is dragged open.
                        modifier = Modifier.graphicsLayer { alpha = (1f - cardExpand * 3f).coerceIn(0f, 1f) },
                    )
                } else if (useNewPlayerDesign) {
                    val shareShape =
                        RoundedCornerShape(
                            topStart = 50.dp,
                            bottomStart = 50.dp,
                            topEnd = 3.dp,
                            bottomEnd = 3.dp,
                        )

                    val favShape =
                        RoundedCornerShape(
                            topStart = 3.dp,
                            bottomStart = 3.dp,
                            topEnd = 50.dp,
                            bottomEnd = 50.dp,
                        )

                    val middleShape = RoundedCornerShape(3.dp)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AnimatedContent(targetState = showInlineLyrics, label = "ShareButton") { showLyrics ->
                            if (showLyrics) {
                                FilledIconButton(
                                    onClick = { isFullScreen = !isFullScreen },
                                    shape = shareShape,
                                    colors =
                                        IconButtonDefaults.filledIconButtonColors(
                                            containerColor = textButtonColor,
                                            contentColor = iconButtonColor,
                                        ),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.fullscreen),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            } else {
                                FilledIconButton(
                                    onClick = {
                                        val intent =
                                            Intent().apply {
                                                action = Intent.ACTION_SEND
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "https://music.youtube.com/watch?v=${mediaMetadata.id}",
                                                )
                                            }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    },
                                    shape = shareShape,
                                    colors =
                                        IconButtonDefaults.filledIconButtonColors(
                                            containerColor = textButtonColor,
                                            contentColor = iconButtonColor,
                                        ),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.share),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }

                        AnimatedContent(targetState = showInlineLyrics, label = "LikeButton") { showLyrics ->
                            if (showLyrics) {
                                val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
                                FilledIconButton(
                                    onClick = {
                                        menuState.show {
                                            com.metrolist.music.ui.menu.LyricsMenu(
                                                lyricsProvider = { currentLyrics },
                                                songProvider = { currentSong?.song },
                                                mediaMetadataProvider = { mediaMetadata },
                                                onDismiss = menuState::dismiss,
                                                onShowOffsetDialog = {
                                                    bottomSheetPageState.show {
                                                        ShowOffsetDialog(
                                                            songProvider = { currentSong?.song },
                                                        )
                                                    }
                                                },
                                            )
                                        }
                                    },
                                    shape = favShape,
                                    colors =
                                        IconButtonDefaults.filledIconButtonColors(
                                            containerColor = textButtonColor,
                                            contentColor = iconButtonColor,
                                        ),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_horiz),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            } else {
                                // For episodes, show saved state (inLibrary); for songs, show liked state
                                val isEpisode = currentSong?.song?.isEpisode == true
                                val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                                FilledIconButton(
                                    onClick = playerConnection::toggleLike,
                                    shape = favShape,
                                    colors =
                                        IconButtonDefaults.filledIconButtonColors(
                                            containerColor = textButtonColor,
                                            contentColor = iconButtonColor,
                                        ),
                                    modifier = Modifier.size(42.dp),
                                ) {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (isFavorite) {
                                                    R.drawable.favorite
                                                } else {
                                                    R.drawable.favorite_border
                                                },
                                            ),
                                        contentDescription = null,
                                        tint = if (isFavorite) com.metrolist.music.ui.theme.likedTint(iconButtonColor) else iconButtonColor,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    AnimatedContent(targetState = showInlineLyrics, label = "ShareButton") { showLyrics ->
                        if (showLyrics) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(textButtonColor)
                                        .clickable { isFullScreen = !isFullScreen },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.fullscreen),
                                    contentDescription = null,
                                    tint = iconButtonColor,
                                    modifier =
                                        Modifier
                                            .align(Alignment.Center)
                                            .size(24.dp),
                                )
                            }
                        } else {
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(textButtonColor)
                                        .clickable {
                                            val intent =
                                                Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    type = "text/plain"
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        "https://music.youtube.com/watch?v=${mediaMetadata.id}",
                                                    )
                                                }
                                            context.startActivity(Intent.createChooser(intent, null))
                                        },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.share),
                                    contentDescription = null,
                                    tint = iconButtonColor,
                                    modifier =
                                        Modifier
                                            .align(Alignment.Center)
                                            .size(24.dp),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    AnimatedContent(targetState = showInlineLyrics, label = "LikeButton") { showLyrics ->
                        if (showLyrics) {
                            val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(textButtonColor)
                                        .clickable {
                                            menuState.show {
                                                com.metrolist.music.ui.menu.LyricsMenu(
                                                    lyricsProvider = { currentLyrics },
                                                    songProvider = { currentSong?.song },
                                                    mediaMetadataProvider = { mediaMetadata },
                                                    onDismiss = menuState::dismiss,
                                                    onShowOffsetDialog = {
                                                        bottomSheetPageState.show {
                                                            ShowOffsetDialog(
                                                                songProvider = { currentSong?.song },
                                                            )
                                                        }
                                                    },
                                                )
                                            }
                                        },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_horiz),
                                    contentDescription = null,
                                    tint = iconButtonColor,
                                    modifier =
                                        Modifier
                                            .align(Alignment.Center)
                                            .size(24.dp),
                                )
                            }
                        } else {
                            PlayerMoreMenuButton(
                                mediaMetadata = mediaMetadata,
                                state = state,
                                textButtonColor = textButtonColor,
                                iconButtonColor = iconButtonColor,
                            )
                        }
                    }
                }
            }
        }

        val playbackControlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val playPauseRoundness by animateDpAsState(
                targetValue = if (isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "playPauseRoundness",
            )

            Spacer(Modifier.height(24.dp))

            // While playback is stuck waiting for data (not every short load), a band keeps
            // running along the rest of the bar, like a classic loading bar.
            val stalled by produceState(false, playbackState, mediaMetadata.id) {
                value = false
                if (playbackState == Player.STATE_BUFFERING && playerConnection.player.playWhenReady) {
                    delay(StallIndicatorDelayMs)
                    value = playerConnection.player.currentPosition > 500L
                }
            }
            Box {
                when (sliderStyle) {
                    SliderStyle.DEFAULT -> {
                        Slider(
                            value = (sliderPosition ?: effectivePosition).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                if (!isListenTogetherGuest) {
                                    sliderPosition = it.toLong()
                                }
                            },
                            onValueChangeFinished = {
                                if (!isListenTogetherGuest) {
                                    sliderPosition?.let {
                                        if (isCasting) {
                                            castHandler?.seekTo(it)
                                            lastManualSeekTime = System.currentTimeMillis()
                                        } else {
                                            playerConnection.player.seekTo(it)
                                        }
                                        position = it
                                    }
                                    sliderPosition = null
                                }
                            },
                            enabled = !isListenTogetherGuest,
                            colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme),
                            modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                        )
                    }

                    SliderStyle.SLIM -> {
                        val sliderValue = (sliderPosition ?: effectivePosition).toFloat()
                        val sliderRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat())
                        val sliderState = remember(sliderRange) {
                            SliderState(value = sliderValue, trackRange = sliderRange)
                        }
                        sliderState.value = sliderValue
                        Slider(
                            state = sliderState,
                            onValueChange = {
                                if (!isListenTogetherGuest) {
                                    sliderPosition = it.toLong()
                                }
                            },
                            onValueChangeFinished = {
                                if (!isListenTogetherGuest) {
                                    sliderPosition?.let {
                                        if (isCasting) {
                                            castHandler?.seekTo(it)
                                            lastManualSeekTime = System.currentTimeMillis()
                                        } else {
                                            playerConnection.player.seekTo(it)
                                        }
                                        position = it
                                    }
                                    sliderPosition = null
                                }
                            },
                            enabled = !isListenTogetherGuest,
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState ->
                                PlayerSliderTrack(
                                    sliderState = sliderState,
                                    colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme),
                                )
                            },
                            modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                        )
                    }
                }
                BufferingSweep(
                    visible = stalled && sliderPosition == null,
                    fraction = {
                        val total = duration
                        if (total <= 0L || total == C.TIME_UNSET) 0f else (effectivePosition.toFloat() / total).coerceIn(0f, 1f)
                    },
                    color = textButtonColor,
                    modifier =
                        Modifier
                            .matchParentSize()
                            .padding(horizontal = PlayerHorizontalPadding + 6.dp),
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding + 4.dp),
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: effectivePosition),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(if (useCardLayout) 8.dp else 24.dp))

            AnimatedVisibility(
                visible = !isFullScreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Column {
                    if (useCardLayout) {
                        // "Background color" buttons: the song's color, brightened on a dark player
                        // and darkened on a light one.
                        val cardControlColor = if (accentButtons) accentLight else TextBackgroundColor
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PlayerHorizontalPadding),
                        ) {
                            PlayerControlButton(
                                icon = R.drawable.player_backward,
                                contentDescription = null,
                                enabled = canSkipPrevious && !isListenTogetherGuest,
                                color = cardControlColor,
                                iconSize = 52.dp,
                                onClick = playerConnection::seekToPrevious,
                                modifier = Modifier.size(72.dp),
                            )

                            PlayerControlButton(
                                icon =
                                    when {
                                        isListenTogetherGuest ->
                                            if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                        playbackState == STATE_ENDED -> R.drawable.replay
                                        effectiveIsPlaying -> R.drawable.player_pause
                                        else -> R.drawable.player_play
                                    },
                                contentDescription =
                                    if (isListenTogetherGuest) {
                                        if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute)
                                    } else {
                                        if (effectiveIsPlaying) stringResource(R.string.player_pause) else stringResource(R.string.play)
                                    },
                                enabled = true,
                                color = cardControlColor,
                                iconSize = 60.dp,
                                onClick = onPlayPauseClick,
                                modifier =
                                    Modifier
                                        .size(80.dp)
                                        .focusRequester(focusRequester),
                            )

                            PlayerControlButton(
                                icon = R.drawable.player_forward,
                                contentDescription = null,
                                enabled = canSkipNext && !isListenTogetherGuest,
                                color = cardControlColor,
                                iconSize = 52.dp,
                                onClick = playerConnection::seekToNext,
                                modifier = Modifier.size(72.dp),
                            )
                        }
                    } else if (useNewPlayerDesign) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PlayerHorizontalPadding),
                        ) {
                            val backInteractionSource = remember { MutableInteractionSource() }
                            val nextInteractionSource = remember { MutableInteractionSource() }
                            val playPauseInteractionSource = remember { MutableInteractionSource() }

                            val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
                            val isBackPressed by backInteractionSource.collectIsPressedAsState()
                            val isNextPressed by nextInteractionSource.collectIsPressedAsState()

                            val playPauseWeight by animateFloatAsState(
                                targetValue =
                                    if (isPlayPausePressed) {
                                        1.9f
                                    } else if (isBackPressed || isNextPressed) {
                                        1.1f
                                    } else {
                                        1.3f
                                    },
                                animationSpec =
                                    spring(
                                        dampingRatio = 0.6f,
                                        stiffness = 500f,
                                    ),
                                label = "playPauseWeight",
                            )

                            val backButtonWeight by animateFloatAsState(
                                targetValue =
                                    if (isBackPressed) {
                                        0.65f
                                    } else if (isPlayPausePressed) {
                                        0.35f
                                    } else {
                                        0.45f
                                    },
                                animationSpec =
                                    spring(
                                        dampingRatio = 0.6f,
                                        stiffness = 500f,
                                    ),
                                label = "backButtonWeight",
                            )

                            val nextButtonWeight by animateFloatAsState(
                                targetValue =
                                    if (isNextPressed) {
                                        0.65f
                                    } else if (isPlayPausePressed) {
                                        0.35f
                                    } else {
                                        0.45f
                                    },
                                animationSpec =
                                    spring(
                                        dampingRatio = 0.6f,
                                        stiffness = 500f,
                                    ),
                                label = "nextButtonWeight",
                            )

                            FilledIconButton(
                                onClick = playerConnection::seekToPrevious,
                                enabled = canSkipPrevious && !isListenTogetherGuest,
                                shape = RoundedCornerShape(50),
                                interactionSource = backInteractionSource,
                                colors =
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = sideButtonContainerColor,
                                        contentColor = sideButtonContentColor,
                                    ),
                                modifier =
                                    Modifier
                                        .height(68.dp)
                                        .weight(backButtonWeight),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_previous),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            FilledIconButton(
                                onClick = {
                                    if (isListenTogetherGuest) {
                                        playerConnection.toggleMute()
                                        return@FilledIconButton
                                    }
                                    if (isCasting) {
                                        if (castIsPlaying) {
                                            castHandler?.pause()
                                        } else {
                                            castHandler?.play()
                                        }
                                    } else if (playbackState == STATE_ENDED) {
                                        playerConnection.player.seekTo(0, 0)
                                        playerConnection.player.playWhenReady = true
                                    } else {
                                        playerConnection.togglePlayPause()
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                interactionSource = playPauseInteractionSource,
                                colors =
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = textButtonColor,
                                        contentColor = iconButtonColor,
                                    ),
                                modifier =
                                    Modifier
                                        .height(68.dp)
                                        .weight(playPauseWeight)
                                        .focusRequester(focusRequester),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (isListenTogetherGuest) {
                                                    if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                                } else {
                                                    if (effectiveIsPlaying) R.drawable.pause else R.drawable.play
                                                },
                                            ),
                                        contentDescription =
                                            if (isListenTogetherGuest) {
                                                if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute)
                                            } else {
                                                if (effectiveIsPlaying) stringResource(R.string.player_pause) else stringResource(R.string.play)
                                            },
                                        modifier = Modifier.size(32.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text =
                                            if (isListenTogetherGuest) {
                                                if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute)
                                            } else {
                                                if (effectiveIsPlaying) stringResource(R.string.player_pause) else stringResource(R.string.play)
                                            },
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            FilledIconButton(
                                onClick = playerConnection::seekToNext,
                                enabled = canSkipNext && !isListenTogetherGuest,
                                shape = RoundedCornerShape(50),
                                interactionSource = nextInteractionSource,
                                colors =
                                    IconButtonDefaults.filledIconButtonColors(
                                        containerColor = sideButtonContainerColor,
                                        contentColor = sideButtonContentColor,
                                    ),
                                modifier =
                                    Modifier
                                        .height(68.dp)
                                        .weight(nextButtonWeight),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_next),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = PlayerHorizontalPadding),
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ResizableIconButton(
                                    icon =
                                        when (repeatMode) {
                                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                            else -> throw IllegalStateException()
                                        },
                                    color = TextBackgroundColor,
                                    modifier =
                                        Modifier
                                            .size(32.dp)
                                            .padding(4.dp)
                                            .align(Alignment.Center)
                                            .alpha(
                                                if (isListenTogetherGuest || repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f,
                                            ),
                                    enabled = !isListenTogetherGuest,
                                    onClick = {
                                        playerConnection.player.toggleRepeatMode()
                                    },
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                ResizableIconButton(
                                    icon = R.drawable.skip_previous,
                                    enabled = canSkipPrevious && !isListenTogetherGuest,
                                    color = TextBackgroundColor,
                                    modifier =
                                        Modifier
                                            .size(32.dp)
                                            .align(Alignment.Center)
                                            .alpha(if (isListenTogetherGuest) 0.5f else 1f),
                                    onClick = playerConnection::seekToPrevious,
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(
                                modifier =
                                    Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(playPauseRoundness))
                                        .background(textButtonColor)
                                        .clickable {
                                            if (isListenTogetherGuest) {
                                                playerConnection.toggleMute()
                                                return@clickable
                                            }
                                            if (isCasting) {
                                                if (castIsPlaying) {
                                                    castHandler?.pause()
                                                } else {
                                                    castHandler?.play()
                                                }
                                            } else if (playbackState == STATE_ENDED) {
                                                playerConnection.player.seekTo(0, 0)
                                                playerConnection.player.playWhenReady = true
                                            } else {
                                                playerConnection.player.togglePlayPause()
                                            }
                                        }
                                        .focusRequester(focusRequester),
                            ) {
                                Image(
                                    painter =
                                        painterResource(
                                            if (isListenTogetherGuest) {
                                                if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                            } else if (playbackState ==
                                                STATE_ENDED
                                            ) {
                                                R.drawable.replay
                                            } else if (effectiveIsPlaying) {
                                                R.drawable.pause
                                            } else {
                                                R.drawable.play
                                            },
                                        ),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(iconButtonColor),
                                    modifier =
                                        Modifier
                                            .align(Alignment.Center)
                                            .size(36.dp),
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                ResizableIconButton(
                                    icon = R.drawable.skip_next,
                                    enabled = canSkipNext && !isListenTogetherGuest,
                                    color = TextBackgroundColor,
                                    modifier =
                                        Modifier
                                            .size(32.dp)
                                            .align(Alignment.Center)
                                            .alpha(if (isListenTogetherGuest) 0.5f else 1f),
                                    onClick = playerConnection::seekToNext,
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                // For episodes, show saved state (inLibrary); for songs, show liked state
                                val isEpisode = currentSong?.song?.isEpisode == true
                                val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true
                                ResizableIconButton(
                                    icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
                                    color = if (isFavorite) com.metrolist.music.ui.theme.likedTint(TextBackgroundColor) else TextBackgroundColor,
                                    modifier =
                                        Modifier
                                            .size(32.dp)
                                            .padding(4.dp)
                                            .align(Alignment.Center),
                                    onClick = playerConnection::toggleLike,
                                )
                            }
                        }
                    }
                }
            }
        }

        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            titleRowContent(mediaMetadata)
            playbackControlsContent(mediaMetadata)
        }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                // Calculate vertical padding like OuterTune
                val density = LocalDensity.current
                val verticalPadding =
                    max(
                        WindowInsets.systemBars.getTop(density),
                        WindowInsets.systemBars.getBottom(density),
                    )
                val verticalPaddingDp = with(density) { verticalPadding.toDp() }
                val verticalWindowInsets = WindowInsets(left = 0.dp, top = verticalPaddingDp, right = 0.dp, bottom = verticalPaddingDp)

                Row(
                    modifier =
                        Modifier
                            .windowInsetsPadding(
                                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).add(verticalWindowInsets),
                            ).padding(bottom = 24.dp)
                            .fillMaxSize(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .weight(1f)
                                .nestedScroll(state.preUpPostDownNestedScrollConnection),
                    ) {
                        // Remember lambdas to prevent unnecessary recomposition
                        val currentSliderPosition by rememberUpdatedState(sliderPosition)
                        val sliderPositionProvider = remember { { currentSliderPosition } }
                        val isExpandedProvider = remember(state) { { state.isExpanded } }
                        AnimatedContent(
                            targetState = showInlineLyrics,
                            label = "Lyrics",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                        ) { showLyrics ->
                            if (showLyrics) {
                                InlineLyricsView(
                                    mediaMetadata = mediaMetadata,
                                    showLyrics = showLyrics,
                                    positionProvider = { effectivePosition },
                                )
                            } else {
                                Thumbnail(
                                    sliderPositionProvider = sliderPositionProvider,
                                    modifier = Modifier.animateContentSize(),
                                    isPlayerExpanded = isExpandedProvider,
                                    isLandscape = true,
                                    isListenTogetherGuest = isListenTogetherGuest,
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .weight(if (showInlineLyrics) 0.65f else 1f, false)
                                .animateContentSize()
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                    ) {
                        Spacer(Modifier.weight(1f))

                        mediaMetadata?.let {
                            controlsContent(it)
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val density = LocalDensity.current
                val navBottomPx = with(density) { WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().toPx() }
                // Where the playback controls sit at rest, and where they end up in the full-screen
                // lyrics (bottom panel). Measured only while the card is closed.
                var boxBottomPx by remember { mutableFloatStateOf(0f) }
                var boxTopPx by remember { mutableFloatStateOf(0f) }
                var cardTopPx by remember { mutableFloatStateOf(0f) }
                val thumbnailCarousel = remember { ThumbnailCarousel() }
                var playbackTopPx by remember { mutableFloatStateOf(0f) }
                var playbackHeightPx by remember { mutableFloatStateOf(0f) }
                // Art and title row at rest (measured while the card is closed). While the card opens
                // they shrink and move to the header of the full-screen lyrics (48 dp art at the
                // player margin under the status bar, title next to it).
                var artBoxPos by remember { mutableStateOf(Offset.Zero) }
                var artBoxSize by remember { mutableStateOf(IntSize.Zero) }
                var titleRowPos by remember { mutableStateOf(Offset.Zero) }
                val headerTopPx = with(density) { (topInset + 8.dp).toPx() }
                val headerArtPx = with(density) { 48.dp.toPx() }
                val marginPx = with(density) { PlayerHorizontalPadding.toPx() }
                val headerTitleLeftPx = with(density) { (PlayerHorizontalPadding + 48.dp + 12.dp).toPx() }
                val headerTitleTopPx = headerTopPx + with(density) { 2.dp.toPx() }
                val cardControlsAlpha by animateFloatAsState(
                    targetValue = if (cardControlsShown) 1f else 0f,
                    animationSpec = tween(FullViewFadeMillis),
                    label = "cardControlsAlpha",
                )
                val playbackTravelPx = {
                    (boxBottomPx - navBottomPx - with(density) { 12.dp.toPx() } - playbackHeightPx - playbackTopPx)
                        .coerceAtLeast(0f)
                }
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .onGloballyPositioned {
                                boxTopPx = it.positionInRoot().y
                                boxBottomPx = it.positionInRoot().y + it.size.height
                            }
                            // Any touch on the opened card shows its auto-hidden controls again.
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.type == PointerEventType.Press && cardOpen) cardTouchTick++
                                    }
                                }
                            },
                ) {
                    // Leave room for the controls (~240dp) and a lyrics card that fits a few lines (~200dp).
                    val artSize =
                        maxOf(
                            minOf(maxWidth - PlayerHorizontalPadding * 2, maxHeight - topInset - 500.dp),
                            120.dp,
                        )
                    if (edgeToEdgeArt) {
                        // Edge to edge: the art fills the top of the player behind the handle,
                        // title and controls. The usual art stays in place (invisible) for
                        // swiping; while the card opens this art itself shrinks into the lyrics /
                        // up next header.
                        val titleTopInBox by remember {
                            derivedStateOf { (titleRowPos.y - boxTopPx).roundToInt() }
                        }
                        val cardTopInBox by remember {
                            derivedStateOf { (cardTopPx - boxTopPx).roundToInt() }
                        }
                        val controlsBottomInBox by remember {
                            derivedStateOf { (playbackTopPx + playbackHeightPx - boxTopPx).roundToInt() }
                        }
                        val blackout = edgeToEdgeArtEdge == com.metrolist.music.constants.MiniPlayerEdgeEffect.BLACKOUT
                        val tailPx = with(density) { EdgeToEdgeTail.toPx() }
                        val fallbackHeightPx = with(density) { (topInset + 44.dp + artSize + 110.dp).toPx() }
                        // Blackout keeps its height and fade. Nothing and Blur reach down behind
                        // the play controls and stop just above the lyrics / up next card, with a
                        // short fade at the very end so the art never runs into the card.
                        val nothing = edgeToEdgeArtEdge == com.metrolist.music.constants.MiniPlayerEdgeEffect.NONE
                        val edgeArtHeightPx =
                            when {
                                blackout || playbackHeightPx == 0f -> fallbackHeightPx
                                // Nothing: sharp all the way down to the lyrics / up next card,
                                // and cut off exactly where the card starts.
                                nothing && cardTopPx > 0f -> cardTopInBox.toFloat()
                                else -> controlsBottomInBox.toFloat()
                            }
                        val blurStartPx =
                            if (titleRowPos.y == 0f) edgeArtHeightPx * 0.6f else titleTopInBox.toFloat()
                        EdgeToEdgeAlbumArt(
                            artworkUrl = mediaMetadata?.thumbnailUrl,
                            carousel = thumbnailCarousel,
                            slide = rememberSongChangeSlide(),
                            isPlaying = effectiveIsPlaying,
                            edge = edgeToEdgeArtEdge,
                            heightPx = edgeArtHeightPx,
                            tailPx = if (nothing) 0f else tailPx,
                            blurStartPx = blurStartPx,
                            expandProvider = { cardExpand },
                            headerLeftPx = marginPx,
                            headerTopPx = headerTopPx,
                            headerSizePx = headerArtPx,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Spacer(Modifier.height(topInset))

                        Box(
                            modifier =
                                Modifier
                                    .graphicsLayer { alpha = (1f - cardExpand * 3f).coerceIn(0f, 1f) }
                                    .padding(top = 12.dp)
                                    .size(width = 40.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(TextBackgroundColor.copy(alpha = 0.35f)),
                        )

                        // Pushes art, title and progress bar down; the 16dp is taken back from the
                        // gap above the play controls, so those stay where they were. It also holds
                        // "Playing from <list>" when the song was started from a list.
                        // Sits halfway between the handle and the cover; tapping it opens the list.
                        val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
                        val sourceRoute by playerConnection.queueSourceRoute.collectAsStateWithLifecycle()
                        val playingFrom = queueTitle?.takeIf { showPlayingFrom && it.isNotBlank() && sourceRoute != null }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .height(28.dp)
                                    .padding(horizontal = PlayerHorizontalPadding)
                                    .graphicsLayer { alpha = (1f - cardExpand * 3f).coerceIn(0f, 1f) },
                        ) {
                            if (playingFrom != null) {
                                Text(
                                    modifier =
                                        Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                sourceRoute?.let { route ->
                                                    state.collapseSoft()
                                                    navController.navigate(route)
                                                }
                                            }.padding(horizontal = 10.dp, vertical = 2.dp),
                                    text = stringResource(R.string.playing_from) + " · " + playingFrom,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextBackgroundColor.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        run {
                            // Remember lambdas to prevent unnecessary recomposition
                            val currentSliderPosition by rememberUpdatedState(sliderPosition)
                            val sliderPositionProvider = remember { { currentSliderPosition } }
                            val isExpandedProvider = remember(state) { { state.isExpanded } }
                            // Art and title stay visible while the card is dragged open: they shrink
                            // and move into the full-screen lyrics header, drawn above the card.
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.zIndex(2f),
                            ) {
                                Thumbnail(
                                    sliderPositionProvider = sliderPositionProvider,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(artSize)
                                            .graphicsLayer {
                                                // With edge-to-edge art the big art itself moves
                                                // into the lyrics / up next header instead.
                                                if (edgeToEdgeArt) alpha = 0f
                                            }
                                            .onGloballyPositioned {
                                                if (cardExpand == 0f) {
                                                    artBoxPos = it.positionInRoot()
                                                    artBoxSize = it.size
                                                }
                                            }.graphicsLayer {
                                                val p = cardExpand
                                                if (p == 0f || artBoxSize.height == 0) return@graphicsLayer
                                                val art = artBoxSize.height.toFloat()
                                                val artLocalLeft = (artBoxSize.width - art) / 2f
                                                val scale = 1f + (headerArtPx / art - 1f) * p
                                                transformOrigin = TransformOrigin(0f, 0f)
                                                scaleX = scale
                                                scaleY = scale
                                                val left = lerp(artBoxPos.x + artLocalLeft, marginPx, p)
                                                translationX = left - artBoxPos.x - artLocalLeft * scale
                                                translationY = lerp(artBoxPos.y, headerTopPx, p) - artBoxPos.y
                                            }.nestedScroll(state.preUpPostDownNestedScrollConnection),
                                    isPlayerExpanded = isExpandedProvider,
                                    isListenTogetherGuest = isListenTogetherGuest,
                                    carousel = thumbnailCarousel,
                                    compact = true,
                                    isPlaying = effectiveIsPlaying,
                                    // The art is scaled down into the lyrics / up next header, so its
                                    // corners are widened to end up as round as the mini player's.
                                    cornerRadiusProvider = {
                                        val p = cardExpand
                                        val art = artBoxSize.height.toFloat()
                                        if (p == 0f || art == 0f) {
                                            20.dp
                                        } else {
                                            val scale = 1f + (headerArtPx / art - 1f) * p
                                            (20f + (10f / scale - 20f) * p).dp
                                        }
                                    },
                                )
                                // Same gap as between the title row and the progress bar.
                                Spacer(Modifier.height(24.dp))
                                Column(
                                    modifier =
                                        Modifier
                                            .onGloballyPositioned {
                                                if (cardExpand == 0f) titleRowPos = it.positionInRoot()
                                            }.graphicsLayer {
                                                val p = cardExpand
                                                if (p == 0f) return@graphicsLayer
                                                // titleLarge (22 sp) -> titleMedium (16 sp) in the header
                                                val scale = 1f + (TitleHeaderScale - 1f) * p
                                                transformOrigin = TransformOrigin(0f, 0f)
                                                scaleX = scale
                                                scaleY = scale
                                                val textLeft = lerp(titleRowPos.x + marginPx, headerTitleLeftPx, p)
                                                translationX = textLeft - titleRowPos.x - marginPx * scale
                                                translationY = lerp(titleRowPos.y, headerTitleTopPx, p) - titleRowPos.y
                                            },
                                ) {
                                    mediaMetadata?.let { titleRowContent(it) }
                                }
                            }
                        }

                        // Progress bar and buttons glide down to where the full-screen lyrics show
                        // them, drawn above the card that grows underneath.
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .zIndex(1f)
                                    .onGloballyPositioned {
                                        if (cardExpand == 0f) {
                                            playbackTopPx = it.positionInRoot().y
                                            playbackHeightPx = it.size.height.toFloat()
                                        }
                                    }.graphicsLayer {
                                        translationY = cardExpand * playbackTravelPx()
                                        alpha = cardControlsAlpha
                                        // They slide down a little as they fade out (and back up).
                                        translationY += (1f - cardControlsAlpha) * 16.dp.toPx()
                                        // Hidden controls are moved away so they don't catch touches.
                                        if (cardControlsAlpha == 0f) translationY += OffscreenOffsetPx
                                    },
                        ) {
                            mediaMetadata?.let { playbackControlsContent(it) }
                        }

                        Spacer(
                            Modifier
                                .height(16.dp)
                                .onGloballyPositioned {
                                    // Where the lyrics / up next card starts (it follows this gap).
                                    if (cardExpand == 0f) cardTopPx = it.positionInRoot().y + it.size.height
                                },
                        )

                        PlayerBottomCard(
                            mediaMetadata = mediaMetadata,
                            positionProvider = { effectivePosition },
                            isPlayerExpanded = state.isExpanded,
                            isPlaying = effectiveIsPlaying,
                            contentColor = TextBackgroundColor,
                            isListenTogetherGuest = isListenTogetherGuest,
                            sleepTimerEnabled = sleepTimerEnabled,
                            onSleepTimerClick = {
                                if (sleepTimerEnabled) {
                                    playerConnection.service.sleepTimer?.clear()
                                } else {
                                    showSleepTimerDialog = true
                                }
                            },
                            onLyricsMenuClick = lyricsMenu@{
                                val metadata = mediaMetadata ?: return@lyricsMenu
                                menuState.show {
                                    com.metrolist.music.ui.menu.LyricsMenu(
                                        lyricsProvider = { playerConnection.currentLyrics.value },
                                        songProvider = { currentSong?.song },
                                        mediaMetadataProvider = { metadata },
                                        onDismiss = menuState::dismiss,
                                        onShowOffsetDialog = {
                                            bottomSheetPageState.show {
                                                ShowOffsetDialog(
                                                    songProvider = { currentSong?.song },
                                                )
                                            }
                                        },
                                    )
                                }
                            },
                            onOpenFullQueue = { openCardFull(UpNextPage) },
                            onOpenFullLyrics = { openCardFull(LyricsPage) },
                            onPageChanged = { cardPage = it },
                            expandFraction = { cardExpand },
                            openBottomPadding = with(density) { (playbackHeightPx + navBottomPx).toDp() } + 24.dp,
                            openBottomCoverPx = {
                                val controlsPx = playbackHeightPx + navBottomPx + with(density) { 12.dp.toPx() }
                                cardExpand * lerp(navBottomPx, controlsPx, cardControlsAlpha)
                            },
                            onPullDownClose = closeCardFull,
                            headerAlpha = { cardControlsAlpha },
                            followState = lyricsFollow,
                            onQueueSongMoreClick = { window ->
                                val metadata = window.mediaItem.metadata
                                if (metadata != null) {
                                    menuState.show {
                                        com.metrolist.music.ui.menu.QueueMenu(
                                            mediaMetadata = metadata,
                                            playerBottomSheetState = state,
                                            onShowDetailsDialog = {
                                                bottomSheetPageState.show {
                                                    ShowMediaInfo(metadata.id)
                                                }
                                            },
                                            onDismiss = menuState::dismiss,
                                            onRemoveFromQueue = {
                                                playerConnection.player.removeMediaItem(window.firstPeriodIndex)
                                            },
                                        )
                                    }
                                }
                            },
                            onMoreClick = moreMenu@{
                                val metadata = mediaMetadata ?: return@moreMenu
                                menuState.show {
                                    PlayerMenu(
                                        mediaMetadata = metadata,
                                        playerBottomSheetState = state,
                                        onShowDetailsDialog = {
                                            bottomSheetPageState.show {
                                                ShowMediaInfo(metadata.id)
                                            }
                                        },
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                                    .onGloballyPositioned {
                                        if (cardExpand == 0f) cardTravelPx = it.positionInRoot().y.coerceAtLeast(1f)
                                    }
                                    // Dragging up opens the card. Detected here, outside the growing card,
                                    // so the finger's movement is measured against a fixed frame.
                                    .pointerInput(Unit) {
                                        val tracker = VelocityTracker()
                                        detectVerticalDragGestures(
                                            onDragStart = { tracker.resetTracking() },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                tracker.addPosition(change.uptimeMillis, change.position)
                                                onCardDrag(cardPage, dragAmount)
                                            },
                                            onDragEnd = { onCardDragEnd(cardPage, tracker.calculateVelocity().y) },
                                            onDragCancel = { onCardDragEnd(cardPage, 0f) },
                                        )
                                    }
                                    // While opening, the card grows up to the top of the screen and out
                                    // to the side edges, staying anchored at the bottom.
                                    .layout { measurable, constraints ->
                                        val fraction = cardExpand
                                        val extraTop = (cardTravelPx * fraction).roundToInt()
                                        val extraSide = (12.dp.toPx() * fraction).roundToInt()
                                        val width = constraints.maxWidth + extraSide * 2
                                        val height = constraints.maxHeight + extraTop
                                        val placeable = measurable.measure(Constraints.fixed(width, height))
                                        layout(constraints.maxWidth, constraints.maxHeight) {
                                            placeable.place(-extraSide, -extraTop)
                                        }
                                    },
                        )
                    }

                    BackHandler(enabled = cardOpen && state.isExpanded) {
                        closeCardFull()
                    }
                    // Header of the opened card: dragging it down closes the card (following the
                    // finger), and the chevron closes it too. The art and title sit underneath.
                    if (cardExpand > 0f) {
                        Box(
                            modifier =
                                Modifier
                                    .zIndex(3f)
                                    .fillMaxWidth()
                                    .height(topInset + 64.dp)
                                    // A tap on the art and title goes back to the big cover too.
                                    .pointerInput(cardOpen) {
                                        detectTapGestures { if (cardOpen) closeCardFull() }
                                    }
                                    .pointerInput(Unit) {
                                        val tracker = VelocityTracker()
                                        detectVerticalDragGestures(
                                            onDragStart = { tracker.resetTracking() },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                tracker.addPosition(change.uptimeMillis, change.position)
                                                onCardDrag(cardPage, dragAmount)
                                            },
                                            onDragEnd = { onCardDragEnd(cardPage, tracker.calculateVelocity().y) },
                                            onDragCancel = { onCardDragEnd(cardPage, 0f) },
                                        )
                                    },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = topInset + 14.dp, end = PlayerHorizontalPadding - 4.dp)
                                        .graphicsLayer { alpha = ((cardExpand - 0.5f) * 2f).coerceIn(0f, 1f) }
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TextBackgroundColor.copy(alpha = 0.12f))
                                        .clickable(enabled = cardOpen) { closeCardFull() },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.expand_more),
                                    contentDescription = stringResource(R.string.close),
                                    tint = TextBackgroundColor,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                    // Re-sync: above the play buttons, and down at the bottom while they are hidden.
                    // It stays until it is tapped.
                    androidx.compose.animation.AnimatedVisibility(
                        // Not shown any more: the lyrics find their place again by themselves
                        // three seconds after the last touch.
                        visible = false && cardOpen && cardPage == LyricsPage && lyricsFollow.paused,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f),
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .zIndex(4f)
                                .graphicsLayer {
                                    val gap = 16.dp.toPx()
                                    val low = navBottomPx + gap
                                    val high = navBottomPx + 12.dp.toPx() + playbackHeightPx + gap
                                    translationY = -lerp(low, high, cardControlsAlpha)
                                },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(TextBackgroundColor.copy(alpha = 0.2f))
                                    // Reacts on touch down: the touch also brings the controls back,
                                    // which moves the button up under the finger.
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            awaitFirstDown()
                                            lyricsFollow.paused = false
                                        }
                                    }.padding(horizontal = 16.dp, vertical = 9.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = null,
                                tint = TextBackgroundColor,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.auto_scroll),
                                style = MaterialTheme.typography.labelLarge,
                                color = TextBackgroundColor,
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isFullScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            val queueContent: @Composable () -> Unit = {
                Queue(
                    state = queueSheetState,
                    playerBottomSheetState = state,
                    background =
                        if (useCardLayout) {
                            cardAlbumColor
                        } else if (useBlackBackground) {
                            Color.Black
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    panelColor = if (useCardLayout) lerp(cardAlbumColor, Color.Black, 0.15f) else null,
                    onBackgroundColor = onBackgroundColor,
                    TextBackgroundColor = TextBackgroundColor,
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    pureBlack = pureBlack,
                    showInlineLyrics = showInlineLyrics,
                    playerBackground = playerBackground,
                    onToggleLyrics = {
                        showInlineLyrics = !showInlineLyrics
                    },
                )
            }
            // Portrait uses its own up-next page in the card; the old queue sheet stays only
            // for the landscape layout.
            if (!useCardLayout) {
                queueContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLyricsView(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    positionProvider: () -> Long,
    // When set, renders the small card version of the lyrics in this color.
    compactColor: Color? = null,
    isPlaying: Boolean = true,
    // Card layout: how far the card is opened toward full screen (0..1).
    expandFraction: () -> Float = { 0f },
    openBottomPadding: Dp = 0.dp,
    followState: LyricsFollowState? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle(initialValue = -1)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    var appInForeground by remember {
        mutableStateOf(
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
        )
    }
    DisposableEffect(Unit) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer =
            LifecycleEventObserver { _, _ ->
                appInForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val nextMetadata =
        remember(queueWindows, currentWindowIndex) {
            if (currentWindowIndex >= 0 && currentWindowIndex + 1 < queueWindows.size) {
                queueWindows[currentWindowIndex + 1].mediaItem.metadata
            } else {
                null
            }
        }

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint =
                        EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.metrolist.music.di.LyricsHelperEntryPoint::class.java,
                        )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                    database.query {
                        upsert(LyricsEntity(mediaMetadata.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider))
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    // Prefetch lyrics for the next queue item only while the lyrics pane is visible, the app is in the
    // foreground, and the current track's lyrics row has finished loading (avoids competing with the
    // active fetch).
    LaunchedEffect(
        nextMetadata?.id,
        showLyrics,
        appInForeground,
        mediaMetadata?.id,
        currentLyrics,
    ) {
        if (!showLyrics || !appInForeground || nextMetadata == null) return@LaunchedEffect
        val loadedForCurrent =
            currentLyrics?.let { lyrics ->
                mediaMetadata == null || lyrics.id == mediaMetadata.id
            } == true
        if (mediaMetadata != null && !loadedForCurrent) return@LaunchedEffect
        val nextId = nextMetadata.id
        delay(400)
        if (!showLyrics || !appInForeground || !isActive) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val existing = database.lyrics(nextId).first()
                if (existing != null) return@withContext
                val entryPoint =
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.metrolist.music.di.LyricsHelperEntryPoint::class.java,
                    )
                val lyricsHelper = entryPoint.lyricsHelper()
                val fetched = lyricsHelper.getLyrics(nextMetadata)
                database.query {
                    upsert(LyricsEntity(nextId, fetched.lyrics, fetched.provider))
                }
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            lyrics == null -> {
                DraggableLyricsState { ContainedLoadingIndicator() }
            }

            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                DraggableLyricsState {
                    Text(
                        text = stringResource(R.string.lyrics_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = (compactColor ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            compactColor != null -> {
                CompactLyrics(
                    lyrics = lyrics,
                    positionProvider = positionProvider,
                    color = compactColor,
                    isPlaying = isPlaying,
                    expandFraction = expandFraction,
                    openBottomPadding = openBottomPadding,
                    followState = followState,
                )
            }

            else -> {
                val lyricsContent: @Composable () -> Unit = {
                    Lyrics(
                        sliderPositionProvider = positionProvider,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        showLyrics = showLyrics,
                    )
                }
                ProvideTextStyle(
                    value =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        ),
                ) {
                    lyricsContent()
                }
            }
        }
    }
}

@Composable
private fun PlayerMoreMenuButton(
    mediaMetadata: MediaMetadata,
    state: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color,
) {
    val navController = LocalNavController.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(textButtonColor)
                .clickable {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = {
                                mediaMetadata.id.let {
                                    bottomSheetPageState.show {
                                        ShowMediaInfo(it)
                                    }
                                }
                            },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
    ) {
        Image(
            painter = painterResource(R.drawable.more_horiz),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor),
        )
    }
}

/**
 * Heart in the card layout: outlined, or filled while the song is liked. Liking it by tap makes
 * the heart grow a little (it stays inside the circle), wobble left and right once and settle.
 */
@Composable
private fun PlayerLikeButton(
    liked: Boolean,
    contentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    // Only a tap animates; a liked song that merely comes on screen shows the filled heart as is.
    var tapped by remember { mutableStateOf(false) }
    val unlikeGuard = rememberUnlikeGuard(liked)
    val crackShake = rememberCrackShake(unlikeGuard)
    LaunchedEffect(liked) {
        if (!liked && tapped) {
            // The like is removed: the heart pulls back in and settles.
            scale.animateTo(0.7f, tween(120, easing = FastOutSlowInEasing))
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
        }
        if (liked && tapped) {
            launch {
                scale.animateTo(1.2f, tween(160, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 300f))
            }
            rotation.animateTo(
                0f,
                keyframes {
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
    PlayerCircleButton(
        icon = unlikeGuard.icon(liked),
        contentDescription = stringResource(R.string.action_like),
        contentColor = if (liked) com.metrolist.music.ui.theme.likedTint(contentColor) else contentColor,
        onClick = {
            if (unlikeGuard.onTap(liked)) {
                tapped = true
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            } else {
                // First tap on a liked song: the heart cracks and shakes, the like stays.
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            }
        },
        onLongClick = onLongClick,
        modifier = modifier,
        iconModifier =
            Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = rotation.value + crackShake.value
            },
    )
}

@Composable
private fun PlayerCircleButton(
    icon: Int,
    contentDescription: String?,
    contentColor: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .background(contentColor.copy(alpha = 0.15f))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick =
                        onLongClick?.let { longClick ->
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                longClick()
                            }
                        },
                ),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = iconModifier.size(24.dp),
        )
    }
}

/**
 * Previous / play-pause / next in the card layout. No ripple circle: the icon shrinks a little
 * while pressed and the phone gives a short vibration on each tap.
 */
@Composable
private fun PlayerControlButton(
    icon: Int,
    contentDescription: String?,
    enabled: Boolean,
    color: Color,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "controlPressScale",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onClick()
                },
            ),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) color else color.copy(alpha = 0.38f),
            modifier =
                Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
        )
    }
}

/**
 * The album art drawn edge to edge across the top of the player. Its lower edge follows the
 * setting: Blackout fades it into the background, Blur blurs it from the title down (stronger
 * toward the bottom), Nothing leaves it sharp. While paused it shrinks slightly and its corners
 * round off. While the card opens ([expandProvider] 0..1) it shrinks straight into the header's
 * small art. With the song change animation on it slides along with the art carousel, so the
 * next song's art shows while swiping.
 */
@Composable
private fun EdgeToEdgeAlbumArt(
    artworkUrl: String?,
    carousel: ThumbnailCarousel,
    slide: Boolean,
    isPlaying: Boolean,
    edge: com.metrolist.music.constants.MiniPlayerEdgeEffect,
    heightPx: Float,
    tailPx: Float,
    blurStartPx: Float,
    expandProvider: () -> Float,
    headerLeftPx: Float,
    headerTopPx: Float,
    headerSizePx: Float,
    modifier: Modifier = Modifier,
) {
    val pauseScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else EdgeToEdgePausedScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "edgeArtScale",
    )
    val blackout = edge == com.metrolist.music.constants.MiniPlayerEdgeEffect.BLACKOUT
    val blur =
        edge == com.metrolist.music.constants.MiniPlayerEdgeEffect.BLUR &&
            !com.metrolist.music.utils.LocalBatterySaver.current.blur
    Box(
        modifier =
            modifier
                .layout { measurable, constraints ->
                    // Full size at rest; while the card opens, the art's own frame shrinks to the
                    // header art (the image is cropped anew each frame, so it never stretches).
                    val p = expandProvider().coerceIn(0f, 1f)
                    val fullWidth = constraints.maxWidth.toFloat()
                    val fullHeight = heightPx.coerceIn(1f, constraints.maxHeight.toFloat())
                    val width = lerp(fullWidth, headerSizePx, p).roundToInt().coerceAtLeast(1)
                    val height = lerp(fullHeight, headerSizePx, p).roundToInt().coerceAtLeast(1)
                    val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(width, height))
                    layout(constraints.maxWidth, fullHeight.roundToInt()) {
                        placeable.place(lerp(0f, headerLeftPx, p).roundToInt(), lerp(0f, headerTopPx, p).roundToInt())
                    }
                }.graphicsLayer {
                    val p = expandProvider().coerceIn(0f, 1f)
                    val scale = lerp(pauseScale, 1f, p)
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 0.4f)
                    // Corners round off as it shrinks while paused, and end as round as the
                    // header art's when the card is open.
                    val shrink = ((1f - pauseScale) / (1f - EdgeToEdgePausedScale)).coerceIn(0f, 1f)
                    val radius = lerp(28f * shrink, 10f, p)
                    shape = RoundedCornerShape(radius.dp)
                    clip = true
                    compositingStrategy = CompositingStrategy.Offscreen
                }.drawWithContent {
                    drawContent()
                    val p = expandProvider().coerceIn(0f, 1f)
                    if (p >= 1f) return@drawWithContent
                    fun a(alpha: Float) = Color.Black.copy(alpha = lerp(alpha, 1f, p))
                    drawRect(
                        brush =
                            if (blackout) {
                                // Blackout: fades out over the lower part, into the background.
                                Brush.verticalGradient(
                                    0f to Color.Black,
                                    0.55f to Color.Black,
                                    0.8f to a(0.45f),
                                    1f to a(0f),
                                )
                            } else {
                                // Nothing and Blur: no darkening, only a short fade at the very
                                // end, just above the lyrics / up next card.
                                if (tailPx <= 0f) return@drawWithContent
                                val tailStart = (1f - tailPx / size.height).coerceIn(0f, 1f)
                                Brush.verticalGradient(
                                    0f to Color.Black,
                                    tailStart to Color.Black,
                                    1f to a(0f),
                                )
                            },
                        blendMode = BlendMode.DstIn,
                    )
                },
    ) {
        val gridState = carousel.gridState
        if (slide && gridState != null) {
            // The current art and the one it is sliding towards, moved with the carousel.
            val base by remember(carousel) {
                derivedStateOf { kotlin.math.floor(carousel.position()).toInt() }
            }
            for (index in listOf(base, base + 1)) {
                val url = carousel.artworks.getOrNull(index) ?: continue
                // Keyed by song, not position: when the queue around the new song is rebuilt the
                // positions shift, and the art already on screen must stay instead of reloading.
                androidx.compose.runtime.key(carousel.keys.getOrNull(index) ?: index) {
                    EdgeArtImage(
                        url = url,
                        blur = blur,
                        blurStartPx = blurStartPx,
                        expandProvider = expandProvider,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer { translationX = (index - carousel.position()) * size.width },
                    )
                }
            }
        } else {
            androidx.compose.animation.Crossfade(
                targetState = artworkUrl,
                animationSpec = tween(250),
                label = "edgeArt",
            ) { url ->
                EdgeArtImage(
                    url = url,
                    blur = blur,
                    blurStartPx = blurStartPx,
                    expandProvider = expandProvider,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * One edge-to-edge art. With [blur], two blurred copies are laid over it, faded in from
 * [blurStartPx] down, the stronger one lower, so the blur grows toward the bottom. The images
 * reach a little below the visible frame, so the blur stays full right to the bottom edge.
 */
@Composable
private fun EdgeArtImage(
    url: String?,
    blur: Boolean,
    blurStartPx: Float,
    expandProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val overhangPx = if (blur) with(LocalDensity.current) { EdgeBlurOverhang.roundToPx() } else 0
    Box(
        modifier =
            modifier
                .clipToBounds()
                .layout { measurable, constraints ->
                    val placeable =
                        measurable.measure(
                            androidx.compose.ui.unit.Constraints.fixed(constraints.maxWidth, constraints.maxHeight + overhangPx),
                        )
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
                },
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
        if (blur) {
            for ((radius, range) in listOf(EdgeBlurLight to (0f to 0.5f), EdgeBlurStrong to (0.3f to 1f))) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = 1f - expandProvider().coerceIn(0f, 1f)
                                compositingStrategy = CompositingStrategy.Offscreen
                            }.drawWithContent {
                                drawContent()
                                val visible = size.height - overhangPx
                                val span = (visible - blurStartPx).coerceAtLeast(1f)
                                drawRect(
                                    brush =
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black),
                                            startY = blurStartPx + span * range.first,
                                            endY = blurStartPx + span * range.second,
                                        ),
                                    blendMode = BlendMode.DstIn,
                                )
                            }.blur(radius, androidx.compose.ui.draw.BlurredEdgeTreatment.Rectangle),
                )
            }
        }
    }
}

private val EdgeToEdgeTail = 40.dp
private val EdgeBlurOverhang = 64.dp
private val EdgeBlurLight = 14.dp
private val EdgeBlurStrong = 34.dp

private const val EdgeToEdgePausedScale = 0.93f

internal val DefaultAlbumBackground = Color(0xFF2B2A30)

/**
 * Glass player background: the album cover, centered, enlarged and blurred until only its colors
 * are left, darkened a little so white text stays readable. It sits behind the whole player, so
 * the lyrics and up-next views share it.
 */
@Composable
private fun GlassCoverBackground(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Modifier.blur needs Android 12; older versions get a tiny image stretched over the screen,
    // which smears the cover into soft color areas as well.
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    AnimatedContent(
        targetState = thumbnailUrl,
        transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
        label = "glassBackground",
        modifier = modifier.fillMaxSize(),
    ) { url ->
        if (url != null) {
            Box(Modifier.fillMaxSize()) {
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
                            }.blur(90.dp),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.28f),
                                0.55f to Color.Black.copy(alpha = 0.36f),
                                1f to Color.Black.copy(alpha = 0.5f),
                            ),
                        ),
                )
            }
        } else {
            Box(Modifier.fillMaxSize())
        }
    }
}

/** Softens an album art color into a calm, mid-dark tone that white text reads well on. */
internal fun albumBackgroundBrush(color: Color): Brush =
    Brush.verticalGradient(
        listOf(
            lerp(color, Color.White, 0.06f),
            color,
            lerp(color, Color.Black, 0.18f),
        ),
    )

/**
 * Full-screen lyrics opened from the lyrics card. Closes with the back button, the close
 * button, or by pulling down again once the lyrics are scrolled to the top.
 * The player controls show at the bottom when it opens and after any touch, and hide again
 * after [ControlsAutoHideMillis] so the lyrics can use the whole screen.
 */
@Composable
private fun FullScreenLyrics(
    mediaMetadata: MediaMetadata?,
    backgroundColor: Color,
    positionProvider: () -> Long,
    onClose: () -> Unit,
    controls: @Composable ColumnScope.() -> Unit,
    // False while parked off screen: the lyrics keep tracking the song at a low rate.
    visible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle()
    var controlsVisible by remember { mutableStateOf(true) }
    var lastTouch by remember { mutableLongStateOf(0L) }
    var showSourceDialog by remember { mutableStateOf(false) }

    // Each time it opens, the controls show again for their usual time.
    LaunchedEffect(visible) {
        if (visible) {
            controlsVisible = true
            lastTouch = System.currentTimeMillis()
        }
    }

    LaunchedEffect(controlsVisible, lastTouch, visible) {
        if (controlsVisible && visible) {
            delay(ControlsAutoHideMillis)
            controlsVisible = false
        }
    }

    if (showSourceDialog && mediaMetadata != null) {
        com.metrolist.music.ui.menu.LyricsSourceDialog(
            mediaMetadata = mediaMetadata,
            currentProvider = currentLyrics?.provider,
            onDismiss = { showSourceDialog = false },
        )
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(albumBackgroundBrush(backgroundColor))
                // Any touch (tap or scroll) brings the controls back and restarts their timer.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            controlsVisible = true
                            lastTouch = System.currentTimeMillis()
                        }
                    }
                }
                // Keeps taps from reaching the player underneath.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        // Art and title line up with the lyrics and the song title in the controls;
                        // the close icon's right edge sits on the same margin.
                        .padding(start = PlayerHorizontalPadding, end = PlayerHorizontalPadding - 12.dp, bottom = 4.dp),
            ) {
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata?.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }
            Lyrics(
                sliderPositionProvider = positionProvider,
                showLyrics = visible,
                liveUpdates = visible,
                accentOverride = Color.White,
                onPullDownAtTop = onClose,
                onProviderClick = { showSourceDialog = true },
                horizontalPadding = PlayerHorizontalPadding,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            )
            Spacer(Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()))
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val bottomColor = lerp(backgroundColor, Color.Black, 0.18f)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        // Solid panel so the lyrics behind it are hidden while the controls show.
                        .background(
                            Brush.verticalGradient(
                                0f to bottomColor.copy(alpha = 0f),
                                0.12f to bottomColor,
                                1f to bottomColor,
                            ),
                        ).padding(top = 32.dp, bottom = 12.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                controls()
            }
        }
    }
}

private const val ControlsAutoHideMillis = 3000L
private const val CardExpandMillis = 380
private const val TitleHeaderScale = 16f / 22f
// Moves the hidden full-screen lyrics well below the screen so they take no touches.
private const val OffscreenOffsetPx = 100_000
private const val FullViewFadeMillis = 200
// px/s; a faster flick opens (or keeps closed) the card regardless of how far it was dragged.
private const val CardFlingVelocity = 1000f

/**
 * Loading or "no lyrics" in place of the lyrics: still a (non-moving) scrollable area, so dragging
 * it down behaves like dragging real lyrics. The first pull closes the lyrics back to the art, and
 * only a second one drags the player down to the mini player.
 */
@Composable
private fun DraggableLyricsState(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val boxHeight = maxHeight
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(boxHeight),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}

private const val StallIndicatorDelayMs = 800L

/**
 * The "still loading" band: a soft bright strip that keeps sliding from the current position to
 * the end of the bar, over and over, while playback is stuck waiting for data.
 */
@Composable
private fun BufferingSweep(
    visible: Boolean,
    fraction: () -> Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(250), label = "stallAlpha")
    if (alpha == 0f) return
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "stall")
    val travel by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            androidx.compose.animation.core.infiniteRepeatable(
                tween(1100, easing = FastOutSlowInEasing),
            ),
        label = "stallTravel",
    )
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val startX = size.width * fraction()
        val span = size.width - startX
        if (span <= 1f) return@Canvas
        val band = (size.width * 0.28f).coerceAtMost(span)
        // From just behind the start to just past the end, so it enters and leaves smoothly.
        val center = startX - band / 2f + (span + band) * travel
        val h = 4.dp.toPx()
        val top = (size.height - h) / 2f
        clipRect(left = startX, right = size.width) {
            drawRoundRect(
                brush =
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to color.copy(alpha = 0.85f * alpha),
                        1f to Color.Transparent,
                        startX = center - band / 2f,
                        endX = center + band / 2f,
                    ),
                topLeft = Offset(center - band / 2f, top),
                size = Size(band, h),
                cornerRadius = CornerRadius(h / 2f),
            )
        }
    }
}

/** A rounded box button in the sleep timer: Set as default, or End of song. */
@Composable
private fun SleepTimerChoiceBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(onSurface.copy(alpha = if (enabled) 0.1f else 0.05f))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides onSurface.copy(alpha = if (enabled) 1f else 0.5f)) {
            content()
        }
    }
}
