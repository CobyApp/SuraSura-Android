package com.coby.surasura.data.prefs

import android.content.Context
import com.coby.surasura.data.model.SupportedLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists top/bottom panel languages across app launches.
 */
@Singleton
class LanguagePreferenceStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class PanelLanguages(
        val top: SupportedLanguage,
        val bottom: SupportedLanguage
    )

    fun load(): PanelLanguages {
        val topTag = prefs.getString(KEY_TOP, null)
        val bottomTag = prefs.getString(KEY_BOTTOM, null)
        val top = if (topTag != null) SupportedLanguage.fromLocaleTag(topTag) else SupportedLanguage.ENGLISH
        val bottom = if (bottomTag != null) SupportedLanguage.fromLocaleTag(bottomTag) else SupportedLanguage.JAPANESE
        return PanelLanguages(top, bottom)
    }

    fun save(top: SupportedLanguage, bottom: SupportedLanguage) {
        prefs.edit()
            .putString(KEY_TOP, top.localeTag)
            .putString(KEY_BOTTOM, bottom.localeTag)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "surasura_languages"
        const val KEY_TOP = "top_locale_tag"
        const val KEY_BOTTOM = "bottom_locale_tag"
    }
}
