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
import com.metrolist.music.playback.sponsorblock.SponsorBlockMusicKey
import com.metrolist.music.playback.sponsorblock.SponsorBlockPodcastsKey
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.ui.component.ThemedSwitch as Switch

@Composable
fun SponsorBlockSettingsGroup() {
    val (podcasts, onPodcastsChange) = rememberPreference(SponsorBlockPodcastsKey, defaultValue = false)
    val (music, onMusicChange) = rememberPreference(SponsorBlockMusicKey, defaultValue = false)

    @Composable
    fun toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            thumbContent = {
                Icon(
                    painter = painterResource(if (checked) R.drawable.check else R.drawable.close),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            },
        )
    }

    Material3SettingsGroup(
        title = stringResource(R.string.sponsorblock),
        items = listOf(
            Material3SettingsItem(
                icon = painterResource(R.drawable.mic),
                title = { Text(stringResource(R.string.sponsorblock_podcasts)) },
                description = { Text(stringResource(R.string.sponsorblock_podcasts_desc)) },
                trailingContent = { toggle(podcasts, onPodcastsChange) },
                onClick = { onPodcastsChange(!podcasts) },
            ),
            Material3SettingsItem(
                icon = painterResource(R.drawable.fast_forward),
                title = { Text(stringResource(R.string.sponsorblock_music)) },
                description = { Text(stringResource(R.string.sponsorblock_music_desc)) },
                trailingContent = { toggle(music, onMusicChange) },
                onClick = { onMusicChange(!music) },
            ),
        ),
    )
}
