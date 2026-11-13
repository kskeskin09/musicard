/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.framework.CastContext

private const val CastDialogTag = "cast_dialog"

/**
 * Opens the Cast device picker (or, while casting, the controls of the connected device), the
 * same dialog the cast button opens. Used by the player menu's Cast item.
 */
fun showCastDialog(context: Context) {
    val activity = context.findFragmentActivity() ?: return
    val selector =
        runCatching { CastContext.getSharedInstance(context).mergedSelector }.getOrNull() ?: return
    val fragments = activity.supportFragmentManager
    if (fragments.findFragmentByTag(CastDialogTag) != null) return
    val route = MediaRouter.getInstance(context).selectedRoute
    if (!route.isDefaultOrBluetooth && route.matchesSelector(selector)) {
        CastControllerDialogFragment().show(fragments, CastDialogTag)
    } else {
        CastChooserDialogFragment().apply { routeSelector = selector }.show(fragments, CastDialogTag)
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? =
    when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }
