package imitjesus.servantspreps.org.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val FONT_SIZE = floatPreferencesKey("font_size")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val NOTIFICATION_TIME = stringPreferencesKey("notification_time")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val SHOWN_QUOTES = stringSetPreferencesKey("shown_quotes")
        val LAST_RANDOM_YEAR = intPreferencesKey("last_random_year")
        
        const val THEME_CREAM = "cream"
        const val THEME_DARK = "dark"
        const val THEME_FOREST = "forest"
        const val THEME_CRIMSON = "crimson"

        const val FONT_SERIF = "serif"
        const val FONT_SANS = "sans-serif"
        const val FONT_MONO = "monospace"
    }

    val fontSizeFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[FONT_SIZE] ?: 20f
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val theme = preferences[THEME_COLOR] ?: THEME_CREAM
        if (theme == "royal") THEME_CRIMSON else theme
    }

    val fontFamilyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FONT_FAMILY] ?: FONT_SERIF
    }

    val notificationTimeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_TIME] ?: "08:00"
    }

    val shownQuotesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[SHOWN_QUOTES] ?: emptySet()
    }

    val lastRandomYearFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LAST_RANDOM_YEAR] ?: -1
    }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE] = size
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_COLOR] = theme
        }
    }

    suspend fun setFontFamily(font: String) {
        context.dataStore.edit { preferences ->
            preferences[FONT_FAMILY] = font
        }
    }

    suspend fun setNotificationTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_TIME] = time
        }
    }

    suspend fun markQuoteAsShown(id: Int, year: Int) {
        context.dataStore.edit { preferences ->
            val currentYear = preferences[LAST_RANDOM_YEAR] ?: -1
            val currentShown = preferences[SHOWN_QUOTES] ?: emptySet()
            
            if (year > currentYear) {
                // New year, clear history
                preferences[SHOWN_QUOTES] = setOf(id.toString())
                preferences[LAST_RANDOM_YEAR] = year
            } else {
                preferences[SHOWN_QUOTES] = currentShown + id.toString()
            }
        }
    }

    suspend fun resetShownQuotes() {
        context.dataStore.edit { preferences ->
            preferences[SHOWN_QUOTES] = emptySet()
        }
    }
}
