/**
 * Musicard (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.ReleaseInfo
import com.metrolist.music.utils.Updater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Opened from "New version available" on the Profile tab:
 * "Download and update" at the top, the release notes from GitHub under it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var release by remember { mutableStateOf(Updater.getCachedLatestRelease()) }
    var loading by remember { mutableStateOf(release == null) }
    var progress by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        if (release == null) {
            release = Updater.getLatestRelease().getOrNull()
            loading = false
        }
    }

    val current = release
    val downloadUrl = current?.let { Updater.getDownloadUrlForCurrentVariant(it) }
    val failedText = stringResource(R.string.update_download_failed)

    fun openReleasePage() {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, (downloadUrl ?: Updater.LATEST_RELEASE_PAGE).toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: ActivityNotFoundException) {
        }
    }

    fun startDownload() {
        val url = downloadUrl ?: return openReleasePage()
        if (progress != null) return
        // Android 8+ asks once to allow installs from Musicard.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                return
            } catch (_: ActivityNotFoundException) {
                // Fall through; the installer asks by itself on some devices.
            }
        }
        scope.launch {
            progress = 0
            val apk =
                try {
                    downloadApk(context, url) { progress = it }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            progress = null
            if (apk != null) {
                installApk(context, apk)
            } else {
                Toast.makeText(context, failedText, Toast.LENGTH_LONG).show()
                openReleasePage()
            }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        Material3SettingsGroup(
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.download),
                        title = {
                            Text(
                                if (progress != null) {
                                    stringResource(R.string.update_downloading, progress ?: 0)
                                } else {
                                    stringResource(R.string.download_and_update)
                                },
                            )
                        },
                        description = {
                            Column {
                                Text(
                                    stringResource(
                                        R.string.download_and_update_desc,
                                        current?.versionName ?: "…",
                                    ),
                                )
                                Text(stringResource(R.string.current_version_label, BuildConfig.BASE_VERSION_NAME))
                                progress?.let { percent ->
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { percent / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        },
                        isHighlighted = true,
                        enabled = current != null,
                        onClick = { startDownload() },
                    ),
                ),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.update_notes),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
        ) {
            when {
                loading -> CircularProgressIndicator(strokeWidth = 2.dp)
                current == null -> Text(stringResource(R.string.update_check_failed))
                else -> ReleaseNotesText(current)
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.update_available_title)) },
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

/** Release notes are GitHub Markdown; show headings bold and list items with a bullet. */
@Composable
private fun ReleaseNotesText(release: ReleaseInfo) {
    val lines =
        release.description
            .lines()
            .map { it.trimEnd() }
            .filterNot { it.startsWith("Released by @") }
            .dropWhile { it.isBlank() }
    if (lines.all { it.isBlank() }) {
        Text(stringResource(R.string.update_notes_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    lines.forEach { raw ->
        val line = raw.replace("**", "").replace("`", "")
        when {
            line.isBlank() -> Spacer(Modifier.height(6.dp))
            line.startsWith("#") ->
                Text(
                    text = line.trimStart('#').trim(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") ->
                Text(
                    text = "•  " + line.trimStart().drop(2),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
            else -> Text(line, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private suspend fun downloadApk(
    context: Context,
    url: String,
    onProgress: (Int) -> Unit,
): File? =
    withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "Musicard-update.apk")
        OkHttpClient().newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body ?: return@withContext null
            val total = body.contentLength()
            var lastPercent = -1
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                withContext(Dispatchers.Main) { onProgress(percent) }
                            }
                        }
                    }
                }
            }
        }
        target.takeIf { it.length() > 0 }
    }

private fun installApk(context: Context, apk: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", apk)
    val intent =
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
