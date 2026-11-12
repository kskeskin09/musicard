/**
 * Musicard (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.NotificationButton
import com.metrolist.music.constants.NotificationButtons
import com.metrolist.music.constants.NotificationButtonsKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference

/**
 * Settings > Appearance > Notification center: a preview of the playback notification whose buttons
 * can be tapped and swapped. Play/pause is fixed; each other button can appear only once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSettings(navController: NavController) {
    val (raw, onRawChange) = rememberPreference(
        NotificationButtonsKey,
        defaultValue = NotificationButtons.serialize(NotificationButtons.Default),
    )
    val buttons = NotificationButtons.parse(raw)
    var editingSlot by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.preview),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
        )

        NotificationPreviewCard(
            buttons = buttons,
            selectedSlot = editingSlot,
            onSlotClick = { editingSlot = it },
        )

        Text(
            text = stringResource(R.string.notification_buttons_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
        )

        Spacer(Modifier.height(12.dp))

        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.replay),
                    title = { Text(stringResource(R.string.notification_restore_defaults)) },
                    enabled = buttons != NotificationButtons.Default,
                    onClick = { onRawChange(NotificationButtons.serialize(NotificationButtons.Default)) },
                ),
            ),
        )
    }

    editingSlot?.let { slot ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { editingSlot = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = stringResource(R.string.notification_choose_button),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Column(Modifier.padding(bottom = 24.dp)) {
                NotificationButton.entries.forEach { option ->
                    val current = buttons[slot] == option
                    val inUse = !current && option in buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (current) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            ).clickable(enabled = !inUse) {
                                if (!current) {
                                    onRawChange(
                                        NotificationButtons.serialize(
                                            buttons.toMutableList().also { it[slot] = option },
                                        ),
                                    )
                                }
                                editingSlot = null
                            }.alpha(if (inUse) 0.38f else 1f)
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                    ) {
                        Icon(
                            painter = painterResource(option.iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(20.dp))
                        Text(
                            text = stringResource(option.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (inUse) {
                            Text(
                                text = stringResource(R.string.notification_button_in_use),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.notification_center)) },
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

@Composable
private fun NotificationPreviewCard(
    buttons: List<NotificationButton>,
    selectedSlot: Int?,
    onSlotClick: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainerHighest)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notification_preview_song),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.notification_preview_artist),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(colors.outlineVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.4f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.onSurface),
            )
        }
        Spacer(Modifier.height(14.dp))

        // Visual order: slot 0, play, slot 1, slot 2, slot 3.
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SlotButton(buttons[0], selectedSlot == 0) { onSlotClick(0) }
            PlayButton()
            SlotButton(buttons[1], selectedSlot == 1) { onSlotClick(1) }
            SlotButton(buttons[2], selectedSlot == 2) { onSlotClick(2) }
            SlotButton(buttons[3], selectedSlot == 3) { onSlotClick(3) }
        }
    }
}

@Composable
private fun SlotButton(
    button: NotificationButton,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) colors.secondaryContainer else colors.surfaceContainerHigh)
                .then(
                    if (selected) {
                        Modifier.border(BorderStroke(2.dp, colors.primary), RoundedCornerShape(16.dp))
                    } else {
                        Modifier
                    },
                ).clickable(onClick = onClick),
        ) {
            Icon(
                painter = painterResource(button.iconRes),
                contentDescription = stringResource(button.labelRes),
                tint = if (selected) colors.primary else colors.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(button.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlayButton() {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(colors.primary),
        ) {
            Icon(
                painter = painterResource(R.drawable.player_play),
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.play),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
