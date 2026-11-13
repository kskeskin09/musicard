package com.metrolist.music.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.metrolist.music.R

/** The launcher logos the user can pick in Settings > Appearance > Logo. */
enum class AppLogo(
    @DrawableRes val glyph: Int,
    @StringRes val label: Int,
) {
    SINGLE_NOTE(R.drawable.ic_logo_single, R.string.logo_single_note),
    DOUBLE_NOTE(R.drawable.ic_logo_double, R.string.logo_double_note),
    WAVE(R.drawable.ic_logo_wave, R.string.logo_wave),
    VINYL(R.drawable.ic_logo_vinyl, R.string.logo_vinyl),
}

/**
 * Colour of the launcher logo. Dark and Light are fixed colours with no monochrome layer, so the
 * icon looks the same whatever the phone's theme, wallpaper or themed-icons setting is. System
 * uses night-qualified colour resources, so the launcher shows Light or Dark to match the phone's
 * dark mode.
 */
enum class LogoTheme(
    @StringRes val label: Int,
) {
    BLACK(R.string.logo_theme_dark),
    WHITE(R.string.logo_theme_light),
    SYSTEM(R.string.logo_theme_system),
    ;

    /** Tile and glyph colours as the launcher shows them, given whether the phone is in dark mode. */
    fun colors(systemDark: Boolean): Pair<Color, Color> {
        val dark = this == BLACK || (this == SYSTEM && systemDark)
        return if (dark) Color(0xFF000000) to Color.White else Color.White to Color(0xFF000000)
    }
}

// Matches the alias names in AndroidManifest.xml, e.g. .LogoNotesBlack. The names come from
// earlier builds (Card was the single note's old name, Notes the double note's) so the enabled
// alias carries over on update.
private val AppLogo.aliasPart: String
    get() = when (this) {
        AppLogo.SINGLE_NOTE -> "Card"
        AppLogo.DOUBLE_NOTE -> "Notes"
        AppLogo.WAVE -> "Wave"
        AppLogo.VINYL -> "Vinyl"
    }

object LogoUtils {
    // Aliases are declared in AndroidManifest.xml relative to the namespace, not the application id.
    private const val PACKAGE = "com.metrolist.music"

    // Retired "device colours" aliases; always disabled once the user picks a logo.
    private val retiredAliases =
        listOf("MainActivityStatic", "LogoNotesDevice", "LogoWaveDevice", "LogoVinylDevice").map { "$PACKAGE.$it" }

    private fun aliasName(logo: AppLogo, theme: LogoTheme): String =
        when {
            // Legacy name kept so icons pinned by older builds keep working.
            logo == AppLogo.SINGLE_NOTE && theme == LogoTheme.BLACK -> "$PACKAGE.MainActivityAlias"
            else -> "$PACKAGE.Logo${logo.aliasPart}${theme.name.lowercase().replaceFirstChar { it.uppercase() }}"
        }

    /** Shows [logo] in [theme] on the home screen and hides every other launcher alias. */
    fun setLogo(context: Context, logo: AppLogo, theme: LogoTheme) {
        val pm = context.packageManager
        val target = aliasName(logo, theme)
        // Enable the new one first so the app never has zero launcher entries.
        pm.setComponentEnabledSetting(
            ComponentName(context, target),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        val others = AppLogo.entries.flatMap { l -> LogoTheme.entries.map { t -> aliasName(l, t) } } + retiredAliases
        others.filter { it != target }.forEach { name ->
            pm.setComponentEnabledSetting(
                ComponentName(context, name),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
