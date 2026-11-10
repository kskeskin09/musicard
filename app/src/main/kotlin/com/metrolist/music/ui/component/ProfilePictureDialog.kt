/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.material3.Slider
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
 * Sets the profile picture from an image link and frames it: pinch or drag the preview, or use
 * the zoom slider. Only the link and the framing are saved (in the app's settings on this device);
 * the picture itself is loaded and cached by the app's image loader.
 */
@Composable
fun ProfilePictureDialog(
    initialUrl: String,
    initialCrop: ProfileCrop,
    onDismiss: () -> Unit,
    onSave: (String, ProfileCrop) -> Unit,
) {
    var url by remember { mutableStateOf(initialUrl) }
    var zoom by remember { mutableFloatStateOf(initialCrop.zoom) }
    var offsetX by remember { mutableFloatStateOf(initialCrop.offsetX) }
    var offsetY by remember { mutableFloatStateOf(initialCrop.offsetY) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_picture)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (url.isNotBlank()) {
                    Box(
                        modifier =
                            Modifier
                                .padding(bottom = 12.dp)
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, gestureZoom, _ ->
                                        zoom = (zoom * gestureZoom).coerceIn(1f, ProfileMaxZoom)
                                        val travelX = (zoom - 1f) * size.width / 2f
                                        val travelY = (zoom - 1f) * size.height / 2f
                                        if (travelX > 0f) offsetX = (offsetX + pan.x / travelX).coerceIn(-1f, 1f)
                                        if (travelY > 0f) offsetY = (offsetY + pan.y / travelY).coerceIn(-1f, 1f)
                                    }
                                },
                    ) {
                        AsyncImage(
                            model = url.trim(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .profileCrop(ProfileCrop(zoom, offsetX, offsetY)),
                        )
                    }
                    Text(
                        text = stringResource(R.string.profile_picture_zoom),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Slider(
                        value = zoom,
                        onValueChange = { zoom = it },
                        valueRange = 1f..ProfileMaxZoom,
                    )
                    Text(
                        text = stringResource(R.string.profile_picture_horizontal),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Slider(
                        value = offsetX,
                        onValueChange = { offsetX = it },
                        valueRange = -1f..1f,
                        enabled = zoom > 1f,
                    )
                    Text(
                        text = stringResource(R.string.profile_picture_vertical),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Slider(
                        value = offsetY,
                        onValueChange = { offsetY = it },
                        valueRange = -1f..1f,
                        enabled = zoom > 1f,
                    )
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.profile_picture_url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(url, ProfileCrop(zoom, offsetX, offsetY)) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
