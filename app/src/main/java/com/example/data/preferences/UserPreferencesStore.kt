package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.localization.AppLang
import com.example.ui.theme.BengaliFontOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.uiPrefsDataStore by preferencesDataStore(name = "pims_ui_preferences")

data class UiPreferences(
    val lang: AppLang = AppLang.BN,
    val darkMode: Boolean? = null,
    val bengaliFont: BengaliFontOption = BengaliFontOption.HIND_SILIGURI
)

class UserPreferencesStore(private val context: Context) {
    private val keyLang = stringPreferencesKey("app_lang")
    private val keyDark = booleanPreferencesKey("dark_mode")
    private val keyHasDark = booleanPreferencesKey("has_dark_override")
    private val keyBengaliFont = stringPreferencesKey("bengali_font")

    val preferences: Flow<UiPreferences> = context.uiPrefsDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val lang = if (prefs[keyLang] == AppLang.EN.name) AppLang.EN else AppLang.BN
            val darkOverride = if (prefs[keyHasDark] == true) prefs[keyDark] else null
            val font = BengaliFontOption.fromRaw(prefs[keyBengaliFont])
            UiPreferences(lang = lang, darkMode = darkOverride, bengaliFont = font)
        }

    suspend fun setLanguage(lang: AppLang) {
        context.uiPrefsDataStore.edit { it[keyLang] = lang.name }
    }

    suspend fun setDarkMode(enabled: Boolean?) {
        context.uiPrefsDataStore.edit { prefs ->
            if (enabled == null) {
                prefs[keyHasDark] = false
            } else {
                prefs[keyHasDark] = true
                prefs[keyDark] = enabled
            }
        }
    }

    suspend fun setBengaliFont(font: BengaliFontOption) {
        context.uiPrefsDataStore.edit { it[keyBengaliFont] = font.name }
    }
}
