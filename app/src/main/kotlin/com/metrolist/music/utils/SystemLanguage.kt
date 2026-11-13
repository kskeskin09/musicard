/**
 * Musicard (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.res.Resources
import com.metrolist.music.constants.CountryCodeToName
import com.metrolist.music.constants.LanguageCodeToName
import com.metrolist.music.constants.SYSTEM_DEFAULT
import java.util.Locale

/** The phone's own language and region (not the in-app language override). */
object SystemLanguage {
    fun locale(): Locale =
        Resources.getSystem().configuration.locales.takeIf { !it.isEmpty }?.get(0) ?: Locale.getDefault()

    /** Language code as used in [LanguageCodeToName], or null when the language isn't listed. */
    fun languageCode(locale: Locale = locale()): String? =
        locale.toLanguageTag().takeIf { it in LanguageCodeToName }
            ?: "${locale.language}-${locale.country}".takeIf { it in LanguageCodeToName }
            ?: locale.language.takeIf { it in LanguageCodeToName }

    fun languageName(locale: Locale = locale()): String =
        languageCode(locale)?.let { LanguageCodeToName[it] }
            ?: locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }

    fun countryName(locale: Locale = locale()): String =
        CountryCodeToName[locale.country] ?: locale.getDisplayCountry(locale)
}

/** Target language for AI lyrics translation; "System language" follows the phone. */
fun resolveTranslateLanguage(value: String): String =
    if (value == SYSTEM_DEFAULT || value.isBlank()) SystemLanguage.languageCode() ?: "en" else value
