/**
 * Musicard
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.constants

import androidx.datastore.preferences.core.booleanPreferencesKey

// Content > Songs and videos. Off by default: some songs only exist as user uploads.
val HideNonMusicalVideosKey = booleanPreferencesKey("hideNonMusicalVideos")
