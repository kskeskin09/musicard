/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.DiscordAvatarKey
import com.metrolist.music.constants.DiscordNameKey
import com.metrolist.music.constants.DiscordUsernameKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.LastFMSessionKey
import com.metrolist.music.constants.LastFMUsernameKey
import com.metrolist.music.constants.LastSeenVersionKey
import com.metrolist.music.constants.ListenTogetherSessionTimestampKey
import com.metrolist.music.constants.ListenTogetherSessionTokenKey
import com.metrolist.music.constants.ProfileImageOffsetXKey
import com.metrolist.music.constants.ProfileImageOffsetYKey
import com.metrolist.music.constants.ProfileImageUrlKey
import com.metrolist.music.constants.ProfileImageZoomKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.db.InternalDatabase
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.MusicService.Companion.PERSISTENT_AUTOMIX_FILE
import com.metrolist.music.playback.MusicService.Companion.PERSISTENT_PLAYER_STATE_FILE
import com.metrolist.music.playback.MusicService.Companion.PERSISTENT_QUEUE_FILE
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * The three reset actions under Import or Export → Reset.
 *
 * - Contents: the music database (songs, playlists, library, history, lyrics), downloads,
 *   the song cache, image cache and other content files. Settings and sign-ins stay.
 *   Files are deleted on the next start, before anything opens them, so nothing is
 *   deleted from under an open database or cache.
 * - Settings: every setting back to its default. Sign-ins, the profile picture and the
 *   first-run setup state stay.
 * - Everything: Android's own "Clear storage", so the app is as after a fresh install.
 */
object AppReset {
    private const val TAG = "AppReset"
    private const val PENDING_CONTENT_RESET_FILE = "pending_content_reset"

    // Media3's StandaloneDatabaseProvider file: the download index and cache indexes.
    private const val MEDIA3_DATABASE_NAME = "exoplayer_internal.db"

    // Settings that are not preferences: sign-ins, profile and first-run state.
    // The onboarding keys are named by text so this also works before that screen exists.
    private val keptSettingNames: Set<String> =
        setOf(
            VisitorDataKey.name,
            DataSyncIdKey.name,
            InnerTubeAuthUserKey.name,
            InnerTubeCookieKey.name,
            AccountNameKey.name,
            AccountEmailKey.name,
            AccountChannelHandleKey.name,
            LastFMSessionKey.name,
            LastFMUsernameKey.name,
            DiscordUsernameKey.name,
            DiscordNameKey.name,
            DiscordAvatarKey.name,
            ListenTogetherSessionTokenKey.name,
            ListenTogetherSessionTimestampKey.name,
            ProfileImageUrlKey.name,
            ProfileImageZoomKey.name,
            ProfileImageOffsetXKey.name,
            ProfileImageOffsetYKey.name,
            LastSeenVersionKey.name,
            "onboardingDone",
            "onboardingPending",
        )

    // Folders under filesDir that hold content.
    private val contentDirs =
        listOf(
            "download", // downloaded songs
            "exoplayer", // song cache
            "playlist_covers",
            "database_backups",
            "autoeq_cache",
        )

    // Files under filesDir that hold content: the saved queue and the last sync's snapshots.
    private val contentFiles =
        listOf(
            PERSISTENT_QUEUE_FILE,
            PERSISTENT_AUTOMIX_FILE,
            PERSISTENT_PLAYER_STATE_FILE,
            "sync_snapshot_liked_songs",
            "sync_snapshot_library_songs",
            "sync_snapshot_liked_albums",
        )

    // SharedPreferences files that hold content rather than settings.
    private val contentPrefs =
        listOf(
            "playlist_descriptions",
            "page_colors_v2",
            "artist_name_aliases",
            "lyrics_translation_choices",
        )

    /**
     * Runs a content reset asked for before the last restart. Must run before Room,
     * the caches or SharedPreferences are opened, i.e. from Application.attachBaseContext.
     */
    fun runPendingContentReset(context: Context) {
        val marker = File(context.filesDir, PENDING_CONTENT_RESET_FILE)
        if (!marker.exists()) return
        Log.i(TAG, "Running pending content reset")

        step("music database") { context.deleteDatabase(InternalDatabase.DB_NAME) }
        step("download index") { context.deleteDatabase(MEDIA3_DATABASE_NAME) }
        contentDirs.forEach { name -> step(name) { File(context.filesDir, name).deleteRecursively() } }
        contentFiles.forEach { name -> step(name) { File(context.filesDir, name).delete() } }
        contentPrefs.forEach { name -> step(name) { context.deleteSharedPreferences(name) } }
        step("cache") { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
        step("external cache") { context.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() } }

        // Last, so a reset cut short by a crash runs again on the next start.
        marker.delete()
    }

    private inline fun step(
        name: String,
        block: () -> Unit,
    ) {
        runCatching(block).onFailure { Log.e(TAG, "Content reset step failed: $name", it) }
    }

    /** Stops playback, marks the contents for deletion and restarts the app, which deletes them. */
    suspend fun resetContents(context: Context): Boolean {
        stopPlayback(context)
        val marker = File(context.filesDir, PENDING_CONTENT_RESET_FILE)
        val marked = runCatching { marker.createNewFile() || marker.exists() }.getOrDefault(false)
        if (!marked) return false
        restartApp(context)
        return true
    }

    /** Puts every setting back to its default, keeping sign-ins, then restarts the app. */
    suspend fun resetSettings(context: Context): Boolean {
        stopPlayback(context)
        val ok =
            runCatching {
                File(context.filesDir, "datastore").mkdirs()
                context.dataStore.edit { prefs ->
                    val kept = prefs.asMap().filterKeys { it.name in keptSettingNames }
                    prefs.clear()
                    kept.forEach { (key, value) ->
                        @Suppress("UNCHECKED_CAST")
                        prefs[key as Preferences.Key<Any>] = value
                    }
                }
                // The chosen equalizer preset lives outside the settings file. Saved presets stay.
                context
                    .getSharedPreferences("nanosonic_eq_profiles", Context.MODE_PRIVATE)
                    .edit()
                    .remove("active_profile_id")
                    .commit()
            }.onFailure { Log.e(TAG, "Settings reset failed", it) }.isSuccess
        if (!ok) return false
        restartApp(context)
        return true
    }

    /** Clears all app data, as Android's "Clear storage" does. The system closes the app. */
    suspend fun resetEverything(context: Context): Boolean {
        stopPlayback(context)
        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return false
        return runCatching { activityManager.clearApplicationUserData() }.getOrDefault(false)
    }

    private suspend fun stopPlayback(context: Context) {
        if (!MusicService.isRunning) return
        runCatching { context.stopService(Intent(context, MusicService::class.java)) }
        withTimeoutOrNull(5000) { MusicService.shutdownDeferred.await() }
    }

    private fun restartApp(context: Context) {
        val intent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        if (intent != null) context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
