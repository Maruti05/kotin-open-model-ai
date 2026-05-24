package com.vedica.labs.ind.app.chat.openmodels.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "openmodels_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val TEMPERATURE = doublePreferencesKey("temperature")
        private val TOP_P = doublePreferencesKey("top_p")
        private val TOP_K = intPreferencesKey("top_k")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val SHOW_THINKING = booleanPreferencesKey("show_thinking")
        private val SHOW_REASONING = booleanPreferencesKey("show_reasoning")
        private val DOWNLOADED_MODEL_IDS = stringPreferencesKey("downloaded_model_ids")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "dark"
    }

    val temperature: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[TEMPERATURE] ?: 0.7
    }

    val topP: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[TOP_P] ?: 0.9
    }

    val topK: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TOP_K] ?: 40
    }

    val maxTokens: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[MAX_TOKENS] ?: 512
    }

    val systemPrompt: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SYSTEM_PROMPT] ?: ""
    }

    val showThinking: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_THINKING] ?: true
    }

    val showReasoning: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_REASONING] ?: true
    }

    val downloadedModelIds: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[DOWNLOADED_MODEL_IDS]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }

    suspend fun setTemperature(value: Double) {
        context.dataStore.edit { prefs -> prefs[TEMPERATURE] = value }
    }

    suspend fun setTopP(value: Double) {
        context.dataStore.edit { prefs -> prefs[TOP_P] = value }
    }

    suspend fun setTopK(value: Int) {
        context.dataStore.edit { prefs -> prefs[TOP_K] = value }
    }

    suspend fun setMaxTokens(value: Int) {
        context.dataStore.edit { prefs -> prefs[MAX_TOKENS] = value }
    }

    suspend fun setSystemPrompt(value: String) {
        context.dataStore.edit { prefs -> prefs[SYSTEM_PROMPT] = value }
    }

    suspend fun setShowThinking(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SHOW_THINKING] = value }
    }

    suspend fun setShowReasoning(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[SHOW_REASONING] = value }
    }

    suspend fun addDownloadedModelId(modelId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[DOWNLOADED_MODEL_IDS]?.split(",")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
            if (!current.contains(modelId)) {
                current.add(modelId)
                prefs[DOWNLOADED_MODEL_IDS] = current.joinToString(",")
            }
        }
    }

    suspend fun removeDownloadedModelId(modelId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[DOWNLOADED_MODEL_IDS]?.split(",")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
            current.remove(modelId)
            prefs[DOWNLOADED_MODEL_IDS] = current.joinToString(",")
        }
    }
}
