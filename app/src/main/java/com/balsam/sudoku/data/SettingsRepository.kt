package com.balsam.sudoku.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

/** 主题模式 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val highlightRegion: Boolean = true,   // 高亮同行/列/宫
    val highlightSame: Boolean = true,     // 高亮相同数字
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val showTimer: Boolean = true,
    val limitMistakes: Boolean = false,    // 开启后错误满 3 次本局失败
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val highlightRegion = booleanPreferencesKey("highlight_region")
        val highlightSame = booleanPreferencesKey("highlight_same")
        val soundEnabled = booleanPreferencesKey("sound_enabled")
        val vibrationEnabled = booleanPreferencesKey("vibration_enabled")
        val showTimer = booleanPreferencesKey("show_timer")
        val limitMistakes = booleanPreferencesKey("limit_mistakes")
        val currentGame = stringPreferencesKey("current_game")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            highlightRegion = prefs[Keys.highlightRegion] ?: true,
            highlightSame = prefs[Keys.highlightSame] ?: true,
            soundEnabled = prefs[Keys.soundEnabled] ?: true,
            vibrationEnabled = prefs[Keys.vibrationEnabled] ?: true,
            showTimer = prefs[Keys.showTimer] ?: true,
            limitMistakes = prefs[Keys.limitMistakes] ?: false,
        )
    }

    val currentGame: Flow<GameSave?> = context.dataStore.data.map { prefs ->
        prefs[Keys.currentGame]?.let { json ->
            runCatching { Json.decodeFromString<GameSave>(json) }.getOrNull()
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.themeMode] = mode.name }

    suspend fun setHighlightRegion(value: Boolean) =
        context.dataStore.edit { it[Keys.highlightRegion] = value }

    suspend fun setHighlightSame(value: Boolean) =
        context.dataStore.edit { it[Keys.highlightSame] = value }

    suspend fun setSoundEnabled(value: Boolean) =
        context.dataStore.edit { it[Keys.soundEnabled] = value }

    suspend fun setVibrationEnabled(value: Boolean) =
        context.dataStore.edit { it[Keys.vibrationEnabled] = value }

    suspend fun setShowTimer(value: Boolean) =
        context.dataStore.edit { it[Keys.showTimer] = value }

    suspend fun setLimitMistakes(value: Boolean) =
        context.dataStore.edit { it[Keys.limitMistakes] = value }

    suspend fun saveCurrentGame(save: GameSave?) {
        context.dataStore.edit { prefs ->
            if (save == null) prefs.remove(Keys.currentGame)
            else prefs[Keys.currentGame] = Json.encodeToString(save)
        }
    }
}
