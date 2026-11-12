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

    /** How far each tab's header was scrolled away. */
    val headerCollapse = HashMap<String, Float>()

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
