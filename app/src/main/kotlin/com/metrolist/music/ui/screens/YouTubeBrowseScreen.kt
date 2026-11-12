/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.viewmodels.YouTubeBrowseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YouTubeBrowseScreen(
    navController: NavController,
    viewModel: YouTubeBrowseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val browseResult by viewModel.result.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    // For You (the page as YouTube gives it), Popular or New. One is always picked; leaving the
    // genre and coming back starts again at For You.
    var filter by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(GenreFilter.FOR_YOU) }
    val sections = browseResult?.items.orEmpty()
    val allItems =
        remember(sections, filter) {
            when (filter) {
                GenreFilter.FOR_YOU -> sections.flatMap { it.items }
                GenreFilter.POPULAR -> {
                    val popular = sections.filter { it.title.matchesAny(PopularWords) }
                    // No section named that way: everything except the new releases.
                    (popular.ifEmpty { sections.filterNot { it.title.matchesAny(NewWords) } }).flatMap { it.items }
                }
                GenreFilter.NEW -> {
                    val fresh = sections.filter { it.title.matchesAny(NewWords) }.flatMap { it.items }
                    // No section named that way: the albums and singles, which are the releases.
                    fresh.ifEmpty { sections.flatMap { it.items }.filterIsInstance<AlbumItem>() }
                        .ifEmpty { sections.flatMap { it.items } }
                }
            }
        }

    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        item(key = "genre_filters") {
            GenreFilterSegments(
                selected = filter,
                onSelect = { filter = it },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            )
        }

        if (browseResult == null) {
            items(8) {
                ShimmerHost {
                    com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder()
                }
            }
        }

        items(
            items = allItems.distinctBy { it.id },
            key = { "yt_browse_${it.id}" },
        ) { item ->
            com.metrolist.music.ui.component.YouTubeListItem(
                item = item,
                isActive =
                    when (item) {
                        is SongItem -> mediaMetadata?.id == item.id
                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                        else -> false
                    },
                isPlaying = isPlaying,
                modifier =
                    Modifier
                        .combinedClickable(
                            onClick = {
                                when (item) {
                                    is SongItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(item.toMediaMetadata()),
                                            )
                                        }
                                    }

                                    is AlbumItem -> {
                                        navController.navigate("album/${item.id}")
                                    }

                                    is ArtistItem -> {
                                        navController.navigate("artist/${item.id}")
                                    }

                                    is PlaylistItem -> {
                                        navController.navigate("online_playlist/${item.id}")
                                    }

                                    is PodcastItem -> {
                                        navController.navigate("online_podcast/${item.id}")
                                    }

                                    is EpisodeItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(item.toMediaMetadata()),
                                            )
                                        }
                                    }
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    when (item) {
                                        is SongItem -> {
                                            YouTubeSongMenu(
                                                song = item,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }

                                        is AlbumItem -> {
                                            YouTubeAlbumMenu(
                                                albumItem = item,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }

                                        is ArtistItem -> {
                                            YouTubeArtistMenu(
                                                artist = item,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }

                                        is PlaylistItem -> {
                                            YouTubePlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }

                                        is PodcastItem -> {
                                            YouTubePlaylistMenu(
                                                playlist = item.asPlaylistItem(),
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }

                                        is EpisodeItem -> {
                                            YouTubeSongMenu(
                                                song = item.asSongItem(),
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }
                                }
                            },
                        ).animateItem(),
            )
        }
    }

    TopAppBar(
        title = { Text(browseResult?.title.orEmpty()) },
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
}

private enum class GenreFilter { FOR_YOU, POPULAR, NEW }

private val PopularWords =
    listOf("popular", "top", "trending", "featured", "hot", "best", "popüler", "öne çıkan", "en iyi", "en çok")
private val NewWords =
    listOf("new", "release", "latest", "fresh", "recent", "yeni", "son çıkan", "çıkanlar")

private fun String?.matchesAny(words: List<String>): Boolean {
    val text = this?.lowercase() ?: return false
    return words.any { it in text }
}

/** The same rounded segments as the Library filters; one of them is always picked. */
@Composable
private fun GenreFilterSegments(
    selected: GenreFilter,
    onSelect: (GenreFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(3.dp),
    ) {
        listOf(
            GenreFilter.FOR_YOU to R.string.genre_filter_for_you,
            GenreFilter.POPULAR to R.string.genre_filter_popular,
            GenreFilter.NEW to R.string.genre_filter_new,
        ).forEach { (value, label) ->
            val isSelected = value == selected
            val background by androidx.compose.animation.animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainer,
                label = "genreSegmentBackground",
            )
            val textColor by androidx.compose.animation.animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                label = "genreSegmentText",
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
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(background)
                        .clickable { onSelect(value) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}
