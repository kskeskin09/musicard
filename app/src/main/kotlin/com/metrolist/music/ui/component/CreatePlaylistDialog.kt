/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalSyncUtils
import com.metrolist.music.R
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.extensions.isSyncEnabled
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
    onPlaylistCreated: ((String) -> Unit)? = null,
) {
    val syncUtils = LocalSyncUtils.current
    val coroutineScope = rememberCoroutineScope()
    var syncedPlaylist by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isSignedIn = innerTubeCookie.isNotEmpty()

    val notLoggedInYoutubeStr = stringResource(R.string.not_logged_in_youtube)
    val syncDisabledStr = stringResource(R.string.sync_disabled)
    val playlistCreatedLocallyStr = stringResource(R.string.playlist_created_locally)

    PlaylistEditorSheet(
        onDismiss = onDismiss,
        initialName = initialTextFieldValue ?: "",
        isNew = true,
        showSync = allowSyncing && isSignedIn,
        onSyncChangeRequest = { wanted, answer ->
            if (!wanted) {
                answer(true)
            } else {
                coroutineScope.launch {
                    val isYtmSyncEnabled = withContext(Dispatchers.IO) { context.isSyncEnabled() }
                    when {
                        !isSignedIn -> {
                            Toast.makeText(context, notLoggedInYoutubeStr, Toast.LENGTH_SHORT).show()
                            answer(false)
                        }
                        !isYtmSyncEnabled -> {
                            Toast.makeText(context, syncDisabledStr, Toast.LENGTH_SHORT).show()
                            answer(false)
                        }
                        else -> answer(true)
                    }
                }
            }
        },
        onSubmit = { playlistName, description, coverUrl, sync ->
            syncedPlaylist = sync
            syncUtils.createPlaylist(
                playlist =
                    PlaylistEntity(
                        name = playlistName,
                        bookmarkedAt = LocalDateTime.now(),
                        isEditable = true,
                        thumbnailUrl = coverUrl,
                    ),
                syncWithYouTube = sync,
            ) { playlistId, remoteCreated ->
                com.metrolist.music.utils.PlaylistDescriptions.set(context, playlistId, description)
                if (sync && !remoteCreated) {
                    Toast.makeText(context, playlistCreatedLocallyStr, Toast.LENGTH_LONG).show()
                }
                onPlaylistCreated?.invoke(playlistId)
            }
        },
    )
}
