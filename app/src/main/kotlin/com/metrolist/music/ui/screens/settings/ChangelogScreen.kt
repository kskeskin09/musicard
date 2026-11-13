/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.utils.ReleaseInfo
import com.metrolist.music.utils.Updater

private val markdownLinkRegex = Regex("(@[a-zA-Z0-9_-]+)|(https?://[\\w-]+(\\.[\\w-]+)+[\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])")
private val boldRegex = Regex("\\*\\*(.+?)\\*\\*")

/**
 * Release notes sheet in Musicard's settings style: the same background as menus, one rounded
 * card per release with a small version label above it (like a settings group title).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(
    onDismiss: () -> Unit
) {
    var releases by remember { mutableStateOf<List<ReleaseInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        Updater.getAllReleases().onSuccess { allReleases ->
            releases = allReleases.filter { release ->
                Updater.compareVersions(BuildConfig.BASE_VERSION_NAME, release.tagName) >= 0
            }
            isLoading = false
        }.onFailure {
            isLoading = false
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val githubReleasesUrl = stringResource(R.string.github_releases_url)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.release_notes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(onClick = { uriHandler.openUri(githubReleasesUrl) }) {
                        Icon(
                            painter = painterResource(R.drawable.github),
                            contentDescription = stringResource(R.string.view_on_github),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            when {
                isLoading -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                releases.isEmpty() -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.changelog_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> itemsIndexed(releases) { index, release ->
                    ReleaseItem(release, isCurrent = index == 0)
                }
            }
        }
    }
}

@Composable
fun ReleaseItem(release: ReleaseInfo, isCurrent: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = release.tagName,
                style = MaterialTheme.typography.labelLarge,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = release.releaseDate.split("T").firstOrNull() ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                MarkdownText(release.description)
            }
        }
    }
}

/**
 * Renders the small Markdown subset used in Musicard's release notes (changelog.md): "## Section"
 * headings, "- item" bullets, **bold** and links. The top "# Musicard x.y.z" title is skipped
 * because the version is already shown above the card.
 */
@Suppress("DEPRECATION")
@Composable
fun MarkdownText(text: String) {
    val uriHandler = LocalUriHandler.current
    val primary = MaterialTheme.colorScheme.primary
    val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("# ") && it != "#" }

    if (lines.isEmpty()) {
        Text(
            text = stringResource(R.string.changelog_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEachIndexed { index, line ->
            if (line.startsWith("#")) {
                val headerText = line.trimStart('#').trim()
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = primary,
                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 10.dp, bottom = 2.dp)
                )
            } else {
                val isListItem = line.startsWith("- ") || line.startsWith("* ")
                val contentText = if (isListItem) line.substring(2).trim() else line
                val annotated = inlineMarkdown(contentText, primary)

                Row(modifier = Modifier.fillMaxWidth()) {
                    if (isListItem) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 12.dp)
                                .size(5.dp)
                                .background(primary.copy(alpha = 0.8f), CircleShape)
                        )
                    }
                    ClickableText(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { uriHandler.openUri(it.item) }
                        }
                    )
                }
            }
        }
    }
}

private fun inlineMarkdown(text: String, linkColor: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        // Split on **bold** first, then mark links/handles inside each piece.
        var cursor = 0
        val pieces = mutableListOf<Pair<String, Boolean>>()
        boldRegex.findAll(text).forEach { m ->
            if (m.range.first > cursor) pieces += text.substring(cursor, m.range.first) to false
            pieces += m.groupValues[1] to true
            cursor = m.range.last + 1
        }
        if (cursor < text.length) pieces += text.substring(cursor) to false

        pieces.forEach { (piece, bold) ->
            if (bold) pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
            var last = 0
            markdownLinkRegex.findAll(piece).forEach { result ->
                append(piece.substring(last, result.range.first))
                val match = result.value
                val link = if (match.startsWith("@")) "https://github.com/${match.substring(1)}" else match
                pushStringAnnotation(tag = "URL", annotation = link)
                withStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = if (match.startsWith("@")) TextDecoration.None else TextDecoration.Underline
                    )
                ) { append(match) }
                pop()
                last = result.range.last + 1
            }
            append(piece.substring(last))
            if (bold) pop()
        }
    }
