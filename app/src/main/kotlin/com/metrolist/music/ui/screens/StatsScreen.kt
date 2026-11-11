/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import com.metrolist.music.ui.theme.TintedPageColors
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.StatPeriod
import com.metrolist.music.constants.TopSize
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.TimeTransfer
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.theme.albumBackgroundColor
import com.metrolist.music.ui.theme.rememberPageCoverColor
import com.metrolist.music.ui.theme.rememberPageTint
import com.metrolist.music.ui.theme.pageTintFor
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.ListeningPattern
import com.metrolist.music.viewmodels.StatsViewModel
import java.time.format.DateTimeFormatter

private enum class StatsSection { SONGS, ARTISTS, ALBUMS }

private const val STATS_PREVIEW_COUNT = 5

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val indexChips by viewModel.indexChips.collectAsStateWithLifecycle()
    val songsWithData by viewModel.mostPlayedSongs.collectAsStateWithLifecycle()
    val songs by viewModel.mostPlayedSongsStats.collectAsStateWithLifecycle()
    val artists by viewModel.mostPlayedArtists.collectAsStateWithLifecycle()
    val albums by viewModel.mostPlayedAlbums.collectAsStateWithLifecycle()
    val pattern by viewModel.listeningPattern.collectAsStateWithLifecycle()

    val (topSizePref) = rememberPreference(TopSize, "50")
    val topSize = topSizePref.toIntOrNull()?.coerceIn(1, 100) ?: 50
    val period = StatPeriod.entries.getOrElse(indexChips) { StatPeriod.WEEK_1 }

    var expandedName by rememberSaveable { mutableStateOf<String?>(null) }
    val expanded = expandedName?.let { name -> StatsSection.entries.firstOrNull { it.name == name } }
    BackHandler(enabled = expanded != null) { expandedName = null }

    var showTimeTransfer by rememberSaveable { mutableStateOf(false) }
    var periodBeforeTransfer by rememberSaveable { mutableStateOf<Int?>(null) }
    // Moving listening time between songs works on the whole history.
    LaunchedEffect(showTimeTransfer) {
        if (showTimeTransfer) {
            if (periodBeforeTransfer == null) periodBeforeTransfer = indexChips
            viewModel.indexChips.value = StatPeriod.ALL.ordinal
        }
    }
    if (showTimeTransfer) {
        TimeTransfer(
            onDismiss = {
                showTimeTransfer = false
                periodBeforeTransfer?.let { viewModel.indexChips.value = it }
                periodBeforeTransfer = null
            },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.selectedOption.value = OptionStats.CONTINUOUS
        viewModel.syncMostPlaylistsIfNeeded()
    }

    val orderedSongs =
        remember(songs, songsWithData) {
            val byId = songsWithData.associateBy { it.song.id }
            songs.mapNotNull { byId[it.id] }
        }

    // The whole page takes the color of the most played song's cover.
    val pageBackground = MaterialTheme.colorScheme.background
    val darkTheme = pageBackground.luminance() < 0.5f
    val coverColor = rememberPageCoverColor(songs.firstOrNull()?.thumbnailUrl, variant = period.name)
    val tint =
        rememberPageTint(
            target = pageTintFor(coverColor, pageBackground),
            ready = coverColor != null,
        )
    val accent = coverColor?.let { lerp(it, if (darkTheme) Color.White else Color.Black, 0.4f) } ?: MaterialTheme.colorScheme.primary
    val chipContainer = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f).compositeOver(tint)

    val playSong: (String) -> Unit = { songId ->
        if (songId == playerConnection.mediaMetadata.value?.id) {
            playerConnection.togglePlayPause()
        } else {
            songsWithData.find { it.id == songId }?.let { target ->
                playerConnection.playQueue(
                    YouTubeQueue(
                        endpoint = WatchEndpoint(songId),
                        preloadItem = target.toMediaMetadata(),
                    ),
                )
            }
        }
    }
    val showSongMenu: (String) -> Unit = { songId ->
        songsWithData.find { it.id == songId }?.let { target ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuState.show { SongMenu(originalSong = target, onDismiss = menuState::dismiss) }
        }
    }

    val songRows: LazyListScope.(Int) -> Unit = { limit ->
        itemsIndexed(songs.take(limit), key = { index, song -> "song_${song.id}_$index" }) { index, song ->
            StatsRow(
                rank = index + 1,
                thumbnailUrl = song.thumbnailUrl,
                title = song.title,
                subtitle =
                    listOfNotNull(
                        (song.artists.joinToString { it.name }.ifBlank { song.artistName.orEmpty() }).takeIf { it.isNotBlank() },
                        pluralStringResource(R.plurals.n_time, song.songCountListened, song.songCountListened),
                        listenTime(song.timeListened),
                    ).joinToString(" · "),
                accent = accent,
                modifier =
                    Modifier.combinedClickable(
                        onClick = { playSong(song.id) },
                        onLongClick = { showSongMenu(song.id) },
                    ),
            )
        }
    }
    val artistRows: LazyListScope.(Int) -> Unit = { limit ->
        itemsIndexed(artists.take(limit), key = { _, artist -> "artist_${artist.id}" }) { index, artist ->
            StatsRow(
                rank = index + 1,
                thumbnailUrl = artist.artist.thumbnailUrl,
                title = artist.artist.name,
                subtitle = listenTime(artist.timeListened?.toLong()),
                accent = accent,
                round = true,
                modifier =
                    Modifier.combinedClickable(
                        onClick = { navController.navigate("artist/${artist.id}") },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = artist,
                                    coroutineScope = coroutineScope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )
        }
    }
    val albumRows: LazyListScope.(Int) -> Unit = { limit ->
        itemsIndexed(albums.take(limit), key = { _, album -> "album_${album.id}" }) { index, album ->
            StatsRow(
                rank = index + 1,
                thumbnailUrl = album.album.thumbnailUrl,
                title = album.album.title,
                subtitle =
                    listOfNotNull(
                        album.artists.joinToString { it.name }.takeIf { it.isNotBlank() },
                        listenTime(album.timeListened),
                    ).joinToString(" · "),
                accent = accent,
                modifier =
                    Modifier.combinedClickable(
                        onClick = { navController.navigate("album/${album.id}") },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show { AlbumMenu(originalAlbum = album, onDismiss = menuState::dismiss) }
                        },
                    ),
            )
        }
    }

    val periodChips: @Composable () -> Unit = {
        ChipsRow(
            chips =
                listOf(
                    StatPeriod.DAY_1 to stringResource(R.string.stats_daily),
                    StatPeriod.WEEK_1 to stringResource(R.string.stats_weekly),
                    StatPeriod.MONTH_1 to stringResource(R.string.stats_monthly),
                    StatPeriod.YEAR_1 to stringResource(R.string.stats_yearly),
                    StatPeriod.ALL to stringResource(R.string.stats_lifetime),
                ),
            currentValue = period,
            onValueUpdate = { viewModel.indexChips.value = it.ordinal },
            containerColor = chipContainer,
        )
    }

    val mainListState = rememberLazyListState()

    TintedPageColors(tint) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(tint),
        ) {
            Crossfade(targetState = expanded, label = "statsSection") { section ->
                val listState = if (section == null) mainListState else remember(section) { LazyListState() }
                LazyColumn(
                    state = listState,
                    contentPadding =
                        LocalPlayerAwareWindowInsets.current
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)),
                ) {
                    item(key = "period") { periodChips() }

                    if (section == null) {
                        item(key = "summary") {
                            StatsSummaryCard(
                                pattern = pattern,
                                songCount = songs.size,
                                artistCount = artists.size,
                                albumCount = albums.size,
                                coverColor = coverColor,
                            )
                        }
                        if (songs.isEmpty() && artists.isEmpty() && albums.isEmpty()) {
                            item(key = "empty") {
                                EmptyPlaceholder(
                                    icon = R.drawable.music_note,
                                    text = stringResource(R.string.stats_empty),
                                )
                            }
                        }
                        if (songs.isNotEmpty()) {
                            item(key = "songs_title") {
                                StatsSectionTitle(stringResource(R.string.stats_top_songs)) { expandedName = StatsSection.SONGS.name }
                            }
                            songRows(STATS_PREVIEW_COUNT)
                        }
                        if (artists.isNotEmpty()) {
                            item(key = "artists_title") {
                                StatsSectionTitle(stringResource(R.string.stats_top_artists)) { expandedName = StatsSection.ARTISTS.name }
                            }
                            artistRows(STATS_PREVIEW_COUNT)
                        }
                        if (albums.isNotEmpty()) {
                            item(key = "albums_title") {
                                StatsSectionTitle(stringResource(R.string.stats_top_albums)) { expandedName = StatsSection.ALBUMS.name }
                            }
                            albumRows(STATS_PREVIEW_COUNT)
                        }
                        item(key = "bottom_space") { Spacer(Modifier.height(24.dp)) }
                    } else {
                        when (section) {
                            StatsSection.SONGS -> songRows(topSize)
                            StatsSection.ARTISTS -> artistRows(topSize)
                            else -> albumRows(topSize)
                        }
                        item(key = "bottom_space") { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }

            if (orderedSongs.isNotEmpty() && expanded != StatsSection.ARTISTS && expanded != StatsSection.ALBUMS) {
                HideOnScrollFAB(
                    visible = true,
                    lazyListState = mainListState,
                    icon = R.drawable.shuffle,
                    onClick = {
                        playerConnection.playQueue(
                            ListQueue(
                                title = context.getString(R.string.most_played_songs),
                                items = orderedSongs.take(topSize).map { it.toMediaMetadata().toMediaItem() }.shuffled(),
                            ),
                        )
                    },
                )
            }

            TopAppBar(
                title = {
                    Text(
                        when (expanded) {
                            StatsSection.SONGS -> stringResource(R.string.stats_top_songs)
                            StatsSection.ARTISTS -> stringResource(R.string.stats_top_artists)
                            StatsSection.ALBUMS -> stringResource(R.string.stats_top_albums)
                            null -> stringResource(R.string.stats)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { if (expanded != null) expandedName = null else navController.navigateUp() },
                        onLongClick = { navController.backToMain() },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    if (expanded == null) {
                        IconButton(
                            onClick = { showTimeTransfer = true },
                            onLongClick = { showTimeTransfer = true },
                        ) {
                            Icon(
                                painterResource(R.drawable.sync),
                                contentDescription = null,
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = tint,
                        scrolledContainerColor = tint,
                    ),
            )
        }
    }
}

/** "34 min" under an hour, "1h 47m" above. */
@Composable
private fun listenTime(millis: Long?): String {
    val minutes = ((millis ?: 0L) / 60000L).toInt()
    return if (minutes < 60) {
        stringResource(R.string.stats_minutes, minutes)
    } else {
        stringResource(R.string.stats_hours_minutes, minutes / 60, minutes % 60)
    }
}

@Composable
private fun StatsSectionTitle(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 2.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            painter = painterResource(R.drawable.navigate_next),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun StatsRow(
    rank: Int,
    thumbnailUrl: String?,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    round: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 7.dp),
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (rank == 1) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.widthIn(min = 30.dp),
        )
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(if (round) CircleShape else RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The card at the top: total listening time, how many different songs, artists and albums were
 * played, and when the user listens most (hour of the day with a small 24-hour chart, and the
 * busiest day).
 */
@Composable
private fun StatsSummaryCard(
    pattern: ListeningPattern,
    songCount: Int,
    artistCount: Int,
    albumCount: Int,
    coverColor: Color?,
) {
    val base = coverColor?.let { albumBackgroundColor(it, true) } ?: Color(0xFF2B2A30)
    val content = Color.White
    val faded = content.copy(alpha = 0.65f)
    val shape = RoundedCornerShape(28.dp)
    val totalMinutes = (pattern.totalPlayTime / 60000L).toInt()

    Column(
        modifier =
            Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(lerp(base, Color.White, 0.16f), base, lerp(base, Color.Black, 0.35f)),
                    ),
                ).border(1.dp, Color.White.copy(alpha = 0.1f), shape)
                .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.stats_your_listening).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.5.sp,
                color = faded,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.graphic_eq),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = totalMinutes.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = content,
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = stringResource(R.string.stats_minutes_listened).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.2.sp,
                    color = faded,
                )
                Text(
                    text = stringResource(R.string.stats_plays, pattern.plays),
                    style = MaterialTheme.typography.bodySmall,
                    color = faded,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                songCount to stringResource(R.string.songs),
                artistCount to stringResource(R.string.artists),
                albumCount to stringResource(R.string.albums),
            ).forEach { (count, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(vertical = 10.dp),
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = content,
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = faded,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        val peakHour = pattern.peakHour
        val topDay = pattern.topDay
        if (peakHour != null && topDay != null) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.stats_peak_hour),
                        style = MaterialTheme.typography.labelMedium,
                        color = faded,
                    )
                    Text(
                        text = "%02d:00".format(peakHour),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = content,
                    )
                    Spacer(Modifier.height(6.dp))
                    HourBars(
                        playTimeByHour = pattern.playTimeByHour,
                        peakHour = peakHour,
                        color = content,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(26.dp),
                    )
                }
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.stats_top_day),
                        style = MaterialTheme.typography.labelMedium,
                        color = faded,
                    )
                    Text(
                        text = remember(topDay) { topDay.format(DateTimeFormatter.ofPattern("EEE, d MMM")) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = content,
                        maxLines = 1,
                    )
                    Text(
                        text = listenTime(pattern.topDayPlayTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = faded,
                    )
                }
            }
        }
    }
}

/** 24 thin bars, one per hour of the day; the busiest hour is fully bright. */
@Composable
private fun HourBars(
    playTimeByHour: List<Long>,
    peakHour: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val max = (playTimeByHour.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
        val slot = size.width / playTimeByHour.size
        val barWidth = slot * 0.6f
        val minHeight = 2.dp.toPx()
        playTimeByHour.forEachIndexed { hour, playTime ->
            val barHeight = (size.height * (playTime / max)).coerceAtLeast(minHeight)
            drawRoundRect(
                color = color.copy(alpha = if (hour == peakHour) 1f else 0.35f),
                topLeft = Offset(hour * slot + (slot - barWidth) / 2f, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

enum class OptionStats { WEEKS, MONTHS, YEARS, CONTINUOUS }
