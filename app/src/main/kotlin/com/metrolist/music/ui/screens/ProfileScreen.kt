/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.ListenTogetherUsernameKey
import com.metrolist.music.constants.PauseListenHistoryKey
import com.metrolist.music.constants.ProfileImageOffsetXKey
import com.metrolist.music.constants.ProfileImageOffsetYKey
import com.metrolist.music.constants.ProfileImageUrlKey
import com.metrolist.music.constants.ProfileImageZoomKey
import com.metrolist.music.ui.component.CroppedProfileImage
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.ProfileCrop
import com.metrolist.music.ui.component.ProfilePictureDialog
import com.metrolist.music.ui.screens.settings.AccountSettings
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.HomeViewModel

/**
 * The Profile tab: the profile picture and name, then history, stats and Listen Together, then
 * everything that used to be in the account window (in the same order).
 */
@Composable
fun ProfileScreen(
    navController: NavController,
    latestVersionName: String,
) {
    val database = LocalDatabase.current
    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()
    val accountName by homeViewModel.accountName.collectAsStateWithLifecycle()

    val (profileImageUrl, setProfileImageUrl) = rememberPreference(ProfileImageUrlKey, defaultValue = "")
    val (profileZoom, setProfileZoom) = rememberPreference(ProfileImageZoomKey, defaultValue = 1f)
    val (profileOffsetX, setProfileOffsetX) = rememberPreference(ProfileImageOffsetXKey, defaultValue = 0f)
    val (profileOffsetY, setProfileOffsetY) = rememberPreference(ProfileImageOffsetYKey, defaultValue = 0f)
    val (profileName, setProfileName) = rememberPreference(ListenTogetherUsernameKey, defaultValue = "")
    val profileCrop = ProfileCrop(profileZoom, profileOffsetX, profileOffsetY)

    val pauseListenHistory by rememberPreference(PauseListenHistoryKey, defaultValue = false)
    val eventCount by database.eventCount().collectAsStateWithLifecycle(initialValue = 0)
    val showHistory = !(pauseListenHistory && eventCount == 0)

    var showPictureDialog by remember { mutableStateOf(false) }
    val listeningDays by database.listeningDays().collectAsStateWithLifecycle(initialValue = emptyList())
    val streak = remember(listeningDays) { com.metrolist.music.utils.ListeningStreak.from(listeningDays) }
    val avatarUrl = profileImageUrl.takeIf { it.isNotBlank() } ?: accountImageUrl

    // Opening another page changes the bottom space (the navigation bar and mini player go away)
    // while this page is still sliding out. That made the page shorter, so a page scrolled to
    // the bottom jumped back up for a moment. The spacing is held as it was until the page is gone.
    val liveInsets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val onThisPage = navController.currentBackStackEntryAsState().value?.destination?.route == Screens.Profile.route
    val heldPadding = remember { arrayOf(liveInsets.calculateTopPadding(), liveInsets.calculateBottomPadding()) }
    if (onThisPage) {
        heldPadding[0] = liveInsets.calculateTopPadding()
        heldPadding[1] = liveInsets.calculateBottomPadding()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                // Kept outside the page, so it stays where it was while the page animates out
                // (it used to jump back to the top for a moment when opening Settings).
                .verticalScroll(com.metrolist.music.ui.utils.TabStates.scroll("profile/page"))
                .padding(
                    start = liveInsets.calculateStartPadding(layoutDirection),
                    end = liveInsets.calculateEndPadding(layoutDirection),
                    top = heldPadding[0],
                    bottom = heldPadding[1],
                ),
    ) {
        Spacer(Modifier.height(12.dp))
        // The profile card: picture on the left, the name and the listening streak beside it.
        // Holding it opens the profile editor.
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .combinedClickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            showPictureDialog = true
                        },
                    ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                if (avatarUrl != null) {
                    CroppedProfileImage(
                        url = avatarUrl,
                        crop = if (profileImageUrl.isNotBlank()) profileCrop else ProfileCrop(),
                        contentDescription = stringResource(R.string.profile_picture),
                        modifier = Modifier.size(112.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = stringResource(R.string.profile_picture),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp),
                    )
                }
            }
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profileName.ifBlank { accountName.ifBlank { stringResource(R.string.profile_tab) } },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (streak.current > 0) {
                    // White (black in the light theme) once a song was played today; gray while
                    // today still needs a play to keep the streak going.
                    val streakColor =
                        if (streak.listenedToday) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.streak_flame),
                            contentDescription = null,
                            tint = streakColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = androidx.compose.ui.res.pluralStringResource(R.plurals.streak_days, streak.current, streak.current),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = streakColor,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // History, stats and Listen Together (they were buttons in the header).
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Material3SettingsGroup(
                items =
                    buildList {
                        if (showHistory) {
                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.history),
                                    title = { Text(stringResource(R.string.history)) },
                                    onClick = { navController.navigate("history") },
                                ),
                            )
                        }
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.stats),
                                title = { Text(stringResource(R.string.stats)) },
                                onClick = { navController.navigate("stats") },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.group_outlined),
                                title = { Text(stringResource(R.string.together)) },
                                onClick = { navController.navigate("listen_together_from_topbar") },
                            ),
                        )
                    },
            )
        }
        Spacer(Modifier.height(4.dp))

        // Everything from the account window, in the same order.
        AccountSettings(
            navController = navController,
            onClose = {},
            latestVersionName = latestVersionName,
            embedded = true,
        )
        Spacer(Modifier.height(16.dp))
    }

    if (showPictureDialog) {
        ProfilePictureDialog(
            initialUrl = profileImageUrl,
            initialCrop = profileCrop,
            initialName = profileName,
            onDismiss = { showPictureDialog = false },
            onSave = { url, crop, name ->
                setProfileName(name)
                setProfileImageUrl(url.trim())
                setProfileZoom(crop.zoom)
                setProfileOffsetX(crop.offsetX)
                setProfileOffsetY(crop.offsetY)
                showPictureDialog = false
            },
        )
    }
}
