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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.music.R

/** How the profile picture is framed in its circle: zoom (1..3) and the shift of the visible part (-1..1). */
data class ProfileCrop(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

const val ProfileMaxZoom = 3f

/**
 * Applies [crop] to a picture filling a round box. Put it on the image; the box around it clips.
 * At the largest shift the picture's edge meets the circle's edge, so the circle is never empty.
 */
fun Modifier.profileCrop(crop: ProfileCrop): Modifier =
    graphicsLayer {
        scaleX = crop.zoom
        scaleY = crop.zoom
        translationX = crop.offsetX * (crop.zoom - 1f) * size.width / 2f
        translationY = crop.offsetY * (crop.zoom - 1f) * size.height / 2f
    }

/**
 * Sets the profile picture and frames it like a photo crop: pick a photo from the gallery (or
 * paste an image link), then pinch to zoom and drag it inside the round frame. The picked photo
 * is copied into the app's own storage on this device; only its location and the framing are
 * saved in the settings.
 */
@Composable
fun ProfilePictureDialog(
    initialUrl: String,
    initialCrop: ProfileCrop,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String, ProfileCrop, String) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(initialUrl) }
    var zoom by remember { mutableFloatStateOf(initialCrop.zoom) }
    var offsetX by remember { mutableFloatStateOf(initialCrop.offsetX) }
    var offsetY by remember { mutableFloatStateOf(initialCrop.offsetY) }
    var showLinkField by remember { mutableStateOf(initialUrl.isNotBlank() && !initialUrl.startsWith("file:")) }
    val pickPhoto =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val saved = withContext(Dispatchers.IO) { copyProfilePhoto(context, uri) }
                if (saved != null) {
                    url = saved
                    zoom = 1f
                    offsetX = 0f
                    offsetY = 0f
                }
            }
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_picture)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .padding(bottom = 16.dp)
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(enabled = url.isBlank()) {
                                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }.pointerInput(Unit) {
                                detectTransformGestures { _, pan, gestureZoom, _ ->
                                    zoom = (zoom * gestureZoom).coerceIn(1f, ProfileMaxZoom)
                                    val travelX = (zoom - 1f) * size.width / 2f
                                    val travelY = (zoom - 1f) * size.height / 2f
                                    offsetX = if (travelX > 0f) (offsetX + pan.x / travelX).coerceIn(-1f, 1f) else 0f
                                    offsetY = if (travelY > 0f) (offsetY + pan.y / travelY).coerceIn(-1f, 1f) else 0f
                                }
                            },
                ) {
                    if (url.isNotBlank()) {
                        AsyncImage(
                            model = url.trim(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .profileCrop(ProfileCrop(zoom, offsetX, offsetY)),
                        )
                        // Darkens everything outside the round frame, like a crop screen.
                        Canvas(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
                        ) {
                            drawRect(Color.Black.copy(alpha = 0.55f))
                            drawCircle(Color.Transparent, radius = size.minDimension / 2f, blendMode = BlendMode.Clear)
                            drawCircle(Color.White.copy(alpha = 0.9f), radius = size.minDimension / 2f - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(72.dp),
                        )
                    }
                }
                if (url.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.profile_picture_crop_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    ) {
                        Text(stringResource(R.string.profile_picture_choose))
                    }
                    TextButton(onClick = { showLinkField = !showLinkField }) {
                        Text(stringResource(R.string.profile_picture_link))
                    }
                }
                // Shown as the creator of your playlists and used in Listen Together.
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.profile_username)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (showLinkField) {
                    OutlinedTextField(
                        value = if (url.startsWith("file:")) "" else url,
                        onValueChange = {
                            url = it
                            zoom = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.profile_picture_url_hint)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(url, ProfileCrop(zoom, offsetX, offsetY), name.trim()) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

/** Copies the picked photo into the app's files (replacing the previous one); returns its file:// address. */
private fun copyProfilePhoto(
    context: Context,
    uri: Uri,
): String? =
    runCatching {
        val dir = File(context.filesDir, "profile").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        // A new name each time, so the image loader doesn't show the old cached picture.
        val file = File(dir, "profile_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        Uri.fromFile(file).toString()
    }.getOrNull()
