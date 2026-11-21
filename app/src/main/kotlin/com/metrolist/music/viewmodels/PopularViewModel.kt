/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.models.filterVideoSongs
import com.metrolist.innertube.pages.ChartsPage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** Popular songs for the bottom of the Search tab: in the content country, and in the world. */
@HiltViewModel
class PopularViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        /** The content country's name in the app's language, e.g. "Türkiye". */
        val countryName: String =
            runCatching {
                val appLocale = context.resources.configuration.locales[0]
                Locale("", YouTube.locale.gl).getDisplayCountry(appLocale)
            }.getOrNull().orEmpty().ifBlank { YouTube.locale.gl }

        val countrySongs = MutableStateFlow<List<SongItem>?>(null)
        val worldSongs = MutableStateFlow<List<SongItem>?>(null)

        init {
            viewModelScope.launch(Dispatchers.IO) {
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                val fromCharts =
                    YouTube.getChartsPage().onFailure { reportException(it) }.getOrNull()?.let { page ->
                        // The country's top songs first, then its trending songs.
                        page.sections
                            .sortedBy { section ->
                                when (section.chartType) {
                                    ChartsPage.ChartType.TOP -> 0
                                    ChartsPage.ChartType.TRENDING -> 1
                                    else -> 2
                                }
                            }.flatMap { it.items }
                            .filterIsInstance<SongItem>()
                    }.orEmpty()
                // The Charts page is empty or holds no songs in some countries: then use YouTube
                // Music's own "Top 100 Songs <country>" chart playlist.
                val songs = fromCharts.ifEmpty { countryChartPlaylistSongs() }
                countrySongs.value =
                    songs
                        .distinctBy { it.id }
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .take(20)
            }

            viewModelScope.launch(Dispatchers.IO) {
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                // YouTube Music's "Top 100 Songs Global" chart playlist.
                YouTube.playlist(GLOBAL_TOP_SONGS_PLAYLIST).onSuccess { page ->
                    worldSongs.value =
                        page.songs
                            .distinctBy { it.id }
                            .filterExplicit(hideExplicit)
                            .filterVideoSongs(hideVideoSongs)
                            .take(20)
                }.onFailure {
                    worldSongs.value = emptyList()
                    reportException(it)
                }
            }
        }

        private suspend fun countryChartPlaylistSongs(): List<SongItem> {
            val gl = YouTube.locale.gl
            val englishName = Locale("", gl).getDisplayCountry(Locale.ENGLISH).ifBlank { return emptyList() }
            val playlist =
                YouTube
                    .search("Top 100 Songs $englishName", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST)
                    .getOrNull()
                    ?.items
                    ?.filterIsInstance<com.metrolist.innertube.models.PlaylistItem>()
                    ?.firstOrNull { item ->
                        // Countries can go by another English name ("Türkiye"/"Turkey"), so only
                        // ask for a "Top … Songs" chart made by YouTube Music itself.
                        item.title.contains("Top", ignoreCase = true) &&
                            item.title.contains("Song", ignoreCase = true) &&
                            !item.title.contains("Global", ignoreCase = true) &&
                            item.author?.name?.contains("YouTube", ignoreCase = true) != false
                    } ?: return emptyList()
            return YouTube.playlist(playlist.id).getOrNull()?.songs.orEmpty()
        }

        private companion object {
            const val GLOBAL_TOP_SONGS_PLAYLIST = "PL4fGSI1pDJn6puJdseH2Rt9sMvt9E2M4i"
        }
    }
