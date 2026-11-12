/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import com.metrolist.music.ui.component.ThemedSwitch as Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The sheet for making a playlist (or changing one): a cover that can be a photo or a link (with
 * neither, the playlist shows the four-cover art), a title, a description and, for signed-in
 * users, the YouTube sync switch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistEditorSheet(
    onDismiss: () -> Unit,
    onSubmit: (name: String, description: String, coverUrl: String?, sync: Boolean) -> Unit,
    initialName: String = "",
    initialDescription: String = "",
    initialCoverUrl: String? = null,
    isNew: Boolean = true,
    showCover: Boolean = true,
    showSync: Boolean = false,
    onSyncChangeRequest: ((Boolean, (Boolean) -> Unit) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var coverUrl by remember { mutableStateOf(initialCoverUrl?.takeIf { it.isNotBlank() }) }
    var showLinkField by remember { mutableStateOf(false) }
    var linkText by remember { mutableStateOf(initialCoverUrl?.takeIf { it.startsWith("http") }.orEmpty()) }
    var showCoverMenu by remember { mutableStateOf(false) }
    var sync by remember { mutableStateOf(false) }

    val pickPhoto =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val saved = withContext(Dispatchers.IO) { copyPlaylistCover(context, uri) }
                if (saved != null) {
                    coverUrl = saved
                    showLinkField = false
                    linkText = ""
                }
            }
        }

    val fieldColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val onSurface = MaterialTheme.colorScheme.onSurface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
        ) {
            if (showCover) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 4.dp, bottom = if (showLinkField) 12.dp else 28.dp)
                            .size(240.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(fieldColor)
                            .clickable { showCoverMenu = true },
                ) {
                    if (coverUrl != null) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .size(if (coverUrl != null) 52.dp else 76.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = if (coverUrl != null) 0.45f else 0.35f)),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.insert_photo),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(if (coverUrl != null) 24.dp else 32.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = showCoverMenu,
                        onDismissRequest = { showCoverMenu = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.profile_picture_choose)) },
                            leadingIcon = { Icon(painterResource(R.drawable.insert_photo), null) },
                            onClick = {
                                showCoverMenu = false
                                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.profile_picture_link)) },
                            leadingIcon = { Icon(painterResource(R.drawable.link), null) },
                            onClick = {
                                showCoverMenu = false
                                showLinkField = true
                            },
                        )
                        if (coverUrl != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.playlist_cover_remove)) },
                                leadingIcon = { Icon(painterResource(R.drawable.close), null) },
                                onClick = {
                                    showCoverMenu = false
                                    coverUrl = null
                                    linkText = ""
                                    showLinkField = false
                                },
                            )
                        }
                    }
                }
                if (showLinkField) {
                    SheetField(
                        value = linkText,
                        onValueChange = {
                            linkText = it
                            coverUrl = it.trim().takeIf { url -> url.isNotEmpty() }
                        },
                        placeholder = stringResource(R.string.profile_picture_url_hint),
                        color = fieldColor,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            SheetLabel(stringResource(R.string.playlist_title_label))
            SheetField(
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.playlist_title_hint),
                color = fieldColor,
            )
            Spacer(Modifier.height(24.dp))
            SheetLabel(stringResource(R.string.playlist_description_label))
            SheetField(
                value = description,
                onValueChange = { description = it },
                placeholder = stringResource(R.string.playlist_description_hint),
                color = fieldColor,
                singleLine = false,
            )

            if (showSync) {
                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = fieldColor,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = null,
                            tint = onSurface,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.sync_playlist),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = onSurface,
                            )
                            Text(
                                text = stringResource(R.string.allows_for_sync_witch_youtube),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = sync,
                            onCheckedChange = { wanted ->
                                val request = onSyncChangeRequest
                                if (request == null) sync = wanted else request(wanted) { allowed -> if (allowed) sync = wanted }
                            },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                                    checkedTrackColor = onSurface,
                                    uncheckedThumbColor = onSurface.copy(alpha = 0.6f),
                                    uncheckedTrackColor = onSurface.copy(alpha = 0.1f),
                                    uncheckedBorderColor = onSurface.copy(alpha = 0.3f),
                                ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            val canSubmit = name.isNotBlank()
            Surface(
                onClick = {
                    if (canSubmit) {
                        onSubmit(name.trim(), description.trim(), coverUrl?.trim(), sync)
                        onDismiss()
                    }
                },
                enabled = canSubmit,
                shape = CircleShape,
                color = if (canSubmit) onSurface else fieldColor,
                contentColor = if (canSubmit) MaterialTheme.colorScheme.surface else onSurface.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(if (isNew) R.string.create_playlist else R.string.save),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
    )
}

@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    color: Color,
    singleLine: Boolean = true,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor, fontSize = 17.sp),
        cursorBrush = SolidColor(textColor),
        decorationBox = { inner ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(color)
                        .padding(horizontal = 22.dp, vertical = 16.dp),
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                        color = textColor.copy(alpha = 0.45f),
                    )
                }
                inner()
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Copies a picked photo into the app's files and returns its file:// address. */
private fun copyPlaylistCover(
    context: Context,
    uri: Uri,
): String? =
    runCatching {
        val dir = File(context.filesDir, "playlist_covers").apply { mkdirs() }
        val file = File(dir, "cover_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        Uri.fromFile(file).toString()
    }.getOrNull()
