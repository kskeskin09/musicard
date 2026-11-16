/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ArtistSongSortDescendingKey
import com.metrolist.music.constants.ArtistSongSortType
import com.metrolist.music.constants.ArtistSongSortTypeKey
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.ListSortRow
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.ArtistSongsViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistSongsScreen(
    navController: NavController,
    viewModel: ArtistSongsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val queueAllSongsStr = stringResource(R.string.queue_all_songs)
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val (sortType, onSortTypeChange) =
        rememberEnumPreference(
            ArtistSongSortTypeKey,
            ArtistSongSortType.CREATE_DATE,
        )
    val (sortDescending, onSortDescendingChange) =
        rememberPreference(
            ArtistSongSortDescendingKey,
            true,
        )
    // Orders the row no longer offers (Most played, Z to A) fall back to one it does.
    androidx.compose.runtime.LaunchedEffect(sortType, sortDescending) {
        if (sortType == ArtistSongSortType.PLAY_TIME) {
            onSortTypeChange(ArtistSongSortType.CREATE_DATE)
            onSortDescendingChange(true)
        } else if (sortType == ArtistSongSortType.NAME && sortDescending) {
            onSortDescendingChange(false)
        }
    }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val artist by viewModel.artist.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item(
                key = "header",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    // The playlist pages' sort row, without Custom and Date modified. Date added is
                    // when the song was added to the library.
                    ArtistSongSortRow(
                        sortType = sortType,
                        sortDescending = sortDescending,
                        onSortTypeChange = onSortTypeChange,
                        onSortDescendingChange = onSortDescendingChange,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            itemsIndexed(
                items = songs,
                key = { index, item -> "${item.id}_$index" },
            ) { index, song ->
                SongListItem(
                    song = song,
                    showInLibraryIcon = true,
                    isActive = song.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = song,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = queueAllSongsStr,
                                                items = songs.map { it.toMediaItem() },
                                                startIndex = index,
                                            ),
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ).animateItem(),
                )
            }
        }

        TopAppBar(
            title = { Text(artist?.artist?.name.orEmpty()) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
        )

        HideOnScrollFAB(
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = artist?.artist?.name,
                        items = songs.shuffled().map { it.toMediaItem() },
                    ),
                )
            },
        )
    }
}

private enum class ArtistSongSortChoice(val type: ArtistSongSortType, val descending: Boolean, val labelRes: Int) {
    NAME(ArtistSongSortType.NAME, false, R.string.library_sort_name),
    ADDED_NEWEST(ArtistSongSortType.CREATE_DATE, true, R.string.library_sort_added_newest),
    ADDED_OLDEST(ArtistSongSortType.CREATE_DATE, false, R.string.library_sort_added_oldest),
}

@Composable
private fun ArtistSongSortRow(
    sortType: ArtistSongSortType,
    sortDescending: Boolean,
    onSortTypeChange: (ArtistSongSortType) -> Unit,
    onSortDescendingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected =
        when {
            sortType == ArtistSongSortType.NAME -> ArtistSongSortChoice.NAME
            sortType == ArtistSongSortType.CREATE_DATE && !sortDescending -> ArtistSongSortChoice.ADDED_OLDEST
            else -> ArtistSongSortChoice.ADDED_NEWEST
        }
    ListSortRow(
        options = ArtistSongSortChoice.entries,
        selected = selected,
        label = { stringResource(it.labelRes) },
        onSelect = {
            onSortTypeChange(it.type)
            onSortDescendingChange(it.descending)
        },
        modifier = modifier,
    )
}
