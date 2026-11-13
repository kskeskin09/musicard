/**
 * Musicard (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.SongSortType

/**
 * The sort row used on playlist pages: a round sort button on the left that opens the options
 * (the chosen one has a check), and the chosen option's name next to it.
 */
@Composable
fun <T> ListSortRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .clickable { expanded = true },
            ) {
                Icon(
                    painter = painterResource(R.drawable.sort),
                    contentDescription = stringResource(R.string.library_sort),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    val isSelected = option == selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label(option),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label(selected),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Song orders for the app's own lists (Liked, Downloaded, Cached, …). */
enum class SongSortChoice(val type: SongSortType, val descending: Boolean) {
    ADDED_NEWEST(SongSortType.CREATE_DATE, true),
    ADDED_OLDEST(SongSortType.CREATE_DATE, false),
    NAME(SongSortType.NAME, false),
    ARTIST(SongSortType.ARTIST, false),
    MOST_PLAYED(SongSortType.PLAY_TIME, true),
    ;

    companion object {
        fun from(type: SongSortType, descending: Boolean): SongSortChoice =
            when (type) {
                SongSortType.CREATE_DATE -> if (descending) ADDED_NEWEST else ADDED_OLDEST
                SongSortType.NAME -> NAME
                SongSortType.ARTIST -> ARTIST
                SongSortType.PLAY_TIME -> MOST_PLAYED
            }
    }
}

@Composable
fun SongSortChoice.label(): String =
    stringResource(
        when (this) {
            SongSortChoice.ADDED_NEWEST -> R.string.library_sort_added_newest
            SongSortChoice.ADDED_OLDEST -> R.string.library_sort_added_oldest
            SongSortChoice.NAME -> R.string.library_sort_name
            SongSortChoice.ARTIST -> R.string.sort_by_artist
            SongSortChoice.MOST_PLAYED -> R.string.sort_most_played
        },
    )

/** [ListSortRow] for [SongSortType] lists. */
@Composable
fun SongSortRow(
    sortType: SongSortType,
    sortDescending: Boolean,
    onSortTypeChange: (SongSortType) -> Unit,
    onSortDescendingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListSortRow(
        options = SongSortChoice.entries,
        selected = SongSortChoice.from(sortType, sortDescending),
        label = { it.label() },
        onSelect = {
            onSortTypeChange(it.type)
            onSortDescendingChange(it.descending)
        },
        modifier = modifier,
    )
}
