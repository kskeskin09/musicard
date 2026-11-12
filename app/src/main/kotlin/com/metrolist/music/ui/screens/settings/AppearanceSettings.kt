/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.SnackbarHostState
import com.metrolist.music.ui.component.ThemedSwitch as Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.ChipSortTypeKey
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.DefaultOpenTabKey
import com.metrolist.music.constants.DensityScale
import com.metrolist.music.constants.DensityScaleKey
import com.metrolist.music.constants.DynamicThemeKey
import com.metrolist.music.constants.AppLogoKey
import com.metrolist.music.constants.LogoThemeKey
import com.metrolist.music.constants.EnableHighRefreshRateKey
import com.metrolist.music.constants.EnableLandscapeScalingKey
import com.metrolist.music.constants.ExperimentalLyricsKey
import com.metrolist.music.constants.ForceBottomNavBarKey
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.HidePlayerThumbnailKey
import com.metrolist.music.constants.HideStatusBarOnFullscreenKey
import com.metrolist.music.constants.LibraryFilter
import com.metrolist.music.constants.ListenTogetherInTopBarKey
import com.metrolist.music.constants.LyricsAnimationStyle
import com.metrolist.music.constants.LyricsAnimationStyleKey
import com.metrolist.music.constants.LyricsClickKey
import com.metrolist.music.constants.LyricsGlowEffectKey
import com.metrolist.music.constants.LyricsLineSpacingKey
import com.metrolist.music.constants.LyricsScrollKey
import com.metrolist.music.constants.LyricStyle
import com.metrolist.music.constants.LyricStyleKey
import com.metrolist.music.constants.LyricsBlurKey
import com.metrolist.music.constants.LyricsTextPositionKey
import com.metrolist.music.constants.PlayerCardBackground
import com.metrolist.music.constants.PlayerCardBackgroundKey
import com.metrolist.music.constants.LyricsTextSizeKey
import com.metrolist.music.constants.MiniPlayerBackgroundStyle
import com.metrolist.music.constants.MiniPlayerBackgroundStyleKey
import com.metrolist.music.constants.MiniPlayerBlackoutKey
import com.metrolist.music.constants.MiniPlayerBlurKey
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerButtonsStyle
import com.metrolist.music.constants.PlayerButtonsStyleKey
import com.metrolist.music.constants.RespectAgentPositioningKey
import com.metrolist.music.constants.SelectedThemeColorKey
import com.metrolist.music.constants.ShowCachedPlaylistKey
import com.metrolist.music.constants.ShowDownloadedPlaylistKey
import com.metrolist.music.constants.ShowLikedPlaylistKey
import com.metrolist.music.constants.ShowTopPlaylistKey
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.constants.SlimNavBarKey
import com.metrolist.music.constants.SwipeSensitivityKey
import com.metrolist.music.constants.SwipeThumbnailKey
import com.metrolist.music.constants.SwipeToRemoveSongKey
import com.metrolist.music.constants.SwipeToSongKey
import com.metrolist.music.constants.UseNewPlayerDesignKey
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.PlayerSliderTrack
import com.metrolist.music.ui.component.StyledSlider
import com.metrolist.music.ui.theme.DefaultThemeColor
import com.metrolist.music.ui.theme.PlayerSliderColors
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.AppLogo
import com.metrolist.music.utils.LogoTheme
import com.metrolist.music.utils.LogoUtils
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
) {
    val (dynamicTheme, onDynamicThemeChange) =
        rememberPreference(
            DynamicThemeKey,
            defaultValue = true,
        )
    val (appLogo, onAppLogoPrefChange) =
        rememberEnumPreference(AppLogoKey, defaultValue = AppLogo.SINGLE_NOTE)
    val (logoTheme, onLogoThemePrefChange) =
        rememberEnumPreference(LogoThemeKey, defaultValue = LogoTheme.BLACK)
    val iconContext = LocalContext.current
    val onAppLogoChange: (AppLogo) -> Unit = { newLogo ->
        onAppLogoPrefChange(newLogo)
        LogoUtils.setLogo(iconContext, newLogo, logoTheme)
    }
    val onLogoThemeChange: (LogoTheme) -> Unit = { newTheme ->
        onLogoThemePrefChange(newTheme)
        LogoUtils.setLogo(iconContext, appLogo, newTheme)
    }
    val (enableHighRefreshRate, onEnableHighRefreshRateChange) =
        rememberPreference(
            EnableHighRefreshRateKey,
            defaultValue = true,
        )
    val (enableLandscapeScaling, onEnableLandscapeScalingChange) =
        rememberPreference(
            EnableLandscapeScalingKey,
            defaultValue = false,
        )
    val (selectedThemeColorInt) =
        rememberPreference(
            SelectedThemeColorKey,
            defaultValue = DefaultThemeColor.toArgb(),
        )
    // Check if user has selected a custom color (not the default/dynamic color)
    val isUsingCustomColor = selectedThemeColorInt != DefaultThemeColor.toArgb()

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) =
        rememberPreference(
            UseNewPlayerDesignKey,
            defaultValue = true,
        )
    val (miniPlayerBackground, onMiniPlayerBackgroundChange) =
        rememberEnumPreference(
            MiniPlayerBackgroundStyleKey,
            defaultValue = MiniPlayerBackgroundStyle.DEFAULT,
        )

    // Follow theme, or Follow background (the player's background: the glass cover or the album
    // color). Older choices count as Follow theme.
    val availableMiniPlayerBackgroundStyles =
        listOf(MiniPlayerBackgroundStyle.DEFAULT, MiniPlayerBackgroundStyle.BLUR)

    var showMiniPlayerBackgroundDialog by rememberSaveable { mutableStateOf(false) }

    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) =
        rememberPreference(
            HidePlayerThumbnailKey,
            defaultValue = false,
        )
    val (cropAlbumArt, onCropAlbumArtChange) =
        rememberPreference(
            CropAlbumArtKey,
            defaultValue = false,
        )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.DEFAULT,
        )

    val (playerCardBackground, onPlayerCardBackgroundChange) =
        rememberEnumPreference(PlayerCardBackgroundKey, defaultValue = PlayerCardBackground.CLASSIC)
    val (lyricStyle, onLyricStyleChange) =
        rememberEnumPreference(LyricStyleKey, defaultValue = LyricStyle.CLASSIC)
    val (lyricsBlur, onLyricsBlurChange) = rememberPreference(LyricsBlurKey, defaultValue = false)
    var showPlayerCardBackgroundDialog by rememberSaveable { mutableStateOf(false) }
    var showLyricStyleDialog by rememberSaveable { mutableStateOf(false) }

    if (showPlayerCardBackgroundDialog) {
        EnumDialog(
            onDismiss = { showPlayerCardBackgroundDialog = false },
            onSelect = {
                onPlayerCardBackgroundChange(it)
                showPlayerCardBackgroundDialog = false
            },
            title = stringResource(R.string.player_background),
            current = playerCardBackground,
            values = PlayerCardBackground.entries,
            valueText = {
                when (it) {
                    PlayerCardBackground.CLASSIC -> stringResource(R.string.style_classic)
                    PlayerCardBackground.GLASS -> stringResource(R.string.style_glass)
                }
            },
        )
    }

    if (showLyricStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricStyleDialog = false },
            onSelect = {
                onLyricStyleChange(it)
                showLyricStyleDialog = false
            },
            title = stringResource(R.string.lyric_style),
            current = lyricStyle,
            values = LyricStyle.entries,
            valueText = {
                when (it) {
                    LyricStyle.CLASSIC -> stringResource(R.string.style_classic)
                    LyricStyle.MODERN -> stringResource(R.string.style_modern)
                }
            },
        )
    }

    val (defaultOpenTab, onDefaultOpenTabChange) =
        rememberEnumPreference(
            DefaultOpenTabKey,
            defaultValue = NavigationTab.HOME,
        )
    val (playerButtonsStyle, onPlayerButtonsStyleChange) =
        rememberEnumPreference(
            PlayerButtonsStyleKey,
            defaultValue = PlayerButtonsStyle.DEFAULT,
        )
    val (lyricsPosition, onLyricsPositionChange) =
        rememberEnumPreference(
            LyricsTextPositionKey,
            defaultValue = LyricsPosition.CENTER,
        )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) =
        rememberPreference(
            LyricsScrollKey,
            defaultValue = true,
        )
    val (hideStatusBarOnFullscreen, onHideStatusBarOnFullscreenChange) =
        rememberPreference(
            HideStatusBarOnFullscreenKey,
            defaultValue = false,
        )
    val (respectAgentPositioning, onRespectAgentPositioningChange) = rememberPreference(RespectAgentPositioningKey, defaultValue = true)
    val (experimentalLyrics, onExperimentalLyricsChange) = rememberPreference(ExperimentalLyricsKey, defaultValue = true)

    val (lyricsGlowEffect, onLyricsGlowEffectChange) = rememberPreference(LyricsGlowEffectKey, defaultValue = false)
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) =
        rememberEnumPreference(
            LyricsAnimationStyleKey,
            defaultValue = LyricsAnimationStyle.FADE,
        )
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 24f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.2f)

    var showExperimentalLyricsBetaDialog by remember { mutableStateOf(false) }
    var showLyricsAnimationStyleDialog by remember { mutableStateOf(false) }
    var showLyricsTextSizeDialog by remember { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by remember { mutableStateOf(false) }

    val (sliderStyle, onSliderStyleChange) =
        rememberEnumPreference(
            SliderStyleKey,
            defaultValue = SliderStyle.DEFAULT,
        )
    val (swipeThumbnail, onSwipeThumbnailChange) =
        rememberPreference(
            SwipeThumbnailKey,
            defaultValue = true,
        )
    val (swipeSensitivity, onSwipeSensitivityChange) =
        rememberPreference(
            SwipeSensitivityKey,
            defaultValue = 0.73f,
        )
    val (gridItemSize, onGridItemSizeChange) =
        rememberEnumPreference(
            GridItemsSizeKey,
            defaultValue = GridItemSize.SMALL,
        )

    val (forceBottomNav, onForceBottomNavChange) =
        rememberPreference(
            ForceBottomNavBarKey,
            defaultValue = false,
        )
    val (slimNav, onSlimNavChange) =
        rememberPreference(
            SlimNavBarKey,
            defaultValue = false,
        )

    val (densityScale, onDensityScaleChange) = rememberPreference(DensityScaleKey, defaultValue = 1f)
    var showDensityScaleDialog by rememberSaveable { mutableStateOf(false) }

    val (listenTogetherInTopBar, onListenTogetherInTopBarChange) =
        rememberPreference(
            ListenTogetherInTopBarKey,
            defaultValue = true,
        )

    val (swipeToSong, onSwipeToSongChange) =
        rememberPreference(
            SwipeToSongKey,
            defaultValue = false,
        )

    val (swipeToRemoveSong, onSwipeToRemoveSongChange) =
        rememberPreference(
            SwipeToRemoveSongKey,
            defaultValue = false,
        )

    val (showLikedPlaylist, onShowLikedPlaylistChange) =
        rememberPreference(
            ShowLikedPlaylistKey,
            defaultValue = true,
        )
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) =
        rememberPreference(
            ShowDownloadedPlaylistKey,
            defaultValue = true,
        )
    val (showTopPlaylist, onShowTopPlaylistChange) =
        rememberPreference(
            ShowTopPlaylistKey,
            defaultValue = true,
        )
    val (showCachedPlaylist, onShowCachedPlaylistChange) =
        rememberPreference(
            ShowCachedPlaylistKey,
            defaultValue = true,
        )

    val availableBackgroundStyles =
        PlayerBackgroundStyle.entries.filter {
            it != PlayerBackgroundStyle.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }

    val (defaultChip, onDefaultChipChange) =
        rememberEnumPreference(
            key = ChipSortTypeKey,
            defaultValue = LibraryFilter.LIBRARY,
        )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showPlayerBackgroundDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showPlayerButtonsStyleDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showLyricsPositionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = {
                onLyricsPositionChange(it)
                showLyricsPositionDialog = false
            },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.values().toList(),
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            },
        )
    }

    if (showLyricsAnimationStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricsAnimationStyleDialog = false },
            onSelect = {
                onLyricsAnimationStyleChange(it)
                showLyricsAnimationStyleDialog = false
            },
            title = stringResource(R.string.lyrics_animation_style_title),
            current = lyricsAnimationStyle,
            values = LyricsAnimationStyle.values().toList(),
            valueText = {
                when (it) {
                    LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_animation_none)
                    LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_animation_fade)
                    LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_animation_glow)
                    LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
                    LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_animation_karaoke)
                    LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
                }
            },
        )
    }

    if (showLyricsTextSizeDialog) {
        var tempTextSize by remember { mutableFloatStateOf(lyricsTextSize) }

        DefaultDialog(
            onDismiss = {
                tempTextSize = lyricsTextSize
                showLyricsTextSizeDialog = false
            },
            buttons = {
                TextButton(
                    onClick = {
                        tempTextSize = 24f
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempTextSize = lyricsTextSize
                        showLyricsTextSizeDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onLyricsTextSizeChange(tempTextSize)
                        showLyricsTextSizeDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.lyrics_text_size),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Text(
                    text = "${tempTextSize.roundToInt()} sp",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                StyledSlider(
                    value = tempTextSize,
                    onValueChange = { tempTextSize = it },
                    valueRange = 12f..48f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showLyricsLineSpacingDialog) {
        var tempLineSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }

        DefaultDialog(
            onDismiss = {
                tempLineSpacing = lyricsLineSpacing
                showLyricsLineSpacingDialog = false
            },
            buttons = {
                TextButton(
                    onClick = {
                        tempLineSpacing = 1.3f
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempLineSpacing = lyricsLineSpacing
                        showLyricsLineSpacingDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onLyricsLineSpacingChange(tempLineSpacing)
                        showLyricsLineSpacingDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.lyrics_line_spacing),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Text(
                    text = String.format(Locale.US, "%.1f", tempLineSpacing),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                StyledSlider(
                    value = tempLineSpacing,
                    onValueChange = { tempLineSpacing = it },
                    valueRange = 1.0f..3.0f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showPlayerButtonsStyleDialog) {
        EnumDialog(
            onDismiss = { showPlayerButtonsStyleDialog = false },
            onSelect = {
                onPlayerButtonsStyleChange(it)
                showPlayerButtonsStyleDialog = false
            },
            title = stringResource(R.string.player_buttons_style),
            current = playerButtonsStyle,
            values = PlayerButtonsStyle.values().toList(),
            valueText = {
                when (it) {
                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                }
            },
        )
    }

    if (showPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showPlayerBackgroundDialog = false },
            onSelect = {
                onPlayerBackgroundChange(it)
                showPlayerBackgroundDialog = false
            },
            title = stringResource(R.string.player_background_style),
            current = playerBackground,
            values = availableBackgroundStyles,
            valueText = {
                when (it) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                }
            },
        )
    }

    if (showMiniPlayerBackgroundDialog) {
        EnumDialog(
            onDismiss = { showMiniPlayerBackgroundDialog = false },
            onSelect = {
                onMiniPlayerBackgroundChange(it)
                showMiniPlayerBackgroundDialog = false
            },
            title = stringResource(R.string.mini_player_background_style),
            current = if (miniPlayerBackground == MiniPlayerBackgroundStyle.BLUR) miniPlayerBackground else MiniPlayerBackgroundStyle.DEFAULT,
            values = availableMiniPlayerBackgroundStyles,
            valueText = {
                if (it == MiniPlayerBackgroundStyle.BLUR) {
                    stringResource(R.string.follow_background)
                } else {
                    stringResource(R.string.follow_theme)
                }
            },
        )
    }

    var showDefaultOpenTabDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDefaultOpenTabDialog) {
        EnumDialog(
            onDismiss = { showDefaultOpenTabDialog = false },
            onSelect = {
                onDefaultOpenTabChange(it)
                showDefaultOpenTabDialog = false
            },
            title = stringResource(R.string.default_open_tab),
            current = defaultOpenTab,
            values = NavigationTab.values().toList(),
            valueText = {
                when (it) {
                    NavigationTab.HOME -> stringResource(R.string.home)
                    NavigationTab.SEARCH -> stringResource(R.string.search)
                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                }
            },
        )
    }

    var showDefaultChipDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDefaultChipDialog) {
        EnumDialog(
            onDismiss = { showDefaultChipDialog = false },
            onSelect = {
                onDefaultChipChange(it)
                showDefaultChipDialog = false
            },
            title = stringResource(R.string.default_lib_chips),
            current = defaultChip,
            values = LibraryFilter.values().toList(),
            valueText = {
                when (it) {
                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                    LibraryFilter.PODCASTS -> stringResource(R.string.filter_podcasts)
                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                }
            },
        )
    }

    var showGridSizeDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showGridSizeDialog) {
        EnumDialog(
            onDismiss = { showGridSizeDialog = false },
            onSelect = {
                onGridItemSizeChange(it)
                showGridSizeDialog = false
            },
            title = stringResource(R.string.grid_cell_size),
            current = gridItemSize,
            values = GridItemSize.values().toList(),
            valueText = {
                when (it) {
                    GridItemSize.BIG -> stringResource(R.string.big)
                    GridItemSize.SMALL -> stringResource(R.string.small)
                }
            },
        )
    }

    if (showDensityScaleDialog) {
        DefaultDialog(
            onDismiss = { showDensityScaleDialog = false },
            buttons = {
                TextButton(
                    onClick = { showDensityScaleDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        ) {
            Column {
                DensityScale.entries.forEach { scale ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDensityScaleChange(scale.value)
                                    showDensityScaleDialog = false
                                }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = scale.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                if (densityScale == scale.value) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    }
                }
            }
        }
    }

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            },
        ) {
            val sliderPreviewColors =
                PlayerSliderColors.getSliderColors(
                    MaterialTheme.colorScheme.primary,
                    PlayerBackgroundStyle.DEFAULT,
                    isSystemInDarkTheme(),
                )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // One full-width card per style, Classic first.
                listOf(SliderStyle.SLIM, SliderStyle.DEFAULT).forEach { style ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    if (sliderStyle == style) 2.dp else 1.dp,
                                    if (sliderStyle == style) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    RoundedCornerShape(16.dp),
                                ).clickable {
                                    onSliderStyleChange(style)
                                    showSliderOptionDialog = false
                                }.padding(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        if (style == SliderStyle.SLIM) {
                            val sliderState = remember { SliderState(value = 0.62f) }
                            Slider(
                                state = sliderState,
                                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                track = { sliderState ->
                                    PlayerSliderTrack(
                                        sliderState = sliderState,
                                        colors = sliderPreviewColors,
                                    )
                                },
                                colors = sliderPreviewColors,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth().height(24.dp),
                            )
                        } else {
                            Slider(
                                value = 0.35f,
                                valueRange = 0f..1f,
                                onValueChange = { /* preview only */ },
                                colors = sliderPreviewColors,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Text(
                            text = sliderStyleName(style),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    val (likeIconColor, onLikeIconColorChange) =
        rememberEnumPreference(com.metrolist.music.constants.LikeIconColorKey, defaultValue = com.metrolist.music.ui.theme.LikeIconColor.WHITE)

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
            .verticalScroll(rememberScrollState())
            // Inside the scroll, so the page runs on under the mini player.
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = stringResource(R.string.theme),
            items =
                buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.favorite),
                            title = { Text(stringResource(R.string.like_icon_color)) },
                            description = {
                                Text(
                                    stringResource(
                                        if (likeIconColor == com.metrolist.music.ui.theme.LikeIconColor.RED) R.string.like_icon_color_red else R.string.like_icon_color_white,
                                    ),
                                )
                            },
                            trailingContent = {
                                // White or red, as a small two-part switch.
                                Row(
                                    modifier =
                                        Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                            .padding(3.dp),
                                ) {
                                    com.metrolist.music.ui.theme.LikeIconColor.entries.forEach { option ->
                                        val selected = option == likeIconColor
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier =
                                                Modifier
                                                    .size(width = 40.dp, height = 30.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f) else Color.Transparent)
                                                    .clickable { onLikeIconColorChange(option) },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.favorite),
                                                contentDescription = null,
                                                tint = if (option == com.metrolist.music.ui.theme.LikeIconColor.RED) Color(0xFFF0455A) else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                onLikeIconColorChange(
                                    if (likeIconColor == com.metrolist.music.ui.theme.LikeIconColor.RED) com.metrolist.music.ui.theme.LikeIconColor.WHITE else com.metrolist.music.ui.theme.LikeIconColor.RED,
                                )
                            },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.speed),
                            title = { Text(stringResource(R.string.enable_high_refresh_rate)) },
                            description = { Text(stringResource(R.string.enable_high_refresh_rate_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = enableHighRefreshRate,
                                    onCheckedChange = onEnableHighRefreshRateChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (enableHighRefreshRate) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onEnableHighRefreshRateChange(!enableHighRefreshRate) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.fullscreen),
                            title = { Text(stringResource(R.string.enable_landscape_scaling)) },
                            description = { Text(stringResource(R.string.enable_landscape_scaling_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = enableLandscapeScaling,
                                    onCheckedChange = onEnableLandscapeScalingChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (enableLandscapeScaling) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onEnableLandscapeScalingChange(!enableLandscapeScaling) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.palette),
                            title = { Text(stringResource(R.string.theme)) },
                            description = { Text(stringResource(R.string.theme_desc)) },
                            onClick = { navController.navigate("settings/appearance/theme") },
                        ),
                    )
                },
        )

        Spacer(modifier = Modifier.height(27.dp))

        // Launcher logo: which mark, and its fixed colours.
        val logoColors: (LogoTheme) -> Pair<Color, Color> = { theme -> theme.background to theme.foreground }
        Material3SettingsGroup(
            title = stringResource(R.string.logo),
            items =
                listOf(
                    Material3SettingsItem(
                        title = {
                            Column {
                                Text(stringResource(R.string.logo))
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    stringResource(R.string.logo_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    val (bg, fg) = logoColors(logoTheme)
                                    AppLogo.entries.forEach { logo ->
                                        val selected = logo == appLogo
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier =
                                                Modifier
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable { onAppLogoChange(logo) }
                                                    .padding(4.dp),
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier =
                                                    Modifier
                                                        .size(60.dp)
                                                        .border(
                                                            width = 2.dp,
                                                            color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                            // Launchers show the icon as a rounded square, so the preview does too.
                                                            shape = RoundedCornerShape(18.dp),
                                                        ).padding(4.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(bg)
                                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                                            ) {
                                                Icon(
                                                    painter = painterResource(logo.glyph),
                                                    contentDescription = stringResource(logo.label),
                                                    tint = fg,
                                                    // The glyph sits in a 108dp adaptive canvas; draw it larger than the circle like a launcher does.
                                                    modifier = Modifier.requiredSize(78.dp),
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                stringResource(logo.label),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    ),
                    Material3SettingsItem(
                        title = {
                            Column {
                                Text(stringResource(R.string.logo_theme))
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    stringResource(R.string.logo_theme_desc, stringResource(logoTheme.label)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                            .padding(3.dp),
                                ) {
                                    LogoTheme.entries.forEach { theme ->
                                        val selected = theme == logoTheme
                                        val (bg, fg) = logoColors(theme)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier =
                                                Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f) else Color.Transparent)
                                                    .clickable { onLogoThemeChange(theme) },
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(16.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(bg)
                                                    .border(1.dp, fg.copy(alpha = 0.6f), RoundedCornerShape(5.dp)),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                stringResource(theme.label),
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(27.dp))

        var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }

        Material3SettingsGroup(
            title = stringResource(R.string.music_player_appearance),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.gradient),
                        title = { Text(stringResource(R.string.player_background)) },
                        description = {
                            Text(
                                when (playerCardBackground) {
                                    PlayerCardBackground.CLASSIC -> stringResource(R.string.style_classic)
                                    PlayerCardBackground.GLASS -> stringResource(R.string.style_glass)
                                },
                            )
                        },
                        onClick = { showPlayerCardBackgroundDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyric_style)) },
                        description = {
                            Text(
                                when (lyricStyle) {
                                    LyricStyle.CLASSIC -> stringResource(R.string.style_classic)
                                    LyricStyle.MODERN -> stringResource(R.string.style_modern)
                                },
                            )
                        },
                        onClick = { showLyricStyleDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_blur)) },
                        description = { Text(stringResource(R.string.lyrics_blur_desc)) },
                        trailingContent = {
                            Switch(
                                checked = lyricsBlur,
                                onCheckedChange = onLyricsBlurChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (lyricsBlur) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onLyricsBlurChange(!lyricsBlur) },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(27.dp))

        val (miniPlayerBlackout, onMiniPlayerBlackoutChange) =
            rememberPreference(MiniPlayerBlackoutKey, defaultValue = true)
        val (miniPlayerBlur, onMiniPlayerBlurChange) =
            rememberPreference(MiniPlayerBlurKey, defaultValue = false)

        Material3SettingsGroup(
            title = stringResource(id = R.string.mini_player),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.gradient),
                        title = { Text(stringResource(R.string.mini_player_background_style)) },
                        description = {
                            Text(
                                if (miniPlayerBackground == MiniPlayerBackgroundStyle.BLUR) {
                                    stringResource(R.string.follow_background)
                                } else {
                                    stringResource(R.string.follow_theme)
                                },
                            )
                        },
                        onClick = { showMiniPlayerBackgroundDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.contrast),
                        title = { Text(stringResource(R.string.mini_player_blackout)) },
                        description = { Text(stringResource(R.string.mini_player_blackout_desc)) },
                        trailingContent = {
                            Switch(
                                checked = miniPlayerBlackout,
                                onCheckedChange = onMiniPlayerBlackoutChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (miniPlayerBlackout) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onMiniPlayerBlackoutChange(!miniPlayerBlackout) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.blur_on),
                        title = { Text(stringResource(R.string.mini_player_blur)) },
                        description = { Text(stringResource(R.string.mini_player_blur_desc)) },
                        trailingContent = {
                            Switch(
                                checked = miniPlayerBlur,
                                onCheckedChange = onMiniPlayerBlurChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (miniPlayerBlur) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onMiniPlayerBlurChange(!miniPlayerBlur) },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.notification_center),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.notification),
                        title = { Text(stringResource(R.string.notification_center)) },
                        description = { Text(stringResource(R.string.notification_center_desc)) },
                        onClick = { navController.navigate("settings/appearance/notification_center") },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.player),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.hide_image),
                        title = { Text(stringResource(R.string.hide_player_thumbnail)) },
                        description = { Text(stringResource(R.string.hide_player_thumbnail_desc)) },
                        trailingContent = {
                            Switch(
                                checked = hidePlayerThumbnail,
                                onCheckedChange = onHidePlayerThumbnailChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (hidePlayerThumbnail) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onHidePlayerThumbnailChange(!hidePlayerThumbnail) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.crop),
                        title = { Text(stringResource(R.string.crop_album_art)) },
                        description = { Text(stringResource(R.string.crop_album_art_desc)) },
                        trailingContent = {
                            Switch(
                                checked = cropAlbumArt,
                                onCheckedChange = onCropAlbumArtChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (cropAlbumArt) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onCropAlbumArtChange(!cropAlbumArt) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.player_buttons_style)) },
                        description = {
                            Text(
                                when (playerButtonsStyle) {
                                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                                },
                            )
                        },
                        onClick = { showPlayerButtonsStyleDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.sliders),
                        title = { Text(stringResource(R.string.player_slider_style)) },
                        description = {
                            Text(
                                sliderStyleName(sliderStyle),
                            )
                        },
                        onClick = { showSliderOptionDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.swipe),
                        title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                        trailingContent = {
                            Switch(
                                checked = swipeThumbnail,
                                onCheckedChange = onSwipeThumbnailChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (swipeThumbnail) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onSwipeThumbnailChange(!swipeThumbnail) },
                    ),
                ) +
                    if (swipeThumbnail) {
                        listOf(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.tune),
                                title = { Text(stringResource(R.string.swipe_sensitivity)) },
                                description = {
                                    Text(
                                        stringResource(
                                            R.string.sensitivity_percentage,
                                            (swipeSensitivity * 100).roundToInt(),
                                        ),
                                    )
                                },
                                onClick = { showSensitivityDialog = true },
                            ),
                        )
                    } else {
                        emptyList()
                    },
        )

        if (showSensitivityDialog) {
            var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }

            DefaultDialog(
                onDismiss = {
                    tempSensitivity = swipeSensitivity
                    showSensitivityDialog = false
                },
                buttons = {
                    TextButton(
                        onClick = {
                            tempSensitivity = 0.73f
                        },
                    ) {
                        Text(stringResource(R.string.reset))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            tempSensitivity = swipeSensitivity
                            showSensitivityDialog = false
                        },
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            onSwipeSensitivityChange(tempSensitivity)
                            showSensitivityDialog = false
                        },
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.swipe_sensitivity),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    Text(
                        text =
                            stringResource(
                                R.string.sensitivity_percentage,
                                (tempSensitivity * 100).roundToInt(),
                            ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    StyledSlider(
                        value = tempSensitivity,
                        onValueChange = { tempSensitivity = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.lyrics),
            items =
                buildList {

                    if (!experimentalLyrics) {
                    }

                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics_text_position)) },
                            description = {
                                Text(
                                    when (lyricsPosition) {
                                        LyricsPosition.LEFT -> stringResource(R.string.left)
                                        LyricsPosition.CENTER -> stringResource(R.string.center)
                                        LyricsPosition.RIGHT -> stringResource(R.string.right)
                                    },
                                )
                            },
                            onClick = { showLyricsPositionDialog = true },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.respect_agent_positioning)) },
                            description = { Text(stringResource(R.string.respect_agent_positioning_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = respectAgentPositioning,
                                    onCheckedChange = onRespectAgentPositioningChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (respectAgentPositioning) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onRespectAgentPositioningChange(!respectAgentPositioning) },
                        ),
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.hide_status_bar_fullscreen)) },
                            description = { Text(stringResource(R.string.hide_status_bar_fullscreen_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = hideStatusBarOnFullscreen,
                                    onCheckedChange = onHideStatusBarOnFullscreenChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (hideStatusBarOnFullscreen) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onHideStatusBarOnFullscreenChange(!hideStatusBarOnFullscreen) },
                        ),
                    )
                },
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.misc),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.nav_bar),
                        title = { Text(stringResource(R.string.default_open_tab)) },
                        description = {
                            Text(
                                when (defaultOpenTab) {
                                    NavigationTab.HOME -> stringResource(R.string.home)
                                    NavigationTab.SEARCH -> stringResource(R.string.search)
                                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                                },
                            )
                        },
                        onClick = { showDefaultOpenTabDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.tab),
                        title = { Text(stringResource(R.string.default_lib_chips)) },
                        description = {
                            Text(
                                when (defaultChip) {
                                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                    LibraryFilter.PODCASTS -> stringResource(R.string.filter_podcasts)
                                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                                },
                            )
                        },
                        onClick = { showDefaultChipDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.swipe),
                        title = { Text(stringResource(R.string.swipe_song_to_add)) },
                        trailingContent = {
                            Switch(
                                checked = swipeToSong,
                                onCheckedChange = onSwipeToSongChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (swipeToSong) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onSwipeToSongChange(!swipeToSong) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.swipe),
                        title = { Text(stringResource(R.string.swipe_song_to_remove)) },
                        trailingContent = {
                            Switch(
                                checked = swipeToRemoveSong,
                                onCheckedChange = onSwipeToRemoveSongChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (swipeToRemoveSong) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onSwipeToRemoveSongChange(!swipeToRemoveSong) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.nav_bar),
                        title = { Text(stringResource(R.string.force_bottom_navbar)) },
                        trailingContent = {
                            Switch(
                                checked = forceBottomNav,
                                onCheckedChange = onForceBottomNavChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (forceBottomNav) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onForceBottomNavChange(!forceBottomNav) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.nav_bar),
                        title = { Text(stringResource(R.string.slim_navbar)) },
                        trailingContent = {
                            Switch(
                                checked = slimNav,
                                onCheckedChange = onSlimNavChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (slimNav) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onSlimNavChange(!slimNav) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.group_outlined),
                        title = { Text(stringResource(R.string.listen_together_in_top_bar)) },
                        description = { Text(stringResource(R.string.listen_together_in_top_bar_desc)) },
                        trailingContent = {
                            Switch(
                                checked = listenTogetherInTopBar,
                                onCheckedChange = onListenTogetherInTopBarChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (listenTogetherInTopBar) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onListenTogetherInTopBarChange(!listenTogetherInTopBar) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.grid_view),
                        title = { Text(stringResource(R.string.grid_cell_size)) },
                        description = {
                            Text(
                                when (gridItemSize) {
                                    GridItemSize.BIG -> stringResource(R.string.big)
                                    GridItemSize.SMALL -> stringResource(R.string.small)
                                },
                            )
                        },
                        onClick = { showGridSizeDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.grid_view),
                        title = { Text(stringResource(R.string.display_density)) },
                        description = {
                            Text(DensityScale.fromValue(densityScale).label)
                        },
                        onClick = { showDensityScaleDialog = true },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.auto_playlists),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.favorite),
                        title = { Text(stringResource(R.string.show_liked_playlist)) },
                        trailingContent = {
                            Switch(
                                checked = showLikedPlaylist,
                                onCheckedChange = onShowLikedPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showLikedPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowLikedPlaylistChange(!showLikedPlaylist) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.offline),
                        title = { Text(stringResource(R.string.show_downloaded_playlist)) },
                        trailingContent = {
                            Switch(
                                checked = showDownloadedPlaylist,
                                onCheckedChange = onShowDownloadedPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showDownloadedPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowDownloadedPlaylistChange(!showDownloadedPlaylist) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.trending_up),
                        title = { Text(stringResource(R.string.show_top_playlist)) },
                        trailingContent = {
                            Switch(
                                checked = showTopPlaylist,
                                onCheckedChange = onShowTopPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showTopPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowTopPlaylistChange(!showTopPlaylist) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.cached),
                        title = { Text(stringResource(R.string.show_cached_playlist)) },
                        trailingContent = {
                            Switch(
                                checked = showCachedPlaylist,
                                onCheckedChange = onShowCachedPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showCachedPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowCachedPlaylistChange(!showCachedPlaylist) },
                    ),
                ),
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (showExperimentalLyricsBetaDialog) {
            DefaultDialog(
                onDismiss = { showExperimentalLyricsBetaDialog = false },
                title = { Text(stringResource(R.string.experimental_lyrics_beta_title)) },
                buttons = {
                    TextButton(onClick = { showExperimentalLyricsBetaDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = {
                        showExperimentalLyricsBetaDialog = false
                        onExperimentalLyricsChange(true)
                    }) {
                        Text(stringResource(R.string.enable))
                    }
                },
            ) {
                Text(stringResource(R.string.experimental_lyrics_beta_message))
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.appearance)) },
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

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

@Composable
private fun sliderStyleName(style: SliderStyle): String =
    when (style) {
        SliderStyle.SLIM -> stringResource(R.string.slider_style_classic)
        SliderStyle.DEFAULT -> stringResource(R.string.slider_style_old)
    }
