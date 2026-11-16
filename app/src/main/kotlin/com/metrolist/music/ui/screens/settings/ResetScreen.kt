/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.AppReset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ResetKind { CONTENTS, SETTINGS, EVERYTHING }

/** The warning shown each time Reset is tapped, before the reset options open. */
@Composable
fun ResetWarningDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    DefaultDialog(
        onDismiss = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.warning),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(R.string.reset_warning_title)) },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reset_warning_back))
            }
            TextButton(onClick = onContinue) {
                Text(
                    stringResource(R.string.reset_warning_continue),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    ) {
        Text(
            text = stringResource(R.string.reset_warning_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by rememberSaveable { mutableStateOf<ResetKind?>(null) }
    var running by remember { mutableStateOf(false) }

    fun item(
        kind: ResetKind,
        title: Int,
        desc: Int,
        icon: Int,
    ) = Material3SettingsItem(
        leadingContent = {
            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        title = { Text(stringResource(title), color = MaterialTheme.colorScheme.error) },
        description = { Text(stringResource(desc)) },
        enabled = !running,
        onClick = { if (!running) pending = kind },
    )

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
                    item(ResetKind.CONTENTS, R.string.reset_contents, R.string.reset_contents_desc, R.drawable.library_music),
                    item(ResetKind.SETTINGS, R.string.reset_settings, R.string.reset_settings_desc, R.drawable.tune),
                    item(ResetKind.EVERYTHING, R.string.reset_everything, R.string.reset_everything_desc, R.drawable.delete),
                ),
        )
        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.reset_app)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )

    // A short, plain "Reset ...?" check before each action.
    pending?.let { kind ->
        val question =
            when (kind) {
                ResetKind.CONTENTS -> R.string.reset_contents_confirm
                ResetKind.SETTINGS -> R.string.reset_settings_confirm
                ResetKind.EVERYTHING -> R.string.reset_everything_confirm
            }
        // A standard full-width Material confirm dialog: title and two buttons, no long text.
        AlertDialog(
            onDismissRequest = { pending = null },
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.warning),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(stringResource(question)) },
            dismissButton = {
                TextButton(onClick = { pending = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pending = null
                        if (running) return@TextButton
                        running = true
                        scope.launch(Dispatchers.IO) {
                            val ok =
                                when (kind) {
                                    ResetKind.CONTENTS -> AppReset.resetContents(context.applicationContext)
                                    ResetKind.SETTINGS -> AppReset.resetSettings(context.applicationContext)
                                    ResetKind.EVERYTHING -> AppReset.resetEverything(context.applicationContext)
                                }
                            // On success the app restarts or closes; only a failure gets here.
                            if (!ok) {
                                withContext(Dispatchers.Main) {
                                    running = false
                                    Toast.makeText(context, R.string.reset_failed, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.reset_action), color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }

    if (running) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        ) {
            androidx.compose.material3.Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.reset_in_progress))
                }
            }
        }
    }
}
