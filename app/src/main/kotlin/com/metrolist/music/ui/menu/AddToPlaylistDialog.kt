/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.constants.AddToPlaylistPosition
import com.metrolist.music.constants.AddToPlaylistPositionKey
import com.metrolist.music.constants.AddToPlaylistSortDescendingKey
import com.metrolist.music.constants.AddToPlaylistSortTypeKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.PlaylistSortType
import com.metrolist.music.db.DatabaseDao
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.CreatePlaylistDialog
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.ListItem
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.ui.component.SortHeader
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.PlaylistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.withContext
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.FilterChip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.FilterChipDefaults
import com.metrolist.music.LocalSyncUtils

@Composable
fun AddToPlaylistDialog(
    isVisible: Boolean,
    allowSyncing: Boolean = true,
    initialTextFieldValue: String? = null,
    onGetSong: suspend () -> List<String>, // list of song ids. Songs should be inserted to database in this function.
    onGetSongIds: (suspend () -> List<String>)? = null,
    onDismiss: () -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val database = LocalDatabase.current
    val syncUtils = LocalSyncUtils.current
    val coroutineScope = rememberCoroutineScope()
    val (addToPlaylistPosition) = rememberEnumPreference(
        AddToPlaylistPositionKey,
        AddToPlaylistPosition.BEGINNING,
    )
    // Playlists come in the same order as in the library (pinned first, then its sort).
    val (librarySort) = rememberEnumPreference(com.metrolist.music.constants.LibrarySortKey, com.metrolist.music.constants.LibrarySortOption.ADDED_NEWEST)
    val (libraryCustomOrder) = rememberPreference(com.metrolist.music.constants.LibraryCustomOrderKey, "")
    val (libraryPinned) = rememberPreference(com.metrolist.music.constants.LibraryPinnedKey, "")
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    // The app's own lists (top, weekly / monthly most) can't take songs, so they aren't offered.
    val playlists =
        remember(allPlaylists, librarySort, libraryCustomOrder, libraryPinned) {
            com.metrolist.music.ui.screens.library.libraryOrder(
                allPlaylists.filter {
                    it.id != com.metrolist.music.db.entities.PlaylistEntity.WEEKLY_MOST_PLAYLIST_ID &&
                        it.id != com.metrolist.music.db.entities.PlaylistEntity.MONTHLY_MOST_PLAYLIST_ID
                },
                librarySort,
                libraryCustomOrder.split('\n').filter { it.isNotBlank() },
                libraryPinned.split('\n').filter { it.isNotBlank() },
            )
        }
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    var showCreatePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showDuplicateDialog by remember {
        mutableStateOf(false)
    }
    var selectedPlaylist by remember {
        mutableStateOf<Playlist?>(null)
    }
    var songIds by remember {
        mutableStateOf<List<String>?>(null)
    }
    var duplicates by remember {
        mutableStateOf(emptyList<String>())
    }
    var playlistsContainingSong by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    suspend fun addSongsAndSync(targetPlaylist: Playlist, ids: List<String>) {
        database.addSongsToPlaylist(
            targetPlaylist,
            ids.map { it to null },
            prepend = addToPlaylistPosition.prepend,
        )
        targetPlaylist.playlist.browseId?.let { browseId ->
            syncUtils.scheduleAddToPlaylist(browseId, targetPlaylist.id, ids)
        }
    }

    LaunchedEffect(isVisible, playlists.isEmpty()) {
        if (!isVisible || playlists.isEmpty()) return@LaunchedEffect
        if (songIds != null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            songIds = onGetSongIds?.invoke() ?: onGetSong()
        }
    }
    LaunchedEffect(isVisible, songIds, playlists) {
        if (!isVisible) {
            songIds = null
            playlistsContainingSong = emptySet()
            return@LaunchedEffect
        }
        val ids = songIds ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            playlistsContainingSong = playlists
                .filter { database.playlistDuplicatesBatched(it.id, ids).isNotEmpty() }
                .map { it.id }
                .toSet()
        }
    }

    // What the user ticked or unticked; nothing changes until Save.
    var selection by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(isVisible) { if (!isVisible) selection = null }
    val selected = selection ?: playlistsContainingSong

    suspend fun removeSongs(targetPlaylist: Playlist, ids: List<String>) {
        val browseId = targetPlaylist.playlist.browseId
        ids.distinct().forEach { songId ->
            while (true) {
                val map = database.playlistSongMaps(songId).firstOrNull { it.playlistId == targetPlaylist.id } ?: break
                database.transaction {
                    move(map.playlistId, map.position, Int.MAX_VALUE)
                    delete(map.copy(position = Int.MAX_VALUE))
                }
                if (browseId != null) {
                    val setVideoId = map.setVideoId
                    syncUtils.scheduleRemoveFromPlaylist(browseId, songId, targetPlaylist.id) { setVideoId }
                }
            }
        }
    }

    if (isVisible) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .padding(horizontal = 24.dp, vertical = 48.dp)
                        .fillMaxWidth(),
            ) {
                androidx.compose.foundation.layout.Column(Modifier.padding(top = 22.dp, bottom = 16.dp)) {
                    Text(
                        text = stringResource(R.string.add_to_playlist),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    androidx.compose.material3.Surface(
                        onClick = { showCreatePlaylistDialog = true },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .height(48.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp).size(20.dp),
                            )
                            Text(
                                text = stringResource(R.string.create_playlist),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            val checked = playlist.id in selected
                            PlaylistListItem(
                                playlist = playlist,
                                trailingContent = { SelectCircle(checked) },
                                modifier =
                                    Modifier
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            selection = if (checked) selected - playlist.id else selected + playlist.id
                                        },
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    ) {
                        androidx.compose.material3.Surface(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.cancel), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        androidx.compose.material3.Surface(
                            onClick = {
                                val added = playlists.filter { it.id in selected && it.id !in playlistsContainingSong }
                                val removed = playlists.filter { it.id !in selected && it.id in playlistsContainingSong }
                                coroutineScope.launch(Dispatchers.IO) {
                                    val ids = songIds ?: onGetSong()
                                    if (added.isNotEmpty()) {
                                        // Songs are written to the database by onGetSong before they are added.
                                        val toAdd = onGetSong()
                                        added.forEach { target ->
                                            val already = database.playlistDuplicatesBatched(target.id, toAdd)
                                            addSongsAndSync(target, toAdd.filter { it !in already })
                                        }
                                    }
                                    removed.forEach { removeSongs(it, ids) }
                                    withContext(Dispatchers.Main) { onDismiss() }
                                }
                            },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.save), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    // duplicate songs warning
        if (showDuplicateDialog) {
            DefaultDialog(
                title = { Text(stringResource(R.string.duplicates)) },
                buttons = {
                    TextButton(
                        onClick = {
                            showDuplicateDialog = false
                            coroutineScope.launch(Dispatchers.IO) {
                                addSongsAndSync(
                                    selectedPlaylist!!,
                                    songIds!!.filter { !duplicates.contains(it) }
                                )
                                withContext(Dispatchers.Main) { onDismiss() }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.skip_duplicates))
                    }

                    TextButton(
                        onClick = {
                            showDuplicateDialog = false
                            coroutineScope.launch(Dispatchers.IO) {
                                addSongsAndSync(selectedPlaylist!!, songIds!!)
                                withContext(Dispatchers.Main) { onDismiss() }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.add_anyway))
                    }

                    TextButton(
                        onClick = {
                            showDuplicateDialog = false
                        }
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
                onDismiss = {
                    showDuplicateDialog = false
                }
            ) {
                Text(
                    text = if (duplicates.size == 1) {
                        stringResource(R.string.duplicates_description_single)
                    } else {
                        stringResource(R.string.duplicates_description_multiple, duplicates.size)
                    },
                    textAlign = TextAlign.Start,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }
}

internal const val MAX_PLAYLIST_DUPLICATES_BATCH_SIZE = 500

internal suspend fun DatabaseDao.playlistDuplicatesBatched(
    playlistId: String,
    songIds: List<String>,
): List<String> =
    if (songIds.size <= MAX_PLAYLIST_DUPLICATES_BATCH_SIZE) {
        playlistDuplicates(playlistId, songIds)
    } else {
        songIds
            .distinct()
            .chunked(MAX_PLAYLIST_DUPLICATES_BATCH_SIZE)
            .flatMap { playlistDuplicates(playlistId, it) }
    }

/** A round tick box: filled with a check when [checked]. */
@Composable
private fun SelectCircle(checked: Boolean) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val fill by animateColorAsState(if (checked) onSurface else Color.Transparent, label = "selectFill")
    androidx.compose.foundation.layout.Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .padding(end = 8.dp)
                .size(24.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(fill)
                .then(
                    if (checked) {
                        Modifier
                    } else {
                        Modifier.border(2.dp, onSurface.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                    },
                ),
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Picks playlists to export together (the same picker as adding a song to a playlist) and saves
 * each one as an [format] file ("m3u" or "csv") in Documents/Musicard.
 */
@Composable
fun ExportPlaylistsDialog(
    format: String,
    onDismiss: () -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val (librarySort) = rememberEnumPreference(com.metrolist.music.constants.LibrarySortKey, com.metrolist.music.constants.LibrarySortOption.ADDED_NEWEST)
    val (libraryCustomOrder) = rememberPreference(com.metrolist.music.constants.LibraryCustomOrderKey, "")
    val (libraryPinned) = rememberPreference(com.metrolist.music.constants.LibraryPinnedKey, "")
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val playlists =
        remember(allPlaylists, librarySort, libraryCustomOrder, libraryPinned) {
            com.metrolist.music.ui.screens.library.libraryOrder(
                allPlaylists,
                librarySort,
                libraryCustomOrder.split('\n').filter { it.isNotBlank() },
                libraryPinned.split('\n').filter { it.isNotBlank() },
            )
        }
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var exporting by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .padding(horizontal = 24.dp, vertical = 48.dp)
                    .fillMaxWidth(),
        ) {
            androidx.compose.foundation.layout.Column(Modifier.padding(top = 22.dp, bottom = 16.dp)) {
                Text(
                    text = stringResource(if (format == "csv") R.string.export_playlists_csv else R.string.export_playlists_m3u),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                // Select all / none.
                val allSelected = playlists.isNotEmpty() && selected.size == playlists.size
                Text(
                    text = stringResource(if (allSelected) R.string.export_select_none else R.string.export_select_all),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { selected = if (allSelected) emptySet() else playlists.map { it.id }.toSet() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        val checked = playlist.id in selected
                        PlaylistListItem(
                            playlist = playlist,
                            trailingContent = { SelectCircle(checked) },
                            modifier =
                                Modifier
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selected = if (checked) selected - playlist.id else selected + playlist.id },
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
                ) {
                    androidx.compose.material3.Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.cancel), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    androidx.compose.material3.Surface(
                        onClick = {
                            if (exporting || selected.isEmpty()) return@Surface
                            exporting = true
                            val targets = playlists.filter { it.id in selected }
                            coroutineScope.launch(Dispatchers.IO) {
                                val mime = if (format == "csv") "text/csv" else "audio/x-mpegurl"
                                var saved = 0
                                targets.forEach { playlist ->
                                    val songs = database.playlistSongsBlocking(playlist.id)
                                    val file =
                                        if (format == "csv") {
                                            com.metrolist.music.utils.PlaylistExporter.exportPlaylistAsCSV(context, playlist.playlist.name, songs)
                                        } else {
                                            com.metrolist.music.utils.PlaylistExporter.exportPlaylistAsM3U(context, playlist.playlist.name, songs)
                                        }
                                    file.getOrNull()?.let { f ->
                                        if (com.metrolist.music.utils.saveToPublicDocuments(context, f, mime, "Musicard").isSuccess) saved++
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    android.widget.Toast
                                        .makeText(
                                            context,
                                            if (saved > 0) {
                                                context.resources.getQuantityString(R.plurals.export_playlists_done, saved, saved)
                                            } else {
                                                context.getString(R.string.export_failed)
                                            },
                                            android.widget.Toast.LENGTH_LONG,
                                        ).show()
                                    onDismiss()
                                }
                            }
                        },
                        enabled = selected.isNotEmpty() && !exporting,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f).height(48.dp).graphicsLayer { alpha = if (selected.isEmpty()) 0.4f else 1f },
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Text(
                                text =
                                    if (selected.isEmpty()) {
                                        stringResource(R.string.export_action)
                                    } else {
                                        stringResource(R.string.export_action_count, selected.size)
                                    },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
