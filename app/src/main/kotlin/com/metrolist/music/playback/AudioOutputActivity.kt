/**
 * Musicard (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.ui.component.AudioOutputSelector
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.theme.MetrolistTheme
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

/**
 * The audio output sheet the notification's "Audio output" button opens. It is the same picker as
 * in the player's menu (System auto, the phone, the connected headphones), in the app's own colors,
 * and it also shows over the lock screen. Tapping outside or "OK" closes it.
 */
class AudioOutputActivity : ComponentActivity() {
    private var service by mutableStateOf<MusicService?>(null)
    private var bound = false

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?,
            ) {
                service = (binder as? MusicService.MusicBinder)?.service
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                service = null
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) setShowWhenLocked(true)
        bound = bindService(Intent(this, MusicService::class.java), connection, 0)
        if (!bound) {
            finish()
            return
        }
        setContent {
            val darkMode by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
            val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
            val systemDark = isSystemInDarkTheme()
            val dark = if (darkMode == DarkMode.AUTO) systemDark else darkMode == DarkMode.ON
            MetrolistTheme(darkTheme = dark, pureBlack = pureBlackEnabled && dark) {
                AudioOutputSheet(service = service, onClose = ::finish)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isShowing = true
        // The sheet is open: the "tap to choose" prompt is no longer needed.
        androidx.core.app.NotificationManagerCompat.from(this).cancel(MusicService.AUDIO_OUTPUT_NOTIFICATION_ID)
    }

    override fun onPause() {
        super.onPause()
        isShowing = false
    }

    override fun onStop() {
        super.onStop()
        // Like a dialog: once it leaves the screen, it is done.
        if (!isChangingConfigurations) finish()
    }

    override fun onDestroy() {
        if (bound) runCatching { unbindService(connection) }
        bound = false
        super.onDestroy()
    }

    companion object {
        /** True while the sheet is on screen (the service uses it to know the sheet really opened). */
        @Volatile
        var isShowing: Boolean = false
            private set

        fun intent(context: Context): Intent =
            Intent(context, AudioOutputActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}

@Composable
private fun AudioOutputSheet(
    service: MusicService?,
    onClose: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(260)) { it / 3 } + fadeIn(tween(200)),
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        // Taps on the sheet itself don't close it.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                NowPlayingHeader(service)
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.audio_output_auto),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.audio_output),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(14.dp))
                val player = service?.player
                if (player != null) {
                    AudioOutputSelector(player = player)
                } else {
                    Spacer(Modifier.height(66.dp))
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onClose,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface,
                            ),
                    ) {
                        Text(stringResource(android.R.string.ok), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingHeader(service: MusicService?) {
    val metadata = service?.currentMediaMetadata?.collectAsState()?.value
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            if (metadata?.thumbnailUrl != null) {
                AsyncImage(
                    model = metadata.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = metadata?.title ?: stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val artists = metadata?.artists?.joinToString { it.name }
            if (!artists.isNullOrBlank()) {
                Text(
                    text = artists,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
