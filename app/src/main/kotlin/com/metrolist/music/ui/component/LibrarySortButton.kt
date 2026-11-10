/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.LibrarySortOption

val LibrarySortOption.labelRes: Int
    get() =
        when (this) {
            LibrarySortOption.CUSTOM -> R.string.library_sort_custom
            LibrarySortOption.NAME -> R.string.library_sort_name
            LibrarySortOption.ADDED_NEWEST -> R.string.library_sort_added_newest
            LibrarySortOption.ADDED_OLDEST -> R.string.library_sort_added_oldest
            LibrarySortOption.MODIFIED_NEWEST -> R.string.library_sort_modified_newest
            LibrarySortOption.MODIFIED_OLDEST -> R.string.library_sort_modified_oldest
        }

/**
 * Round sort button. Opens a menu with the six sort options (Custom first); the chosen one is
 * marked with a check.
 */
@Composable
fun LibrarySortButton(
    selected: LibrarySortOption,
    onSelect: (LibrarySortOption) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(containerColor)
                    .clickable { expanded = true },
        ) {
            Icon(
                painter = painterResource(R.drawable.sort),
                contentDescription = stringResource(R.string.library_sort),
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LibrarySortOption.entries.forEach { option ->
                val isSelected = option == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(option.labelRes),
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
}
