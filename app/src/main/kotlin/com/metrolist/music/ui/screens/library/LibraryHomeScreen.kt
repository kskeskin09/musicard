/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.LibraryCustomOrderKey
import com.metrolist.music.constants.LibraryPinnedKey
import com.metrolist.music.constants.LibraryEditLockKey
import com.metrolist.music.constants.LibrarySortKey
import com.metrolist.music.constants.LibrarySortOption
import com.metrolist.music.constants.ShowCachedPlaylistKey
import com.metrolist.music.constants.ShowDownloadedPlaylistKey
import com.metrolist.music.constants.ShowLikedPlaylistKey
import com.metrolist.music.constants.ShowTopPlaylistKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.ui.component.CreatePlaylistDialog
import com.metrolist.music.ui.component.LibrarySortButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.PlaylistMenu
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.Collator
import java.time.LocalDateTime

enum class LibraryKind { PLAYLISTS, ARTISTS, ALBUMS }

private const val MaxPinned = 3

/**
 * Library home: a segmented "Playlists | Artists | Albums" filter (nothing selected shows
 * everything; tapping the selected one again clears it), a round sort button, then one list of
 * the saved playlists, artists and albums. Auto playlists (Liked, My Top, Downloaded, Cached)
 * come first, then up to three items pinned by swiping right, then the rest in the chosen order.
 * In the Custom order the rows can be dragged by their handle.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryHomeScreen(
    navController: NavController,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var kind by rememberSaveable { mutableStateOf<LibraryKind?>(null) }
    var sortOption by rememberEnumPreference(LibrarySortKey, LibrarySortOption.ADDED_NEWEST)
    var customOrderText by rememberPreference(LibraryCustomOrderKey, "")
    var pinnedText by rememberPreference(LibraryPinnedKey, "")
    // Custom order: with the lock open, holding a row picks it up to move it.
    var editLocked by rememberPreference(LibraryEditLockKey, true)
    val pinnedIds = remember(pinnedText) { pinnedText.split('\n').filter { it.isNotBlank() } }
    val customOrder = remember(customOrderText) { customOrderText.split('\n').filter { it.isNotBlank() } }

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)
    val topSize by viewModel.topValue.collectAsStateWithLifecycle(initialValue = "50")

    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onPlaylistCreated = { playlistId ->
                showCreatePlaylistDialog = false
                navController.navigate("local_playlist/$playlistId")
            },
        )
    }

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) { viewModel.syncAllLibrary() }
        }
    }

    val collator = remember { Collator.getInstance().apply { strength = Collator.PRIMARY } }
    val allItems: List<LocalItem> =
        remember(albums, artists, playlists, kind) {
            when (kind) {
                LibraryKind.PLAYLISTS -> playlists
                LibraryKind.ARTISTS -> artists
                LibraryKind.ALBUMS -> albums
                null -> playlists + artists + albums
            }.distinctBy { it.id }
        }
    val sortedItems =
        remember(allItems, sortOption, customOrder) {
            val byAddedNewest = allItems.sortedWith(compareByDescending(nullsFirst()) { it.addedAt() })
            when (sortOption) {
                LibrarySortOption.CUSTOM -> {
                    // Items never ordered by hand (e.g. just added) come first, newest on top.
                    val position = customOrder.withIndex().associate { it.value to it.index }
                    byAddedNewest.sortedBy { position[it.id] ?: -1 }
                }
                LibrarySortOption.NAME -> allItems.sortedWith(compareBy(collator) { it.title })
                LibrarySortOption.ADDED_NEWEST -> byAddedNewest
                LibrarySortOption.ADDED_OLDEST -> allItems.sortedWith(compareBy(nullsLast()) { it.addedAt() })
                LibrarySortOption.MODIFIED_NEWEST -> allItems.sortedWith(compareByDescending(nullsFirst()) { it.modifiedAt() })
                LibrarySortOption.MODIFIED_OLDEST -> allItems.sortedWith(compareBy(nullsLast()) { it.modifiedAt() })
            }
        }
    val pinnedItems =
        remember(sortedItems, pinnedIds) {
            pinnedIds.mapNotNull { id -> sortedItems.firstOrNull { it.id == id } }
        }
    val restItems = remember(sortedItems, pinnedIds) { sortedItems.filterNot { it.id in pinnedIds } }

    // Rows being dragged in the Custom order; written back to the saved order when dropped.
    val dragList = remember { mutableStateListOf<LocalItem>() }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(restItems) {
        if (!dragging) {
            dragList.clear()
            dragList.addAll(restItems)
        }
    }
    val shownRest = if (sortOption == LibrarySortOption.CUSTOM) dragList else restItems

    val lazyListState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(lazyListState = lazyListState) { from, to ->
            val fromIndex = dragList.indexOfFirst { it.id == from.key }
            val toIndex = dragList.indexOfFirst { it.id == to.key }
            if (fromIndex >= 0 && toIndex >= 0) dragList.add(toIndex, dragList.removeAt(fromIndex))
        }
    val saveCustomOrder = {
        val shown = dragList.map { it.id }
        // Keep the place of items hidden by the current filter.
        val hidden = customOrder.filterNot { it in shown }
        customOrderText = (pinnedIds + shown + hidden).distinct().joinToString("\n")
        dragging = false
    }

    val pinLimitText = stringResource(R.string.library_pin_limit)
    val togglePin: (String) -> Unit = { id ->
        if (id in pinnedIds) {
            pinnedText = pinnedIds.filterNot { it == id }.joinToString("\n")
        } else if (pinnedIds.size >= MaxPinned) {
            Toast.makeText(context, pinLimitText, Toast.LENGTH_SHORT).show()
        } else {
            pinnedText = (pinnedIds + id).joinToString("\n")
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val openItem: (LocalItem) -> Unit = { item ->
        when (item) {
            is Playlist ->
                if (!item.playlist.isEditable && item.songCount == 0 && item.playlist.browseId != null) {
                    navController.navigate("online_playlist/${item.playlist.browseId}")
                } else {
                    navController.navigate("local_playlist/${item.id}")
                }
            is Album -> navController.navigate("album/${item.id}")
            is Artist -> navController.navigate("artist/${item.id}")
            else -> Unit
        }
    }
    val showMenu: (LocalItem) -> Unit = { item ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menuState.show {
            when (item) {
                is Playlist -> PlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                is Album -> AlbumMenu(originalAlbum = item, onDismiss = menuState::dismiss)
                is Artist -> ArtistMenu(originalArtist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                else -> Unit
            }
        }
    }

    val showAutoPlaylists = kind == null || kind == LibraryKind.PLAYLISTS
    val autoPlaylists =
        buildList {
            if (showLiked) add(AutoPlaylist("auto_liked", stringResource(R.string.liked), R.drawable.favorite, "auto_playlist/liked", highlighted = true))
            if (showTop) add(AutoPlaylist("auto_top", stringResource(R.string.my_top) + " $topSize", R.drawable.trending_up, "top_playlist/$topSize"))
            if (showDownloaded) add(AutoPlaylist("auto_downloaded", stringResource(R.string.offline), R.drawable.offline, "auto_playlist/downloaded"))
            if (showCached) add(AutoPlaylist("auto_cached", stringResource(R.string.cached_playlist), R.drawable.cached, "cache_playlist/cached"))
        }

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                ),
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "filters") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            // Same width as the search bar above.
                            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
                ) {
                    KindSegments(
                        selected = kind,
                        onSelect = { kind = if (kind == it) null else it },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    LibrarySortButton(selected = sortOption, onSelect = { sortOption = it })
                    if (sortOption == LibrarySortOption.CUSTOM) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                                    .clickable { editLocked = !editLocked },
                        ) {
                            Icon(
                                painter = painterResource(if (editLocked) R.drawable.lock else R.drawable.lock_open),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            if (showAutoPlaylists) {
                items(autoPlaylists, key = { it.key }) { auto ->
                    LibraryRow(
                        title = auto.title,
                        // The app's own lists: just their name, no "Playlist" under it.
                        subtitle = "",
                        cover = { AutoPlaylistCover(auto) },
                        onClick = { navController.navigate(auto.route) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            items(pinnedItems, key = { "pinned_" + it.id }) { item ->
                PinSwipe(pinned = true, onToggle = { togglePin(item.id) }, modifier = Modifier.animateItem()) {
                    LibraryItemRow(item = item, pinned = true, onClick = { openItem(item) }, onLongClick = { showMenu(item) })
                }
            }

            items(shownRest, key = { it.id }) { item ->
                ReorderableItem(state = reorderState, key = item.id) { isDragging ->
                    val rowBackground by animateColorAsState(
                        if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.background,
                        label = "dragBackground",
                    )
                    val custom = sortOption == LibrarySortOption.CUSTOM && !editLocked
                    val index = dragList.indexOfFirst { it.id == item.id }
                    PinSwipe(pinned = false, onToggle = { togglePin(item.id) }) {
                        LibraryItemRow(
                            item = item,
                            pinned = false,
                            onClick = { openItem(item) },
                            onLongClick = if (custom) null else ({ showMenu(item) }),
                            // In the Custom order, holding a row picks it up to move it.
                            modifier =
                                Modifier
                                    .background(rowBackground)
                                    .then(
                                        if (custom) {
                                            Modifier.longPressDraggableHandle(
                                                onDragStarted = {
                                                    dragging = true
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDragStopped = { saveCustomOrder() },
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ),
                            trailing =
                                when {
                                    custom && isDragging -> {
                                        // Small arrows toward where the row can still go.
                                        {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    painter = painterResource(R.drawable.arrow_upward),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (index > 0) 0.9f else 0f),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Icon(
                                                    painter = painterResource(R.drawable.arrow_downward),
                                                    contentDescription = null,
                                                    tint =
                                                        MaterialTheme.colorScheme.onSurface.copy(
                                                            alpha = if (index in 0 until dragList.lastIndex) 0.9f else 0f,
                                                        ),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                    custom -> {
                                        {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier =
                                                    Modifier
                                                        .clip(CircleShape)
                                                        .clickable { showMenu(item) }
                                                        .padding(8.dp)
                                                        .size(20.dp),
                                            )
                                        }
                                    }
                                    else -> null
                                },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreatePlaylistDialog = true },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    ).padding(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = stringResource(R.string.create_playlist),
            )
        }

        Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}

private class AutoPlaylist(
    val key: String,
    val title: String,
    val icon: Int,
    val route: String,
    val highlighted: Boolean = false,
)

private fun LocalItem.addedAt(): LocalDateTime? =
    when (this) {
        is Playlist -> playlist.bookmarkedAt ?: playlist.createdAt
        is Album -> album.bookmarkedAt
        is Artist -> artist.bookmarkedAt
        else -> null
    }

private fun LocalItem.modifiedAt(): LocalDateTime? =
    when (this) {
        is Playlist -> playlist.lastUpdateTime
        is Album -> album.lastUpdateTime
        is Artist -> artist.lastUpdateTime
        else -> null
    }

/** "Playlists | Artists | Albums" in one pill; the selected one is filled. */
@Composable
private fun KindSegments(
    selected: LibraryKind?,
    onSelect: (LibraryKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(3.dp),
    ) {
        listOf(
            LibraryKind.PLAYLISTS to R.string.filter_playlists,
            LibraryKind.ALBUMS to R.string.filter_albums,
            LibraryKind.ARTISTS to R.string.filter_artists,
        ).forEach { (value, label) ->
            val isSelected = value == selected
            val background by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainer,
                label = "segmentBackground",
            )
            val textColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                label = "segmentText",
            )
            Text(
                text = stringResource(label),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(background)
                        .clickable { onSelect(value) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

/**
 * Swiping a row to the right pins it to the top (or unpins it); the row springs back. Past the
 * trigger point the phone ticks and the label shows. The offset is plain state (nothing saved),
 * so a row that moves between the pinned and other sections can't come back half swiped.
 */
@Composable
private fun PinSwipe(
    pinned: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val latestOnToggle by rememberUpdatedState(onToggle)
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val thresholdPx = with(density) { 110.dp.toPx() }
    val maxPx = with(density) { 160.dp.toPx() }
    var armed by remember { mutableStateOf(false) }
    val dragState =
        rememberDraggableState { delta ->
            val next = (offset.value + delta).coerceIn(0f, maxPx)
            scope.launch { offset.snapTo(next) }
            val nowArmed = next >= thresholdPx
            if (nowArmed != armed) {
                armed = nowArmed
                if (nowArmed) view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
    Box(
        modifier =
            modifier.draggable(
                orientation = Orientation.Horizontal,
                state = dragState,
                onDragStopped = {
                    if (offset.value >= thresholdPx) latestOnToggle()
                    armed = false
                    offset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
                },
            ),
    ) {
        if (offset.value > 0f) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(start = 24.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_push_pin),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
                if (armed) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(if (pinned) R.string.unpin else R.string.pin),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.background),
        ) { content() }
    }
}

@Composable
private fun LibraryItemRow(
    item: LocalItem,
    pinned: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val (kindLabel, count) =
        when (item) {
            is Playlist -> R.string.library_kind_playlist to (item.songCount.takeIf { it > 0 } ?: item.playlist.remoteSongCount ?: 0)
            is Album -> R.string.library_kind_album to item.album.songCount
            is Artist -> R.string.library_kind_artist to item.songCount
            else -> R.string.library_kind_playlist to 0
        }
    val subtitle =
        if (count > 0) {
            stringResource(kindLabel) + " · " + pluralStringResource(R.plurals.n_song, count, count)
        } else {
            stringResource(kindLabel)
        }
    LibraryRow(
        title = item.title,
        subtitle = subtitle,
        pinned = pinned,
        cover = {
            when (item) {
                is Playlist -> PlaylistCover(item)
                is Artist -> CoverImage(item.artist.thumbnailUrl, CircleShape, R.drawable.person)
                is Album -> CoverImage(item.album.thumbnailUrl, RoundedCornerShape(CoverCorner), R.drawable.album)
                else -> Unit
            }
        },
        onClick = onClick,
        onLongClick = onLongClick,
        trailing = trailing,
        modifier = modifier,
    )
}

private val CoverSize = 56.dp
private val CoverCorner = 10.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryRow(
    title: String,
    subtitle: String,
    cover: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Box(Modifier.size(CoverSize)) { cover() }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty() || pinned) Spacer(Modifier.height(2.dp))
                if (subtitle.isNotEmpty() || pinned) Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pinned) {
                        Icon(
                            painter = painterResource(R.drawable.ic_push_pin),
                            contentDescription = stringResource(R.string.library_pinned),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    painter = painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(start = 20.dp + CoverSize + 14.dp),
        )
    }
}

@Composable
private fun AutoPlaylistCover(auto: AutoPlaylist) {
    // Liked stands out: filled with the text color, heart in the background color.
    val background = if (auto.highlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh
    val tint = if (auto.highlighted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(CoverSize)
                .clip(RoundedCornerShape(CoverCorner))
                .background(background),
    ) {
        Icon(
            painter = painterResource(auto.icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
    }
}

/**
 * A playlist's own cover, or else the covers of its first four songs in a 2×2 grid (one cover
 * when it has fewer songs).
 */
@Composable
fun PlaylistCover(
    playlist: Playlist,
    size: Dp = CoverSize,
    corner: Dp = CoverCorner,
) {
    val thumbnails = playlist.thumbnails
    com.metrolist.music.ui.theme.PrefetchCoverColor(thumbnails.firstOrNull())
    androidx.compose.runtime.SideEffect { com.metrolist.music.viewmodels.PlaylistPreviewCache.put(playlist) }
    val shape = RoundedCornerShape(corner)
    when {
        playlist.playlist.thumbnailUrl == null && thumbnails.size >= 4 ->
            Column(
                modifier =
                    Modifier
                        .size(size)
                        .clip(shape),
            ) {
                listOf(0, 2).forEach { row ->
                    Row(Modifier.weight(1f)) {
                        listOf(row, row + 1).forEach { index ->
                            AsyncImage(
                                model = thumbnails[index],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                            )
                        }
                    }
                }
            }

        else -> CoverImage(thumbnails.firstOrNull(), shape, R.drawable.queue_music, size)
    }
}

@Composable
private fun CoverImage(
    url: String?,
    shape: androidx.compose.ui.graphics.Shape,
    placeholderIcon: Int,
    size: Dp = CoverSize,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                painter = painterResource(placeholderIcon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.45f),
            )
        }
    }
}
