package com.suvojeet.notenext.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.suvojeet.notenext.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferencesKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
    val ENABLE_RICH_LINK_PREVIEW = booleanPreferencesKey("enable_rich_link_preview")
    val ENABLE_APP_LOCK = booleanPreferencesKey("enable_app_lock")
    val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
    val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
    val LANGUAGE = stringPreferencesKey("language")
    val LAST_SEEN_VERSION = intPreferencesKey("last_seen_version")
    val DISALLOW_SCREENSHOTS = booleanPreferencesKey("disallow_screenshots")
    
    // Groq API Settings
    val USE_CUSTOM_GROQ_KEY = booleanPreferencesKey("use_custom_groq_key")
    val CUSTOM_GROQ_KEY = stringPreferencesKey("custom_groq_key")
    val CUSTOM_FAST_MODEL = stringPreferencesKey("custom_fast_model")
    val CUSTOM_LARGE_MODEL = stringPreferencesKey("custom_large_model")
    
    // AI Provider Settings
    val PREFERRED_AI_PROVIDER = stringPreferencesKey("preferred_ai_provider")
    val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
    val OPENAI_BASE_URL = stringPreferencesKey("openai_base_url")
    val ANTHROPIC_API_KEY = stringPreferencesKey("anthropic_api_key")
}

class SettingsRepository(private val context: Context) {
    
    // Groq API Settings
    val useCustomGroqKey: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.USE_CUSTOM_GROQ_KEY] ?: false }

    suspend fun saveUseCustomGroqKey(use: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.USE_CUSTOM_GROQ_KEY] = use }
    }

    val customGroqKey: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.CUSTOM_GROQ_KEY] ?: "" }

    suspend fun saveCustomGroqKey(key: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.CUSTOM_GROQ_KEY] = key }
    }

    val customFastModel: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.CUSTOM_FAST_MODEL] ?: "llama-3.1-8b-instant" }

    suspend fun saveCustomFastModel(model: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.CUSTOM_FAST_MODEL] = model }
    }

    val customLargeModel: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.CUSTOM_LARGE_MODEL] ?: "llama-3.3-70b-versatile" }

    suspend fun saveCustomLargeModel(model: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.CUSTOM_LARGE_MODEL] = model }
    }

    val lastSeenVersion: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_SEEN_VERSION] ?: 0
        }

    suspend fun saveLastSeenVersion(version: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SEEN_VERSION] = version
        }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map {
            preferences ->
            ThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    val autoDeleteDays: Flow<Int> = context.dataStore.data
        .map {
            preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] ?: 7
        }

    suspend fun saveAutoDeleteDays(days: Int) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] = days
        }
    }

    val enableRichLinkPreview: Flow<Boolean> = context.dataStore.data
        .map {
            preferences ->
            preferences[PreferencesKeys.ENABLE_RICH_LINK_PREVIEW] ?: true
        }

    suspend fun saveEnableRichLinkPreview(enable: Boolean) {
        context.dataStore.edit {
            preferences ->
            preferences[PreferencesKeys.ENABLE_RICH_LINK_PREVIEW] = enable
        }
    }

    val enableAppLock: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ENABLE_APP_LOCK] ?: false
        }

    suspend fun saveEnableAppLock(enable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_APP_LOCK] = enable
        }
    }

    val appLockPin: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.APP_LOCK_PIN]
        }

    suspend fun saveAppLockPin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK_PIN] = pin
        }
    }

    val isSetupComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_SETUP_COMPLETE] ?: false
        }

    suspend fun setSetupComplete(isComplete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SETUP_COMPLETE] = isComplete
        }
    }

    val language: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LANGUAGE] ?: "en"
        }

    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    val disallowScreenshots: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DISALLOW_SCREENSHOTS] ?: false
        }

    suspend fun saveDisallowScreenshots(disallow: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISALLOW_SCREENSHOTS] = disallow
        }
    }

    // AI Provider Settings
    val preferredAIProvider: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.PREFERRED_AI_PROVIDER] ?: "GROQ" }

    suspend fun savePreferredAIProvider(provider: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.PREFERRED_AI_PROVIDER] = provider }
    }

    val openAIApiKey: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.OPENAI_API_KEY] ?: "" }

    suspend fun saveOpenAIApiKey(key: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.OPENAI_API_KEY] = key }
    }

    val openAIBaseUrl: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.OPENAI_BASE_URL] ?: "https://api.openai.com/" }

    suspend fun saveOpenAIBaseUrl(url: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.OPENAI_BASE_URL] = url }
    }

    val anthropicApiKey: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.ANTHROPIC_API_KEY] ?: "" }

    suspend fun saveAnthropicApiKey(key: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.ANTHROPIC_API_KEY] = key }
    }
}