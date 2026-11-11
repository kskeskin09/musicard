/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
    val avatarUrl = profileImageUrl.takeIf { it.isNotBlank() } ?: accountImageUrl

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { showPictureDialog = true },
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
        Spacer(Modifier.height(12.dp))
        Text(
            text = profileName.ifBlank { accountName.ifBlank { stringResource(R.string.profile_tab) } },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            text = stringResource(R.string.profile_change_photo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .padding(top = 2.dp)
                    .clip(CircleShape)
                    .clickable { showPictureDialog = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
        )
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
