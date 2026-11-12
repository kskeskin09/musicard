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
    CARD(R.drawable.ic_logo_card, R.string.logo_card),
    WAVE(R.drawable.ic_logo_wave, R.string.logo_wave),
    VINYL(R.drawable.ic_logo_vinyl, R.string.logo_vinyl),
    NOTES(R.drawable.ic_logo_notes, R.string.logo_notes),
}

/**
 * Colour of the launcher logo. [BLACK] (dark) and [WHITE] (light) are fixed, so the icon looks
 * the same whatever the phone's theme, wallpaper or themed-icons setting is. [DEVICE] follows
 * the wallpaper colours (Android 12+) and the launcher's themed icons.
 */
enum class LogoTheme(
    val background: Color,
    val foreground: Color,
    @StringRes val label: Int,
) {
    BLACK(Color(0xFF000000), Color.White, R.string.logo_theme_dark),
    WHITE(Color.White, Color(0xFF000000), R.string.logo_theme_light),
    DEVICE(Color(0xFF000000), Color.White, R.string.logo_theme_device),
}

object LogoUtils {
    // Aliases are declared in AndroidManifest.xml relative to the namespace, not the application id.
    private const val PACKAGE = "com.metrolist.music"

    private fun aliasName(logo: AppLogo, theme: LogoTheme): String =
        when {
            // Legacy names kept so icons pinned by older builds keep working.
            logo == AppLogo.CARD && theme == LogoTheme.BLACK -> "$PACKAGE.MainActivityAlias"
            logo == AppLogo.CARD && theme == LogoTheme.DEVICE -> "$PACKAGE.MainActivityStatic"
            else -> "$PACKAGE.Logo${logo.name.lowercase().replaceFirstChar { it.uppercase() }}" +
                theme.name.lowercase().replaceFirstChar { it.uppercase() }
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
        AppLogo.entries.forEach { l ->
            LogoTheme.entries.forEach { t ->
                val name = aliasName(l, t)
                if (name != target) {
                    pm.setComponentEnabledSetting(
                        ComponentName(context, name),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP,
                    )
                }
            }
        }
    }
}
