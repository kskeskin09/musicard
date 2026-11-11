/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import com.metrolist.music.viewmodels.MoodAndGenresViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import com.metrolist.music.LocalNavController
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.utils.YouTubeUrlParser
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalIsPlayerExpanded
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.constants.SearchSource
import com.metrolist.music.constants.SearchSourceKey
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.utils.SearchRoutes
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    pureBlack: Boolean,
    savedStateHandle: SavedStateHandle,
) {
    val navController = LocalNavController.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val playerConnection = LocalPlayerConnection.current
    val isPlayerExpanded = LocalIsPlayerExpanded.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lazyListState = rememberLazyListState()
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var isHandlingScrollToTop by remember { mutableStateOf(false) }

    val scrollToTopCount by savedStateHandle.getStateFlow("scrollToTopCount", 0).collectAsStateWithLifecycle(initialValue = 0)

    var lastHandledCount by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(scrollToTopCount) {
        if (scrollToTopCount > lastHandledCount) {
            lastHandledCount = scrollToTopCount
            isHandlingScrollToTop = true

            kotlinx.coroutines.delay(100)

            if (!isPlayerExpanded) {
                // Tapping the tab again goes back to the top, and from the top opens the search.
                if (!searchActive && lazyListState.firstVisibleItemIndex > 0) {
                    lazyListState.animateScrollToItem(0)
                } else {
                    searchActive = true
                }
            }

            kotlinx.coroutines.delay(500)
            isHandlingScrollToTop = false
        }
    }

    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)

    fun handleSearch(searchQuery: String) {
        if (searchQuery.isEmpty()) {
            return
        }

        focusManager.clearFocus()

        when (val parsedUrl = YouTubeUrlParser.parse(searchQuery)) {
            is YouTubeUrlParser.ParsedUrl.Video -> {
                playerConnection?.playQueue(
                    YouTubeQueue(
                        WatchEndpoint(videoId = parsedUrl.id),
                    ),
                )
            }

            is YouTubeUrlParser.ParsedUrl.Playlist -> {
                navController.navigate("online_playlist/${parsedUrl.id}")
            }

            is YouTubeUrlParser.ParsedUrl.Album -> {
                navController.navigate("album/MPREb_${parsedUrl.id}")
            }

            is YouTubeUrlParser.ParsedUrl.Artist -> {
                navController.navigate("artist/${parsedUrl.id}")
            }

            null -> {
                navController.navigate(SearchRoutes.resultRoute(searchQuery))
            }
        }

        if (!pauseSearchHistory) {
            coroutineScope.launch(Dispatchers.IO) {
                database.query {
                    insert(SearchHistory(query = searchQuery))
                }
            }
        }
    }

    val onSearch: (String) -> Unit = { searchQuery -> handleSearch(searchQuery) }

    val onSearchFromSuggestion: (String) -> Unit = { searchQuery -> handleSearch(searchQuery) }

    // The page shows Genres until the search field is tapped; then it shows the search history
    // and suggestions under the field, with a back arrow that returns to Genres.
    val closeSearch: () -> Unit = {
        searchActive = false
        query = TextFieldValue("")
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    BackHandler(enabled = searchActive) { closeSearch() }
    LaunchedEffect(searchActive) {
        if (searchActive) {
            kotlinx.coroutines.delay(50)
            runCatching {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }
    val pageBackground = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background

    val searchBar: @Composable () -> Unit = {
        SearchPill(
            query = query,
            onQueryChange = { query = it },
            active = searchActive,
            onActivate = { searchActive = true },
            onBack = closeSearch,
            onSearch = { onSearch(query.text) },
            focusRequester = focusRequester,
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(pageBackground)
                .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        if (searchActive) {
            val historyListState = androidx.compose.foundation.lazy.rememberLazyListState()
            // The "Search history" heading hides once the list is scrolled down, and comes back at the top.
            val historyAtTop by remember {
                derivedStateOf {
                    historyListState.firstVisibleItemIndex == 0 && historyListState.firstVisibleItemScrollOffset < 8
                }
            }
            Column(Modifier.fillMaxSize()) {
                // The page title stays while searching.
                Text(
                    text = stringResource(R.string.search),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .padding(horizontal = 20.dp)
                            .padding(top = 8.dp)
                            .height(56.dp)
                            .wrapContentHeight(Alignment.CenterVertically),
                )
                Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { searchBar() }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = query.text.isEmpty() && historyAtTop,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
                ) {
                    Text(
                        text = stringResource(R.string.search_history),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
                    )
                }
                Box(Modifier.weight(1f)) {
                    OnlineSearchScreen(
                        query = query.text,
                        onQueryChange = { query = it },
                        onSearch = onSearchFromSuggestion,
                        onDismiss = { /* Don't dismiss when searching from suggestions */ },
                        pureBlack = pureBlack,
                        lazyListState = historyListState,
                    )
                }
            }
        } else {
            SearchBrowsePage(
                listState = lazyListState,
                pageBackground = pageBackground,
                searchBar = searchBar,
            )

            HideOnScrollFAB(
                lazyListState = lazyListState,
                icon = R.drawable.mic,
                onClick = { navController.navigate("recognition") },
            )
        }
    }

    // Handle lifecycle events to manage keyboard visibility
    DisposableEffect(lifecycleOwner, isPlayerExpanded) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        if (isHandlingScrollToTop) return@LifecycleEventObserver
                        // Always hide keyboard when resuming if player is expanded
                        if (isPlayerExpanded) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        if (isHandlingScrollToTop) return@LifecycleEventObserver
                        // Clear focus when pausing to prevent keyboard from showing on resume
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }

                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Initial check - hide keyboard if player is expanded
        if (isPlayerExpanded) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
/** The search field as a pill: a search icon at rest, a back arrow while searching. */
@Composable
private fun SearchPill(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    active: Boolean,
    onActivate: () -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(enabled = !active, onClick = onActivate)
                .padding(start = if (active) 4.dp else 20.dp, end = 4.dp),
    ) {
        if (active) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        if (active) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint_everything),
                            style = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp),
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            )
            if (query.text.isNotEmpty()) {
                IconButton(onClick = { onQueryChange(TextFieldValue("")) }) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.search_hint_everything),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The Search tab at rest: the page name (it scrolls away), the search pill (it stays at the top,
 * with the page fading out under it) and YouTube Music's moods and genres as colored tiles.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchBrowsePage(
    listState: androidx.compose.foundation.lazy.LazyListState,
    pageBackground: Color,
    searchBar: @Composable () -> Unit,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val rawSections by viewModel.moodAndGenres.collectAsStateWithLifecycle()
    // Genres come first, then moods.
    val sections =
        remember(rawSections) {
            rawSections?.let { list ->
                val genreWords = listOf("genre", "tür", "genre", "género", "gênero", "genere", "жанр")
                val genreIndex = list.indexOfFirst { s -> genreWords.any { s.title.contains(it, ignoreCase = true) } }
                when {
                    genreIndex > 0 -> listOf(list[genreIndex]) + list.filterIndexed { i, _ -> i != genreIndex }
                    genreIndex == -1 && list.size == 2 -> list.reversed()
                    else -> list
                }
            }
        }
    val bottomPadding =
        LocalPlayerAwareWindowInsets.current
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "search_title") {
            Text(
                text = stringResource(R.string.search),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp)
                        .height(56.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
            )
        }
        stickyHeader(key = "search_bar") {
            Column {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(pageBackground)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                ) { searchBar() }
                // The tiles fade out as they scroll up under the search bar.
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .background(Brush.verticalGradient(listOf(pageBackground, pageBackground.copy(alpha = 0f)))),
                )
            }
        }
        if (sections == null) {
            item(key = "genres_loading") {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        sections?.forEachIndexed { index, section ->
            item(key = "genres_title_$index") {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = if (index == 0) 0.dp else 20.dp, bottom = 16.dp)) {
                    Text(
                        text = section.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                }
            }
            section.items.chunked(2).forEachIndexed { rowIndex, row ->
                item(key = "genres_row_${index}_$rowIndex") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp),
                    ) {
                        row.forEach { genre ->
                            GenreTile(
                                title = genre.title,
                                stripeColor = Color(genre.stripeColor),
                                pageBackground = pageBackground,
                                onClick = {
                                    navController.navigate("youtube_browse/${genre.endpoint.browseId}?params=${genre.endpoint.params}")
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size < 2) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** A mood or genre: a rounded tile in a calm version of its YouTube color, with its name. */
@Composable
private fun GenreTile(
    title: String,
    stripeColor: Color,
    pageBackground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val base = androidx.compose.ui.graphics.lerp(stripeColor, pageBackground, 0.45f)
    val end = androidx.compose.ui.graphics.lerp(base, pageBackground, 0.35f)
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier =
            modifier
                .height(84.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(base, end)))
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
