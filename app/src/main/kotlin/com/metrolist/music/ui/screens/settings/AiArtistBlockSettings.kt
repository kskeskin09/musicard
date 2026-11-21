/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.metrolist.music.R
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.utils.aiblock.BlockAiArtistsKey
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.ui.component.ThemedSwitch as Switch

/** "Hide AI-generated artists" row for the content filters group. */
@Composable
fun aiArtistBlockSettingsItem(): Material3SettingsItem {
    val (blockAi, onBlockAiChange) = rememberPreference(BlockAiArtistsKey, defaultValue = false)
    return Material3SettingsItem(
        icon = painterResource(R.drawable.ai_block),
        title = { Text(stringResource(R.string.block_ai_artists)) },
        description = { Text(stringResource(R.string.block_ai_artists_desc)) },
        trailingContent = {
            Switch(
                checked = blockAi,
                onCheckedChange = onBlockAiChange,
                thumbContent = {
                    Icon(
                        painter = painterResource(if (blockAi) R.drawable.check else R.drawable.close),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
        },
        onClick = { onBlockAiChange(!blockAi) },
    )
}
