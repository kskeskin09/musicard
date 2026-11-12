/**
 * Musicard (C) 2026, based on Metrolist
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.importer.ImportBundle
import com.metrolist.music.importer.ImportOptions
import com.metrolist.music.importer.MatchQuality
import com.metrolist.music.importer.MatchReportItem
import com.metrolist.music.importer.ServiceImportManager
import com.metrolist.music.importer.ServiceImportState
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.SearchRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ImportFromServicesViewModel @Inject constructor(
    val manager: ServiceImportManager,
) : ViewModel()

private val SpotifyGreen = Color(0xFF1DB954)
private val AppleRed = Color(0xFFFA2D48)
private val YouTubeRed = Color(0xFFFF0033)
private val GoodColor = Color(0xFF7FDC9E)
private val UnsureColor = Color(0xFFE8C96A)
private val BadColor = Color(0xFFF28B82)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportFromServicesScreen(
    navController: NavController,
    viewModel: ImportFromServicesViewModel = hiltViewModel(),
) {
    val manager = viewModel.manager
    val state by manager.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) manager.read(uri)
    }
    val pick = { picker.launch(arrayOf("*/*")) }

    val stepSongs = stringResource(R.string.import_step_songs)
    val stepAlbums = stringResource(R.string.import_step_albums)
    val stepArtists = stringResource(R.string.import_step_artists)

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        when (val s = state) {
            is ServiceImportState.Ready -> PreviewSection(
                bundle = s.bundle,
                onStart = { options ->
                    manager.start(s.bundle, options, ServiceImportManager.StepNames(stepSongs, stepAlbums, stepArtists))
                },
                onPickOther = { manager.reset(); pick() },
            )

            is ServiceImportState.Running -> RunningSection(s, onCancel = manager::cancel)

            is ServiceImportState.Finished -> FinishedSection(
                state = s,
                onSearch = { query -> navController.navigate(SearchRoutes.resultRoute(query)) },
                onDone = { manager.reset() },
            )

            else -> {
                Text(
                    text = stringResource(R.string.import_services_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                )

                if (s is ServiceImportState.Reading) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.import_reading))
                    }
                }
                if (s is ServiceImportState.Unsupported) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    ) {
                        Text(
                            stringResource(R.string.import_unsupported),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }

                SourceCard(
                    badge = { BrandBadge("S", SpotifyGreen, Color.Black) },
                    title = "Spotify",
                    description = stringResource(R.string.import_spotify_desc),
                    chips = listOf(
                        stringResource(R.string.import_chip_playlists),
                        stringResource(R.string.import_chip_liked),
                        stringResource(R.string.import_chip_albums),
                    ),
                    expanded = expanded == "spotify",
                    onClick = { expanded = if (expanded == "spotify") null else "spotify" },
                ) {
                    Hint(stringResource(R.string.import_no_login))
                    Step(1, stringResource(R.string.import_spotify_step1))
                    Step(2, stringResource(R.string.import_spotify_step2))
                    Step(3, stringResource(R.string.import_spotify_step3))
                    TextButton(onClick = { uriHandler.openUri("https://www.spotify.com/account/privacy/") }) {
                        Text(stringResource(R.string.import_spotify_open))
                    }
                    Button(onClick = pick, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.import_pick_zip))
                    }
                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://exportify.app/") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.import_spotify_exportify))
                    }
                }

                SourceCard(
                    badge = { BrandBadge("♪", AppleRed, Color.White) },
                    title = "Apple Music",
                    description = stringResource(R.string.import_apple_desc),
                    chips = listOf(
                        stringResource(R.string.import_chip_playlists),
                        stringResource(R.string.import_chip_library),
                    ),
                    expanded = expanded == "apple",
                    onClick = { expanded = if (expanded == "apple") null else "apple" },
                ) {
                    Text(stringResource(R.string.import_apple_way1_title), style = MaterialTheme.typography.titleSmall)
                    Hint(stringResource(R.string.import_apple_way1))
                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://privacy.apple.com/") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.import_apple_open))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.import_apple_way2_title), style = MaterialTheme.typography.titleSmall)
                    Hint(stringResource(R.string.import_apple_way2))
                    Button(onClick = pick, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.import_pick_file))
                    }
                }

                SourceCard(
                    badge = { BrandBadge("▶", YouTubeRed, Color.White) },
                    title = "YouTube Music",
                    description = stringResource(R.string.import_ytm_desc),
                    chips = listOf(stringResource(R.string.import_ytm_badge)),
                    expanded = false,
                    onClick = null,
                ) {}

                SourceCard(
                    badge = {
                        Box(
                            Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.upload),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    },
                    title = stringResource(R.string.import_file),
                    description = stringResource(R.string.import_file_desc),
                    chips = emptyList(),
                    expanded = false,
                    onClick = pick,
                ) {}
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.import_services)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

@Composable
private fun BrandBadge(letter: String, background: Color, foreground: Color) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = foreground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SourceCard(
    badge: @Composable () -> Unit,
    title: String,
    description: String,
    chips: List<String>,
    expanded: Boolean,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                badge()
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (chips.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 6.dp),
                        ) {
                            chips.forEach { chip ->
                                Text(
                                    chip,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .padding(bottom = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PreviewSection(
    bundle: ImportBundle,
    onStart: (ImportOptions) -> Unit,
    onPickOther: () -> Unit,
) {
    var playlists by rememberSaveable { mutableStateOf(true) }
    var liked by rememberSaveable { mutableStateOf(true) }
    var albums by rememberSaveable { mutableStateOf(true) }
    var artists by rememberSaveable { mutableStateOf(true) }

    Text(
        stringResource(R.string.import_found_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
    )
    if (bundle.playlists.isNotEmpty()) {
        OptionRow(stringResource(R.string.playlists), bundle.playlists.size, playlists) { playlists = it }
    }
    if (bundle.likedSongs.isNotEmpty()) {
        OptionRow(stringResource(R.string.liked_songs), bundle.likedSongs.size, liked) { liked = it }
    }
    if (bundle.albums.isNotEmpty()) {
        OptionRow(stringResource(R.string.import_saved_albums), bundle.albums.size, albums) { albums = it }
    }
    if (bundle.artists.isNotEmpty()) {
        OptionRow(stringResource(R.string.import_followed_artists), bundle.artists.size, artists) { artists = it }
    }
    if (bundle.likedSongs.isNotEmpty()) {
        Text(
            stringResource(R.string.import_likes_local_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(4.dp),
        )
    }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { onStart(ImportOptions(playlists, liked, albums, artists)) },
        enabled = playlists || liked || albums || artists,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.import_start))
    }
    OutlinedButton(onClick = onPickOther, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.import_choose_other))
    }
}

@Composable
private fun OptionRow(label: String, count: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 2.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            "%,d".format(count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

@Composable
private fun RunningSection(state: ServiceImportState.Running, onCancel: () -> Unit) {
    Text(
        stringResource(R.string.import_running),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
    Text(
        stringResource(R.string.import_progress, state.step, state.done, state.total),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    LinearProgressIndicator(
        progress = { if (state.total == 0) 0f else state.done.toFloat() / state.total },
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
    )
    StatRow("✓ " + stringResource(R.string.import_matched), state.matched, GoodColor)
    StatRow("? " + stringResource(R.string.import_unsure), state.unsure, UnsureColor)
    StatRow("✕ " + stringResource(R.string.import_not_found), state.notFound, BadColor)
    Hint(stringResource(R.string.import_background))
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.import_cancel))
    }
}

@Composable
private fun StatRow(label: String, value: Int, color: Color) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(label, modifier = Modifier.weight(1f))
            Text("%,d".format(value), color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FinishedSection(
    state: ServiceImportState.Finished,
    onSearch: (String) -> Unit,
    onDone: () -> Unit,
) {
    Text(
        stringResource(R.string.import_done),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
    Hint(stringResource(R.string.import_done_summary, state.playlistsCreated, state.albumsSaved, state.artistsFollowed))
    Spacer(Modifier.height(8.dp))
    StatRow("✓ " + stringResource(R.string.import_matched), state.matched, GoodColor)
    StatRow("? " + stringResource(R.string.import_unsure), state.unsure, UnsureColor)
    StatRow("✕ " + stringResource(R.string.import_not_found), state.notFound, BadColor)
    Spacer(Modifier.height(8.dp))
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.import_finish))
    }
    if (state.problems.isNotEmpty()) {
        Text(
            stringResource(R.string.import_check_these),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
        )
        state.problems.take(300).forEach { item -> ProblemRow(item, onSearch) }
    }
}

@Composable
private fun ProblemRow(item: MatchReportItem, onSearch: (String) -> Unit) {
    val unsure = item.quality == MatchQuality.UNSURE
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSearch(item.query) },
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background((if (unsure) UnsureColor else BadColor).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (unsure) "?" else "✕", color = if (unsure) UnsureColor else BadColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.original, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(
                    if (unsure && item.found != null) stringResource(R.string.import_found_as, item.found)
                    else stringResource(R.string.import_search_manually),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}
