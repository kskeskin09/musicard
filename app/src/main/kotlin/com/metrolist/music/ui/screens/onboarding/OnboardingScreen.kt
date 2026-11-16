/**
 * Musicard Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.EdgeToEdgeAlbumArtKey
import com.metrolist.music.constants.LyricStyle
import com.metrolist.music.constants.LyricStyleKey
import com.metrolist.music.constants.LyricsBlurKey
import com.metrolist.music.constants.LyricsTextPositionKey
import com.metrolist.music.constants.MiniPlayerBackgroundStyle
import com.metrolist.music.constants.MiniPlayerBackgroundStyleKey
import com.metrolist.music.constants.PageBackgroundKey
import com.metrolist.music.constants.PageEdgeToEdgeCoverKey
import com.metrolist.music.constants.PlayerCardBackground
import com.metrolist.music.constants.PlayerCardBackgroundKey
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.lyrics.LyricsEntry
import com.metrolist.music.lyrics.LyricsUtils
import com.metrolist.music.ui.component.PageGlassBackground
import com.metrolist.music.ui.component.StyledSlider
import com.metrolist.music.ui.player.CompactLyrics
import com.metrolist.music.ui.player.DefaultAlbumBackground
import com.metrolist.music.ui.player.GlassCoverBackground
import com.metrolist.music.ui.player.albumBackgroundBrush
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.screens.settings.LyricsPosition
import com.metrolist.music.ui.screens.settings.ModeCircle
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.ui.theme.albumBackgroundColor
import com.metrolist.music.utils.LocalBatterySaver
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.OnboardingAlbum
import com.metrolist.music.viewmodels.OnboardingArtist
import com.metrolist.music.viewmodels.OnboardingSong
import com.metrolist.music.viewmodels.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The first-run setup, one step at a time. The order is the order Back and Next walk through. */
enum class OnboardingStep {
    WELCOME,
    THEME,
    PLAYER_BACKGROUND,
    SLIDER,
    EDGE_TO_EDGE,
    LYRICS,
    PAGE,
    ARTISTS,
}

private enum class Scene { WELCOME, PLAYER, PAGE, ARTISTS }

private val OnboardingStep.scene: Scene
    get() =
        when (this) {
            OnboardingStep.WELCOME -> Scene.WELCOME
            OnboardingStep.PAGE -> Scene.PAGE
            OnboardingStep.ARTISTS -> Scene.ARTISTS
            else -> Scene.PLAYER
        }

// Steps that are just switches: leaving the switch as it is counts as a choice, so Next shows
// from the start.
private val SwitchSteps = setOf(OnboardingStep.EDGE_TO_EDGE, OnboardingStep.PAGE)

private val MiniPlayerHeight = 64.dp

/**
 * Shown over the app on the first launch: a welcome page, then the look of the player, lyrics
 * and album pages over a song that seems to play (it makes no sound), then favorite artists.
 * Every choice is saved to the same settings as in Settings, so they can be changed there later.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }
    val chosen = remember { mutableStateMapOf<OnboardingStep, Boolean>() }
    val markChosen: () -> Unit = { chosen[step] = true }

    val finish: (Boolean) -> Unit = { save ->
        viewModel.finish(saveArtists = save)
        onFinished()
    }
    val goBack: () -> Unit = {
        if (step != OnboardingStep.WELCOME) step = OnboardingStep.entries[step.ordinal - 1]
    }
    val goNext: () -> Unit = {
        if (step == OnboardingStep.ARTISTS) finish(true) else step = OnboardingStep.entries[step.ordinal + 1]
    }
    BackHandler(enabled = step != OnboardingStep.WELCOME, onBack = goBack)

    LaunchedEffect(step) {
        if (step >= OnboardingStep.PAGE) viewModel.loadSuggestions()
    }

    val song by viewModel.song.collectAsStateWithLifecycle()
    val lyricsText by viewModel.lyrics.collectAsStateWithLifecycle()
    val album by viewModel.album.collectAsStateWithLifecycle()

    // The pretend playback clock. During the lyrics step it keeps repeating a stretch where the
    // words run without a break, so the lyric styles can be seen moving.
    val lines =
        remember(lyricsText) {
            lyricsText
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { LyricsUtils.parseLyrics(it) }.getOrNull() }
                ?.filter { it.text.isNotBlank() }
                .orEmpty()
        }
    val loop = remember(lines) { findLyricsLoop(lines) }
    var positionMs by remember { mutableLongStateOf(0L) }
    val currentStep by rememberUpdatedState(step)
    val duration by rememberUpdatedState(song.durationMs)
    val currentLoop by rememberUpdatedState(loop)
    LaunchedEffect(Unit) {
        var last = withFrameMillis { it }
        while (true) {
            withFrameMillis { now ->
                val dt = (now - last).coerceIn(0L, 100L)
                last = now
                var next = positionMs + dt
                val window = currentLoop
                if (currentStep == OnboardingStep.LYRICS && window != null) {
                    if (next < window.first || next > window.last) next = window.first
                } else if (next >= duration) {
                    next = 0L
                }
                positionMs = next
            }
        }
    }

    // The soft album color the player and pages are painted with, taken from the cover.
    val context = LocalContext.current
    var coverBase by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(song.thumbnailUrl) {
        val url = song.thumbnailUrl ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(url)
                    .size(100, 100)
                    .allowHardware(false)
                    .build()
            val bitmap = runCatching { context.imageLoader.execute(request).image?.toBitmap() }.getOrNull()
            if (bitmap != null) {
                val palette =
                    Palette
                        .from(bitmap)
                        .maximumColorCount(8)
                        .resizeBitmapArea(100 * 100)
                        .generate()
                coverBase =
                    PlayerColorExtractor
                        .extractGradientColors(palette, DefaultAlbumBackground.toArgb())
                        .firstOrNull()
            }
        }
    }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val albumColor by animateColorAsState(
        targetValue = coverBase?.let { albumBackgroundColor(it, dark) } ?: DefaultAlbumBackground,
        animationSpec = tween(800),
        label = "onboardingAlbumColor",
    )

    // The settings, the same ones Settings changes.
    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(PlayerCardBackgroundKey, PlayerCardBackground.CLASSIC)
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(SliderStyleKey, SliderStyle.SLIM)
    val (edgeArt, onEdgeArtChange) = rememberPreference(EdgeToEdgeAlbumArtKey, false)
    val (lyricStyle, onLyricStyleChange) = rememberEnumPreference(LyricStyleKey, LyricStyle.CLASSIC)
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val (lyricsBlur, onLyricsBlurChange) = rememberPreference(LyricsBlurKey, false)
    val (pageEdge, onPageEdgeChange) = rememberPreference(PageEdgeToEdgeCoverKey, false)
    val (pageBackground, onPageBackgroundChange) =
        rememberEnumPreference(PageBackgroundKey, PlayerCardBackground.CLASSIC)
    val (miniBackground, onMiniBackgroundChange) =
        rememberEnumPreference(MiniPlayerBackgroundStyleKey, MiniPlayerBackgroundStyle.DEFAULT)

    val blurOff = LocalBatterySaver.current.blur
    val playerGlass = playerBackground == PlayerCardBackground.GLASS && !blurOff
    val pageGlass = pageBackground == PlayerCardBackground.GLASS && !blurOff

    val edgeFraction by animateFloatAsState(
        targetValue = if (edgeArt) 1f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "onboardingEdge",
    )
    val pageEdgeFraction by animateFloatAsState(
        targetValue = if (pageEdge) 1f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "onboardingPageEdge",
    )
    val lyricsExpand by animateFloatAsState(
        targetValue = if (step == OnboardingStep.LYRICS) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "onboardingLyricsExpand",
    )

    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var cardHeight by remember { mutableStateOf(220.dp) }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                // Nothing under the setup can be touched.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
    ) {
        val geometry = playerGeometry(maxWidth, maxHeight, statusTop, edgeFraction)

        AnimatedContent(
            targetState = step.scene,
            transitionSpec = {
                (fadeIn(tween(450, delayMillis = 100)) + scaleIn(tween(500), initialScale = 0.96f))
                    .togetherWith(fadeOut(tween(300)) + scaleOut(tween(400), targetScale = 1.03f))
            },
            label = "onboardingScene",
            modifier = Modifier.fillMaxSize(),
        ) { scene ->
            when (scene) {
                Scene.WELCOME ->
                    WelcomePage(
                        onStart = { step = OnboardingStep.THEME },
                        onSkip = { finish(false) },
                    )

                Scene.PLAYER ->
                    PlayerPreview(
                        song = song,
                        lyricsText = lyricsText,
                        positionProvider = { positionMs },
                        albumColor = albumColor,
                        glass = playerGlass,
                        geometry = geometry,
                        edgeFraction = edgeFraction,
                        lyricsExpand = lyricsExpand,
                        statusTop = statusTop,
                        lyricsBottomSpace = cardHeight + navBottom + 24.dp,
                    )

                Scene.PAGE ->
                    AlbumPagePreview(
                        song = song,
                        album = album,
                        positionProvider = { positionMs },
                        albumColor = albumColor,
                        pageGlass = pageGlass,
                        playerGlass = playerGlass,
                        edgeFraction = pageEdgeFraction,
                        miniBackground = miniBackground,
                        statusTop = statusTop,
                        navBottom = navBottom,
                    )

                Scene.ARTISTS ->
                    ArtistPicker(
                        viewModel = viewModel,
                        statusTop = statusTop,
                        navBottom = navBottom,
                        onBack = goBack,
                        onFinish = { finish(true) },
                        onSkip = { finish(false) },
                    )
            }
        }

        // The floating settings card. It slides to a spot that doesn't cover what its setting
        // changes: over the cover while the player's look is chosen, over the controls while the
        // cover itself changes, under the lyrics and above the mini player.
        val bottomLimit = maxHeight - navBottom - 12.dp
        val cardTarget =
            when (step) {
                OnboardingStep.THEME, OnboardingStep.PLAYER_BACKGROUND, OnboardingStep.SLIDER ->
                    geometry.coverTop + geometry.coverSize * 0.5f
                OnboardingStep.EDGE_TO_EDGE -> geometry.controlsCenter - cardHeight / 2
                OnboardingStep.PAGE -> bottomLimit - MiniPlayerHeight - 12.dp - cardHeight
                else -> bottomLimit - cardHeight
            }.coerceAtMost(bottomLimit - cardHeight).coerceAtLeast(statusTop + 8.dp)
        val cardTop by animateDpAsState(
            targetValue = cardTarget,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
            label = "onboardingCardTop",
        )
        val cardShown = step.scene == Scene.PLAYER || step.scene == Scene.PAGE
        AnimatedVisibility(
            visible = cardShown,
            enter = fadeIn(tween(350, delayMillis = 150)) + slideInVertically(tween(500)) { it / 3 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(300)) { it / 3 },
            modifier =
                Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(0, cardTop.roundToPx()) }
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .onSizeChanged { size -> cardHeight = with(density) { size.height.toDp() } },
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                SettingsCard(
                    step = step,
                    nextVisible = chosen[step] == true || step in SwitchSteps,
                    onBack = goBack,
                    onNext = goNext,
                    onSkip = { step = OnboardingStep.ARTISTS },
                ) { shown ->
                    when (shown) {
                        OnboardingStep.THEME ->
                            ThemeChoices(darkMode) {
                                onDarkModeChange(it)
                                markChosen()
                            }

                        OnboardingStep.PLAYER_BACKGROUND ->
                            ChoiceRow(
                                options = PlayerCardBackground.entries,
                                selected = playerBackground,
                                label = {
                                    when (it) {
                                        PlayerCardBackground.CLASSIC -> stringResource(R.string.style_classic)
                                        PlayerCardBackground.GLASS -> stringResource(R.string.style_glass)
                                    }
                                },
                                onSelect = {
                                    onPlayerBackgroundChange(it)
                                    markChosen()
                                },
                            )

                        OnboardingStep.SLIDER ->
                            ChoiceRow(
                                options = listOf(SliderStyle.SLIM, SliderStyle.DEFAULT),
                                selected = sliderStyle,
                                label = {
                                    when (it) {
                                        SliderStyle.SLIM -> stringResource(R.string.slider_style_classic)
                                        SliderStyle.DEFAULT -> stringResource(R.string.slider_style_old)
                                    }
                                },
                                onSelect = {
                                    onSliderStyleChange(it)
                                    markChosen()
                                },
                            )

                        OnboardingStep.EDGE_TO_EDGE ->
                            SwitchRow(
                                title = stringResource(R.string.edge_to_edge_album_art_desc),
                                checked = edgeArt,
                                onCheckedChange = {
                                    onEdgeArtChange(it)
                                    markChosen()
                                },
                                dim = true,
                            )

                        OnboardingStep.LYRICS -> {
                            Label(stringResource(R.string.lyric_style))
                            ChoiceRow(
                                options = LyricStyle.entries,
                                selected = lyricStyle,
                                label = {
                                    when (it) {
                                        LyricStyle.CLASSIC -> stringResource(R.string.style_classic)
                                        LyricStyle.MODERN -> stringResource(R.string.style_modern)
                                    }
                                },
                                onSelect = {
                                    onLyricStyleChange(it)
                                    markChosen()
                                },
                            )
                            Spacer(Modifier.height(10.dp))
                            Label(stringResource(R.string.lyrics_text_position))
                            ChoiceRow(
                                options = LyricsPosition.entries,
                                selected = lyricsPosition,
                                label = {
                                    when (it) {
                                        LyricsPosition.LEFT -> stringResource(R.string.left)
                                        LyricsPosition.CENTER -> stringResource(R.string.center)
                                        LyricsPosition.RIGHT -> stringResource(R.string.right)
                                    }
                                },
                                onSelect = {
                                    onLyricsPositionChange(it)
                                    markChosen()
                                },
                            )
                            Spacer(Modifier.height(6.dp))
                            SwitchRow(
                                title = stringResource(R.string.lyrics_blur),
                                checked = lyricsBlur,
                                onCheckedChange = {
                                    onLyricsBlurChange(it)
                                    markChosen()
                                },
                            )
                        }

                        OnboardingStep.PAGE -> {
                            SwitchRow(
                                title = stringResource(R.string.page_edge_to_edge_cover),
                                checked = pageEdge,
                                onCheckedChange = onPageEdgeChange,
                            )
                            Spacer(Modifier.height(6.dp))
                            Label(stringResource(R.string.page_background))
                            ChoiceRow(
                                options = PlayerCardBackground.entries,
                                selected = pageBackground,
                                label = {
                                    when (it) {
                                        PlayerCardBackground.CLASSIC -> stringResource(R.string.style_classic)
                                        PlayerCardBackground.GLASS -> stringResource(R.string.style_glass)
                                    }
                                },
                                onSelect = onPageBackgroundChange,
                            )
                            Spacer(Modifier.height(10.dp))
                            Label(stringResource(R.string.onboarding_mini_player))
                            ChoiceRow(
                                options =
                                    listOf(
                                        MiniPlayerBackgroundStyle.DEFAULT,
                                        MiniPlayerBackgroundStyle.BLUR,
                                        MiniPlayerBackgroundStyle.PAGE,
                                    ),
                                selected =
                                    miniBackground.takeIf {
                                        it == MiniPlayerBackgroundStyle.BLUR || it == MiniPlayerBackgroundStyle.PAGE
                                    } ?: MiniPlayerBackgroundStyle.DEFAULT,
                                label = {
                                    when (it) {
                                        MiniPlayerBackgroundStyle.BLUR -> stringResource(R.string.onboarding_mini_music)
                                        MiniPlayerBackgroundStyle.PAGE -> stringResource(R.string.onboarding_mini_page)
                                        else -> stringResource(R.string.onboarding_mini_theme)
                                    }
                                },
                                onSelect = onMiniBackgroundChange,
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}

/** Where the parts of the pretend player sit, for a screen [width] x [height]. */
private data class PlayerGeometry(
    val coverLeft: Dp,
    val coverTop: Dp,
    val coverSize: Dp,
    val coverCorner: Dp,
    val infoTop: Dp,
    val sliderTop: Dp,
    val controlsTop: Dp,
    val controlsCenter: Dp,
    val cardTop: Dp,
)

private fun playerGeometry(
    width: Dp,
    height: Dp,
    statusTop: Dp,
    edge: Float,
): PlayerGeometry {
    val normalSize = minOf(width - 64.dp, height * 0.42f)
    val normalLeft = (width - normalSize) / 2
    val normalTop = statusTop + 44.dp
    val size = lerp(normalSize, width, edge)
    val top = lerp(normalTop, 0.dp, edge)
    val infoTop = top + size + lerp(28.dp, 12.dp, edge)
    val sliderTop = infoTop + 64.dp
    val controlsTop = sliderTop + 64.dp
    return PlayerGeometry(
        coverLeft = lerp(normalLeft, 0.dp, edge),
        coverTop = top,
        coverSize = size,
        coverCorner = lerp(16.dp, 0.dp, edge),
        infoTop = infoTop,
        sliderTop = sliderTop,
        controlsTop = controlsTop,
        controlsCenter = controlsTop + 36.dp,
        cardTop = controlsTop + 84.dp,
    )
}

/**
 * The stretch of the song the lyrics step repeats: the longest run of lines (up to about half a
 * minute) where no gap between two lines is longer than a few seconds.
 */
private fun findLyricsLoop(lines: List<LyricsEntry>): LongRange? {
    if (lines.size < 3) return null
    var best: Pair<Int, Int>? = null
    for (start in lines.indices) {
        var end = start
        while (end + 1 < lines.size &&
            lines[end + 1].time - lines[end].time <= 6_000L &&
            lines[end + 1].time - lines[start].time <= 30_000L
        ) {
            end++
        }
        val current = best
        if (current == null || end - start > current.second - current.first) best = start to end
    }
    val (start, end) = best ?: return null
    if (end - start < 2) return null
    val from = (lines[start].time - 1_500L).coerceAtLeast(0L)
    val to =
        if (end + 1 < lines.size) {
            minOf(lines[end + 1].time - 300L, lines[end].time + 6_000L)
        } else {
            lines[end].time + 4_000L
        }
    return from..to
}

@Composable
private fun WelcomePage(
    onStart: () -> Unit,
    onSkip: () -> Unit,
) {
    // The parts come in one after another.
    val appear = remember { List(4) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        appear.forEachIndexed { index, anim ->
            launch {
                delay(150L + index * 120L)
                anim.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            }
        }
    }
    fun Modifier.appearing(index: Int) =
        graphicsLayer {
            val value = appear[index].value
            alpha = value
            translationY = (1f - value) * 24.dp.toPx()
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(WindowInsets.navigationBars.asPaddingValues()),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .offset(y = (-48).dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .appearing(0)
                        .size(104.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(64.dp),
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.appearing(1),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.appearing(2),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .appearing(3),
        ) {
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_start),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkip) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.onboarding_skip_note),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A cover image, or the album color with a note while it loads. */
@Composable
private fun CoverImage(
    url: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
        if (url == null) {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp),
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Fades the bottom of the content out, so an edge-to-edge cover melts into the page. */
private fun Modifier.bottomFade(amount: Float): Modifier =
    if (amount <= 0f) {
        this
    } else {
        graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            0f to Color.Black,
                            (1f - 0.35f * amount) to Color.Black,
                            1f to Color.Black.copy(alpha = 1f - amount),
                        ),
                    blendMode = BlendMode.DstIn,
                )
            }
    }

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun PlayerPreview(
    song: OnboardingSong,
    lyricsText: String?,
    positionProvider: () -> Long,
    albumColor: Color,
    glass: Boolean,
    geometry: PlayerGeometry,
    edgeFraction: Float,
    lyricsExpand: Float,
    statusTop: Dp,
    lyricsBottomSpace: Dp,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight
        Box(Modifier.fillMaxSize().background(albumBackgroundBrush(albumColor)))
        androidx.compose.animation.AnimatedVisibility(
            visible = glass,
            enter = fadeIn(tween(600)),
            exit = fadeOut(tween(600)),
        ) {
            GlassCoverBackground(thumbnailUrl = song.thumbnailUrl)
        }

        Text(
            text = stringResource(R.string.playing_from) + " · " + song.albumTitle,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 14.dp, start = 32.dp, end = 32.dp)
                    .alpha(1f - edgeFraction),
        )

        CoverImage(
            url = song.thumbnailUrl,
            modifier =
                Modifier
                    .offset(geometry.coverLeft, geometry.coverTop)
                    .size(geometry.coverSize)
                    .bottomFade(edgeFraction)
                    .clip(RoundedCornerShape(geometry.coverCorner)),
        )

        // Title, artist and the like button.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .offset(y = geometry.infoTop)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.player_heart),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // The progress bar moves with the pretend clock; it can't be dragged.
        val position = positionProvider()
        Column(
            modifier =
                Modifier
                    .offset(y = geometry.sliderTop)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
        ) {
            StyledSlider(
                value = (position.toFloat() / song.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f),
                onValueChange = {},
                colors =
                    SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                    ),
                modifier = Modifier.fillMaxWidth().height(32.dp),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text(formatTime(position), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.weight(1f))
                Text(formatTime(song.durationMs), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .offset(y = geometry.controlsTop)
                    .fillMaxWidth()
                    .height(72.dp),
        ) {
            Icon(painterResource(R.drawable.player_backward), null, tint = Color.White, modifier = Modifier.size(40.dp))
            Icon(painterResource(R.drawable.player_pause), null, tint = Color.White, modifier = Modifier.size(52.dp))
            Icon(painterResource(R.drawable.player_forward), null, tint = Color.White, modifier = Modifier.size(40.dp))
        }

        // The lyrics card. In the lyrics step it opens to the whole screen, as in the player.
        val e = lyricsExpand
        val cardTop = lerp(geometry.cardTop, 0.dp, e)
        val side = lerp(8.dp, 0.dp, e)
        val corner = lerp(28.dp, 0.dp, e)
        val panel = if (glass) Color.Black.copy(alpha = lerp(0.18f, 0.35f, e)) else lerp(albumColor, Color.Black, 0.15f)
        Box(
            modifier =
                Modifier
                    .offset(side, cardTop)
                    .size(width - side * 2, height - cardTop + lerp(16.dp, 0.dp, e))
                    .clip(RoundedCornerShape(topStart = corner, topEnd = corner))
                    .background(panel)
                    .border(1.dp, Color.White.copy(alpha = 0.08f * (1f - e)), RoundedCornerShape(topStart = corner, topEnd = corner)),
        ) {
            Column(Modifier.fillMaxSize()) {
                // Opened: the small cover and title on top, like the opened card.
                Box(Modifier.fillMaxWidth().height((statusTop + 64.dp) * e).alpha(e)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .height(64.dp)
                                .padding(horizontal = 24.dp),
                    ) {
                        CoverImage(song.thumbnailUrl, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(song.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .padding(top = 2.dp)
                            .height(40.dp)
                            .padding(horizontal = 24.dp),
                ) {
                    Text(stringResource(R.string.lyrics), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Spacer(Modifier.width(20.dp))
                    Text(stringResource(R.string.up_next), style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.55f))
                }
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    when {
                        lyricsText == null ->
                            CircularProgressIndicator(
                                color = Color.White.copy(alpha = 0.7f),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp),
                            )

                        lyricsText.isBlank() ->
                            Icon(
                                painter = painterResource(R.drawable.lyrics),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp),
                            )

                        else ->
                            CompactLyrics(
                                lyrics = lyricsText,
                                positionProvider = positionProvider,
                                color = Color.White,
                                isPlaying = true,
                                expandFraction = { e },
                                openBottomPadding = lyricsBottomSpace,
                                modifier = Modifier.fillMaxSize(),
                            )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumPagePreview(
    song: OnboardingSong,
    album: OnboardingAlbum?,
    positionProvider: () -> Long,
    albumColor: Color,
    pageGlass: Boolean,
    playerGlass: Boolean,
    edgeFraction: Float,
    miniBackground: MiniPlayerBackgroundStyle,
    statusTop: Dp,
    navBottom: Dp,
) {
    val cover = album?.thumbnailUrl ?: song.thumbnailUrl
    val pageColor = lerp(albumColor, Color.Black, 0.1f)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        androidx.compose.animation.AnimatedContent(
            targetState = pageGlass,
            transitionSpec = { fadeIn(tween(600)).togetherWith(fadeOut(tween(600))) },
            label = "onboardingPageBackground",
        ) { glass ->
            if (glass) {
                PageGlassBackground(url = cover)
            } else {
                Box(Modifier.fillMaxSize().background(pageColor))
            }
        }
        val textColor = if (pageGlass) MaterialTheme.colorScheme.onBackground else Color.White

        // Header: the cover, square in the middle or filling the top edge to edge.
        val normalSize = minOf(width * 0.62f, 260.dp)
        val size = lerp(normalSize, width, edgeFraction)
        val top = lerp(statusTop + 64.dp, 0.dp, edgeFraction)
        CoverImage(
            url = cover,
            modifier =
                Modifier
                    .offset(lerp((width - normalSize) / 2, 0.dp, edgeFraction), top)
                    .size(size)
                    .bottomFade(edgeFraction)
                    .clip(RoundedCornerShape(lerp(16.dp, 0.dp, edgeFraction))),
        )
        Icon(
            painter = painterResource(R.drawable.arrow_back),
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.padding(start = 16.dp, top = statusTop + 16.dp).size(24.dp),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .offset(y = top + size + lerp(20.dp, (-24).dp, edgeFraction))
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
        ) {
            Text(
                text = album?.title ?: song.albumTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = album?.artist ?: song.artist,
                style = MaterialTheme.typography.titleMedium,
                color = textColor.copy(alpha = 0.8f),
            )
            if (album != null) {
                val totalSec = album.songs.sumOf { it.second }
                val parts =
                    listOfNotNull(
                        album.year?.toString(),
                        pluralStringResource(R.plurals.n_song, album.songs.size, album.songs.size),
                        formatTime(totalSec * 1000L),
                    )
                Text(
                    text = parts.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(textColor.copy(alpha = 0.16f))
                            .padding(horizontal = 22.dp),
                ) {
                    Icon(painterResource(R.drawable.play), null, tint = textColor, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.play), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = textColor)
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(textColor.copy(alpha = 0.9f)),
                ) {
                    Icon(painterResource(R.drawable.shuffle), null, tint = pageColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(textColor.copy(alpha = 0.16f)),
                ) {
                    Icon(painterResource(R.drawable.favorite_border), null, tint = textColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            if (album != null) album.songs.take(8).forEachIndexed { index, (title, seconds) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor.copy(alpha = 0.85f),
                        modifier = Modifier.width(36.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = "${album.artist} • ${formatTime(seconds * 1000L)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.65f),
                            maxLines = 1,
                        )
                    }
                    Icon(painterResource(R.drawable.more_vert), null, tint = textColor.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
                }
            }
        }

        // The mini player, in the background chosen for it.
        val miniShape = RoundedCornerShape(32.dp)
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = navBottom + 12.dp)
                    .fillMaxWidth()
                    .height(MiniPlayerHeight)
                    .clip(miniShape)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), miniShape),
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = Triple(miniBackground, pageGlass, playerGlass),
                transitionSpec = { fadeIn(tween(400)).togetherWith(fadeOut(tween(400))) },
                label = "onboardingMiniBackground",
            ) { (style, pageGlassNow, playerGlassNow) ->
                when (style) {
                    MiniPlayerBackgroundStyle.BLUR ->
                        if (playerGlassNow) {
                            GlassCoverBackground(thumbnailUrl = song.thumbnailUrl)
                        } else {
                            Box(Modifier.fillMaxSize().background(albumBackgroundBrush(albumColor)))
                        }

                    MiniPlayerBackgroundStyle.PAGE ->
                        if (pageGlassNow) {
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                        } else {
                            Box(Modifier.fillMaxSize().background(lerp(pageColor, Color.White, 0.06f)))
                        }

                    else -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer))
                }
            }
            val miniText =
                if (miniBackground == MiniPlayerBackgroundStyle.DEFAULT ||
                    (miniBackground != MiniPlayerBackgroundStyle.BLUR && miniBackground != MiniPlayerBackgroundStyle.PAGE)
                ) {
                    MaterialTheme.colorScheme.onSurface
                } else if (miniBackground == MiniPlayerBackgroundStyle.PAGE) {
                    textColor
                } else {
                    Color.White
                }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            ) {
                CoverImage(song.thumbnailUrl, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = miniText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = miniText.copy(alpha = 0.75f), maxLines = 1)
                }
                Icon(painterResource(R.drawable.player_heart_filled), null, tint = miniText, modifier = Modifier.padding(horizontal = 12.dp).size(24.dp))
            }
            // A thin progress line along the bottom.
            val fraction = (positionProvider().toFloat() / song.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 28.dp, vertical = 4.dp)
                    .fillMaxWidth(fraction)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(miniText.copy(alpha = 0.8f)),
            )
        }
    }
}

/** The card holding one step's settings, with Back, Next (once a choice is made) and Skip. */
@Composable
private fun SettingsCard(
    step: OnboardingStep,
    nextVisible: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    content: @Composable ColumnScope.(OnboardingStep) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
        shadowElevation = 12.dp,
        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    (fadeIn(tween(300, delayMillis = 90)) + slideInVertically(tween(350)) { if (forward) it / 6 else -it / 6 })
                        .togetherWith(fadeOut(tween(150)))
                        .using(SizeTransform(clip = false) { _, _ -> spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow) })
                },
                label = "onboardingCardContent",
            ) { shown ->
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text =
                            when (shown) {
                                OnboardingStep.THEME -> stringResource(R.string.onboarding_choose_theme)
                                OnboardingStep.PLAYER_BACKGROUND -> stringResource(R.string.onboarding_player_background)
                                OnboardingStep.SLIDER -> stringResource(R.string.onboarding_slider_style)
                                OnboardingStep.EDGE_TO_EDGE -> stringResource(R.string.edge_to_edge_album_art)
                                OnboardingStep.LYRICS -> stringResource(R.string.lyrics)
                                OnboardingStep.PAGE -> stringResource(R.string.onboarding_album_pages)
                                else -> ""
                            },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    content(shown)
                }
            }
            Spacer(Modifier.height(6.dp))
            StepNavigation(
                nextVisible = nextVisible,
                nextText = stringResource(R.string.next),
                onBack = onBack,
                onNext = onNext,
                onSkip = onSkip,
            )
        }
    }
}

/** Back on the left, Skip on the right and, once there's something to go on with, Next between them. */
@Composable
private fun StepNavigation(
    nextVisible: Boolean,
    nextText: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().height(56.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(painterResource(R.drawable.arrow_back), null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.back))
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = nextVisible,
            enter = fadeIn(tween(250)) + scaleIn(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium), initialScale = 0.6f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp),
            ) {
                Text(nextText, fontWeight = FontWeight.SemiBold)
            }
        }
        TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterEnd)) {
            Text(stringResource(R.string.onboarding_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun ThemeChoices(
    current: DarkMode,
    onSelect: (DarkMode) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        listOf(DarkMode.AUTO, DarkMode.OFF, DarkMode.ON).forEach { mode ->
            val selected = current == mode
            val background by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                tween(250),
                label = "themeChoice",
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(background)
                        .clickable { onSelect(mode) }
                        .padding(vertical = 12.dp),
            ) {
                ModeCircle(
                    darkMode = current,
                    pureBlack = false,
                    targetMode = mode,
                    targetPureBlack = false,
                    showIcon = mode == DarkMode.AUTO,
                    onClick = { onSelect(mode) },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text =
                        when (mode) {
                            DarkMode.AUTO -> stringResource(R.string.onboarding_theme_system)
                            DarkMode.OFF -> stringResource(R.string.onboarding_theme_light)
                            DarkMode.ON -> stringResource(R.string.onboarding_theme_dark)
                        },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val background by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                tween(250),
                label = "choiceBackground",
            )
            val content by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                tween(250),
                label = "choiceContent",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(background)
                        .clickable { onSelect(option) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            tint = content,
                            modifier = Modifier.padding(end = 6.dp).size(18.dp),
                        )
                    }
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = content,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    dim: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 4.dp),
    ) {
        Text(
            text = title,
            style = if (dim) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            color = if (dim) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ArtistPicker(
    viewModel: OnboardingViewModel,
    statusTop: Dp,
    navBottom: Dp,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
) {
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val loading by viewModel.suggestionsLoading.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.searchResults.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val selectedIds = remember(selected) { selected.map { it.id }.toSet() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = statusTop + 20.dp, bottom = navBottom)
                .imePadding(),
    ) {
        Text(
            text = stringResource(R.string.onboarding_artists_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            text = stringResource(R.string.onboarding_artists_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 16.dp),
        )

        // Search bar.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp),
        ) {
            Icon(painterResource(R.drawable.search), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.onboarding_search_artists),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AnimatedVisibility(visible = query.isNotEmpty(), enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { viewModel.onQueryChange("") }
                            .padding(7.dp),
                )
            }
        }

        // Only artists, in rounded squares.
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier =
                Modifier
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            val searching = query.isNotBlank()
            val shown: List<OnboardingArtist>? =
                if (searching) results?.map { OnboardingArtist(it) } else suggestions
            AnimatedContent(
                targetState =
                    when {
                        shown == null || (shown.isEmpty() && !searching && loading) -> 0
                        shown.isEmpty() -> 1
                        else -> 2
                    },
                transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(200))) },
                label = "onboardingArtistsState",
            ) { state ->
                when (state) {
                    0 ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                        }

                    1 ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.onboarding_no_artists),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                    else ->
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(shown.orEmpty(), key = { it.item.id }) { artist ->
                                ArtistTile(
                                    artist = artist,
                                    selected = artist.item.id in selectedIds,
                                    onClick = {
                                        keyboard?.hide()
                                        viewModel.toggle(artist.item)
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                }
            }
        }

        StepNavigation(
            nextVisible = selected.isNotEmpty(),
            nextText = stringResource(R.string.onboarding_finish),
            onBack = onBack,
            onNext = onFinish,
            onSkip = onSkip,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ArtistTile(
    artist: OnboardingArtist,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    val scale by animateFloatAsState(if (selected) 0.94f else 1f, spring(dampingRatio = 0.6f), label = "artistScale")
    val border by animateDpAsState(if (selected) 3.dp else 0.dp, tween(200), label = "artistBorder")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            modifier
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }.clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(border, MaterialTheme.colorScheme.primary, shape),
        ) {
            AsyncImage(
                model = artist.item.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            val tint by animateFloatAsState(if (selected) 1f else 0f, tween(200), label = "artistTint")
            val badge by animateFloatAsState(
                if (selected) 1f else 0f,
                spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "artistBadge",
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = tint }
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .graphicsLayer {
                            alpha = badge.coerceIn(0f, 1f)
                            scaleX = badge
                            scaleY = badge
                        }.size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (artist.similarTo != null && !selected) {
                Text(
                    text = stringResource(R.string.onboarding_similar),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            text = artist.item.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
    }
}
