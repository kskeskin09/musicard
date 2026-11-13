/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns a color taken from cover art into a calm background color: less saturated, and kept in a
 * lightness range that suits the theme (a little darker in dark mode, a little lighter in light
 * mode). White text stays readable on both.
 */
fun albumBackgroundColor(
    base: Color,
    darkTheme: Boolean,
): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(base.toArgb(), hsl)
    hsl[1] = (hsl[1] * 0.8f).coerceAtMost(0.55f)
    hsl[2] = if (darkTheme) hsl[2].coerceIn(0.18f, 0.28f) else hsl[2].coerceIn(0.34f, 0.44f)
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * Player buttons in "Background color" mode: a light tint of the song's color for the filled
 * buttons and a deep shade of it for their icons. Light enough to stand out on the album
 * background, the glass cover and the light theme alike.
 */
fun playerButtonAccent(base: Color): Pair<Color, Color> {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(base.toArgb(), hsl)
    val saturation = (hsl[1] * 0.9f).coerceIn(0.25f, 0.6f)
    val light = Color(ColorUtils.HSLToColor(floatArrayOf(hsl[0], saturation, 0.8f)))
    val deep = Color(ColorUtils.HSLToColor(floatArrayOf(hsl[0], saturation, 0.16f)))
    return light to deep
}

/** Colors already read from covers, so a page opened again (or prefetched) starts in its color. */
private val coverColorCache = android.util.LruCache<String, Color>(200)

/** The dominant color of the image at [url], or null while it loads or if it can't be read. */
@Composable
fun rememberCoverColor(url: String?): Color? {
    val context = LocalContext.current
    var color by remember(url) { mutableStateOf(url?.let { coverColorCache.get(it) }) }
    LaunchedEffect(url) {
        if (url == null || color != null) return@LaunchedEffect
        color = loadCoverColor(context, url)
    }
    return color
}

/**
 * Like [rememberCoverColor], for the color a whole page is tinted with. A page usually learns its
 * cover only after its data has loaded, and then the cover still has to be read, so the color the
 * page had last time is saved (per page, and per [variant] of it such as a chosen period) and
 * returned until the current one is known. The page then opens in its color right away.
 */
@Composable
fun rememberPageCoverColor(
    url: String?,
    variant: String? = null,
): Color? {
    val context = LocalContext.current
    val loaded = rememberCoverColor(url)
    val entry = LocalViewModelStoreOwner.current as? NavBackStackEntry
    val pageKey =
        remember(entry, variant) {
            entry?.let {
                buildString {
                    append(it.destination.route)
                    it.destination.arguments.keys.forEach { name ->
                        @Suppress("DEPRECATION")
                        append('|').append(it.arguments?.get(name))
                    }
                    if (variant != null) append('#').append(variant)
                }
            }
        }
    var last by remember(pageKey) {
        mutableStateOf(
            pageKey?.let { key ->
                val prefs = context.getSharedPreferences(PAGE_COLOR_PREFS, Context.MODE_PRIVATE)
                if (prefs.contains(key)) Color(prefs.getInt(key, 0)) else null
            },
        )
    }
    LaunchedEffect(pageKey, loaded) {
        if (pageKey == null || loaded == null || loaded == last) return@LaunchedEffect
        last = loaded
        val prefs = context.getSharedPreferences(PAGE_COLOR_PREFS, Context.MODE_PRIVATE)
        prefs
            .edit()
            .apply { if (prefs.all.size > PAGE_COLOR_LIMIT) clear() }
            .putInt(pageKey, loaded.toArgb())
            .apply()
    }
    return loaded ?: last
}

private const val PAGE_COLOR_PREFS = "page_colors"
private const val PAGE_COLOR_LIMIT = 400

/** Reads the cover color of [url] in the background so a page opened from this card starts tinted. */
@Composable
fun PrefetchCoverColor(url: String?) {
    val context = LocalContext.current
    LaunchedEffect(url) {
        if (url == null || coverColorCache.get(url) != null) return@LaunchedEffect
        loadCoverColor(context, url)
    }
}

/**
 * The page tint for [target]: jumps straight to the color when it is known right away (or within
 * a moment of opening the page), and only fades when the color arrives later.
 */
@Composable
fun rememberPageTint(target: Color, ready: Boolean): Color {
    // While the page is opening the tint follows the target directly; after that it fades.
    var fading by remember { mutableStateOf(false) }
    val animatable = remember { androidx.compose.animation.Animatable(target) }
    val latestTarget by androidx.compose.runtime.rememberUpdatedState(target)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(450)
        if (!fading) {
            animatable.snapTo(latestTarget)
            fading = true
        }
    }
    LaunchedEffect(target, ready) {
        if (!fading) {
            if (ready) {
                animatable.snapTo(target)
                fading = true
            }
            return@LaunchedEffect
        }
        animatable.animateTo(target, androidx.compose.animation.core.tween(600))
    }
    return if (fading) animatable.value else target
}

private suspend fun loadCoverColor(
    context: Context,
    url: String,
): Color? {
    val request =
        ImageRequest
            .Builder(context)
            .data(url)
            .size(100, 100)
            .allowHardware(false)
            .build()
    val bitmap = runCatching { context.imageLoader.execute(request) }.getOrNull()?.image?.toBitmap() ?: return null
    return withContext(Dispatchers.Default) {
        val palette =
            Palette
                .from(bitmap)
                .maximumColorCount(8)
                .resizeBitmapArea(100 * 100)
                .generate()
        Color(palette.rankedColors(1, android.graphics.Color.DKGRAY).first())
    }.also { coverColorCache.put(url, it) }
}

/**
 * The even page color taken from a cover: a calm version of it, deep enough in dark mode and a
 * clear wash in light mode, with the text still readable on top.
 */
fun pageTintFor(
    cover: Color?,
    pageBackground: Color,
): Color {
    cover ?: return pageBackground
    val dark = pageBackground.luminance() < 0.5f
    return if (dark) {
        albumBackgroundColor(cover, true)
    } else {
        lerp(pageBackground, albumBackgroundColor(cover, false), 0.42f)
    }
}

/** The page color for the cover at [url], ready on the first frame when it is already known. */
@Composable
fun rememberCoverPageTint(url: String?): Color {
    val pageBackground = MaterialTheme.colorScheme.background
    val cover = rememberPageCoverColor(url)
    return rememberPageTint(target = pageTintFor(cover, pageBackground), ready = cover != null)
}

/** A step lighter (dark mode) or darker (light mode) than [base], clearly set apart from it. */
fun raisedPageColor(base: Color): Color =
    if (base.luminance() < 0.5f) lerp(base, Color.White, 0.14f) else lerp(base, Color.Black, 0.09f)

/** The raised color of the page the content sits on; set by pages that are tinted. */
val LocalPageRaisedColor = compositionLocalOf<Color?> { null }

/** Color of the playing song's row and of the round buttons on a page. */
@Composable
fun pageRaisedColor(): Color = LocalPageRaisedColor.current ?: raisedPageColor(MaterialTheme.colorScheme.background)

/** Gives everything inside the raised color that belongs to a page tinted [tint]. */
@Composable
fun TintedPageColors(
    tint: Color,
    content: @Composable () -> Unit,
) {
    PageFadeColors.Register(tint)
    CompositionLocalProvider(LocalPageRaisedColor provides raisedPageColor(tint), content = content)
}

/**
 * The color of the cover-tinted page on screen, so the fade toward the tab bar and the mini
 * player can use it instead of the plain background. The page shown last wins while two pages
 * are on screen during a page change.
 */
object PageFadeColors {
    private class Entry(color: Color) {
        var color by mutableStateOf(color)

        // Set once the page starts going away (it was shown and is now being left), so the
        // colors switch back right as the page change starts instead of after it ends.
        var leaving by mutableStateOf(false)
    }

    private val entries = mutableStateListOf<Entry>()

    val current: Color?
        get() = entries.lastOrNull { !it.leaving }?.color

    @Composable
    fun Register(tint: Color) {
        val entry = remember { Entry(tint) }
        SideEffect { entry.color = tint }
        val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
        DisposableEffect(entry, lifecycle) {
            entries.add(entry)
            val observer =
                androidx.lifecycle.LifecycleEventObserver { _, event ->
                    when (event) {
                        androidx.lifecycle.Lifecycle.Event.ON_RESUME -> entry.leaving = false
                        androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> entry.leaving = true
                        else -> Unit
                    }
                }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                entries.remove(entry)
            }
        }
    }
}
