/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** A row of [ReorderableToggleList]; [enabled] null means the row has no switch. */
data class ReorderableToggleItem(
    val id: String,
    val title: String,
    val icon: Painter? = null,
    val enabled: Boolean? = null,
)

private val RowHeight = 60.dp
private val RowGap = 6.dp

/**
 * Rounded rows that are moved by holding one and dragging it (no handles); while a row is held,
 * small arrows show where it can still go. Tapping a row flips its switch.
 * The list has a fixed height: all rows, or at most [maxHeight] (then it scrolls).
 */
@Composable
fun ReorderableToggleList(
    items: List<ReorderableToggleItem>,
    onReorder: (List<String>) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp? = null,
    rowColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val haptic = LocalHapticFeedback.current
    val rows = remember { mutableStateListOf<ReorderableToggleItem>() }
    var dragged by remember { mutableStateOf(false) }
    val latestOnReorder by rememberUpdatedState(onReorder)
    // Follows the given rows, except while one is being moved.
    LaunchedEffect(items) {
        if (!dragged) {
            rows.clear()
            rows.addAll(items)
        }
    }
    val listState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            rows.add(to.index, rows.removeAt(from.index))
            dragged = true
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (!reorderState.isAnyItemDragging && dragged) {
            latestOnReorder(rows.map { it.id })
            dragged = false
        }
    }

    val fullHeight = RowHeight * rows.size + RowGap * (rows.size - 1).coerceAtLeast(0)
    val height = maxHeight?.let { minOf(it, fullHeight) } ?: fullHeight
    LazyColumn(
        state = listState,
        userScrollEnabled = maxHeight != null && fullHeight > maxHeight,
        verticalArrangement = Arrangement.spacedBy(RowGap),
        modifier = modifier.fillMaxWidth().height(height),
    ) {
        items(rows, key = { it.id }) { item ->
            ReorderableItem(reorderState, key = item.id) { isDragging ->
                val index = rows.indexOfFirst { it.id == item.id }
                val background by animateColorAsState(
                    if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else rowColor,
                    label = "rowBackground",
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(RowHeight)
                            .clip(RoundedCornerShape(16.dp))
                            .background(background)
                            .longPressDraggableHandle(
                                onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                            ).clickable(enabled = item.enabled != null) {
                                onToggle(item.id, !(item.enabled ?: false))
                            }.padding(horizontal = 16.dp),
                ) {
                    item.icon?.let {
                        Icon(
                            painter = it,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isDragging) {
                        // Small arrows toward where the row can still go.
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_upward),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (index > 0) 0.9f else 0f),
                                modifier = Modifier.size(16.dp),
                            )
                            Icon(
                                painter = painterResource(R.drawable.arrow_downward),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (index in 0 until rows.lastIndex) 0.9f else 0f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    item.enabled?.let { on ->
                        Switch(
                            checked = on,
                            onCheckedChange = { onToggle(item.id, it) },
                            thumbContent = {
                                Icon(
                                    painter = painterResource(if (on) R.drawable.check else R.drawable.close),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
