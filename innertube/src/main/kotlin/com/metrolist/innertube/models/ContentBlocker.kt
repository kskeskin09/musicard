/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.innertube.models

/**
 * App-wide hide rule for online results (e.g. known AI-generated artists). It runs
 * inside every `filterExplicit` call, so lists that are filtered for explicit content
 * also drop these items, whether "hide explicit" is on or not. Null when inactive.
 */
object ContentBlocker {
    @Volatile
    var isBlocked: ((YTItem) -> Boolean)? = null

    val active: Boolean get() = isBlocked != null
}
