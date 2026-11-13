/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState

/**
 * What the main tabs looked like when the user left them (scroll positions, the Library filter,
 * the search text), kept in memory while the app runs. Coming back to a tab shows it as it was;
 * tapping the tab's button again while on it starts the tab over ([reset]).
 *
 * This lives outside the page, so it never shows an empty state while a page animates out.
 */
object TabStates {
    private val lists = HashMap<String, LazyListState>()
    private val grids = HashMap<String, LazyGridState>()
    private val scrolls = HashMap<String, ScrollState>()
    private val values = HashMap<String, MutableState<Any?>>()
    private val resets = HashMap<String, androidx.compose.runtime.MutableIntState>()

    /** How far each tab's header was scrolled away. Observable, so the header follows changes. */
    val headerCollapse = androidx.compose.runtime.mutableStateMapOf<String, Float>()

    fun list(key: String): LazyListState = lists.getOrPut(key) { LazyListState() }

    fun scroll(key: String): ScrollState = scrolls.getOrPut(key) { ScrollState(0) }

    fun grid(key: String): LazyGridState = grids.getOrPut(key) { LazyGridState() }

    @Suppress("UNCHECKED_CAST")
    fun <T> value(
        key: String,
        initial: T,
    ): MutableState<T> = values.getOrPut(key) { mutableStateOf<Any?>(initial) } as MutableState<T>

    /** Changes each time the tab starts over; pages key their content on it. */
    fun resetCount(tab: String): Int = resets.getOrPut(tab) { mutableIntStateOf(0) }.intValue

    /**
     * Scrolls every sideways row of a tab back to its start, leaving the states named in
     * [except] (such as the page's own vertical list) where they are.
     */
    suspend fun scrollRowsToStart(
        tab: String,
        except: Set<String> = emptySet(),
    ) {
        val prefix = "$tab/"
        lists.filterKeys { it.startsWith(prefix) && it !in except }.values.toList().forEach { it.scrollToItem(0) }
        grids.filterKeys { it.startsWith(prefix) && it !in except }.values.toList().forEach { it.scrollToItem(0) }
    }

    /**
     * Forgets every tab's state. Called when the app opens fresh (after it was closed from
     * recents), so the tabs start at the top even if music kept the process alive.
     */
    fun clearAll() {
        lists.clear()
        grids.clear()
        scrolls.clear()
        values.clear()
        headerCollapse.clear()
        resets.values.forEach { it.intValue += 1 }
    }

    /** Starts the tab over: back to the top, filters cleared. */
    fun reset(tab: String) {
        val prefix = "$tab/"
        lists.keys.filter { it.startsWith(prefix) }.forEach { lists.remove(it) }
        grids.keys.filter { it.startsWith(prefix) }.forEach { grids.remove(it) }
        scrolls.keys.filter { it.startsWith(prefix) }.forEach { scrolls.remove(it) }
        values.keys.filter { it.startsWith(prefix) }.forEach { values.remove(it) }
        headerCollapse.remove(tab)
        val counter = resets.getOrPut(tab) { mutableIntStateOf(0) }
        counter.intValue += 1
    }
}

/**
 * Keeps a tab's header in step with its list: the header can never be scrolled away further
 * than the list itself is. When the list is at the top (after a filter change, a jump back to
 * the top, data that loaded again, or a stray scroll from a page that was leaving), the page
 * name and search bar come back instead of leaving an empty gap above the list.
 */
@androidx.compose.runtime.Composable
fun HeaderFollowsList(
    tab: String,
    state: LazyListState,
) {
    androidx.compose.runtime.LaunchedEffect(tab, state) {
        androidx.compose.runtime
            .snapshotFlow {
                val scrolled =
                    if (state.firstVisibleItemIndex == 0) state.firstVisibleItemScrollOffset.toFloat() else Float.MAX_VALUE
                scrolled to (TabStates.headerCollapse[tab] ?: 0f)
            }.collect { (scrolled, collapse) ->
                if (collapse > scrolled) TabStates.headerCollapse[tab] = scrolled
            }
    }
}
