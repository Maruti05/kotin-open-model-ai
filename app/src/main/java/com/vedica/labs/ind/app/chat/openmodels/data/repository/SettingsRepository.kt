package com.vedica.labs.ind.app.chat.openmodels.data.repository

import com.vedica.labs.ind.app.chat.openmodels.data.local.preferences.AppPreferences
import com.vedica.labs.ind.app.chat.openmodels.data.model.InferenceParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val preferences: AppPreferences
) {
    fun getInferenceParams(): Flow<InferenceParams> = combine(
        combine(preferences.temperature, preferences.topP, preferences.topK, preferences.maxTokens) { temp, p, k, tokens ->
            InferenceParams(
                temperature = temp, topP = p, topK = k, maxTokens = tokens,
                systemPrompt = "", showThinking = false, showReasoning = false
            )
        },
        preferences.systemPrompt, preferences.showThinking, preferences.showReasoning
    ) { params, prompt, thinking, reasoning ->
        params.copy(
            systemPrompt = prompt, showThinking = thinking, showReasoning = reasoning
        )
    }

    val themeMode: Flow<String> = preferences.themeMode

    suspend fun setTemperature(value: Double) = preferences.setTemperature(value)
    suspend fun setTopP(value: Double) = preferences.setTopP(value)
    suspend fun setTopK(value: Int) = preferences.setTopK(value)
    suspend fun setMaxTokens(value: Int) = preferences.setMaxTokens(value)
    suspend fun setSystemPrompt(value: String) = preferences.setSystemPrompt(value)
    suspend fun setShowThinking(value: Boolean) = preferences.setShowThinking(value)
    suspend fun setShowReasoning(value: Boolean) = preferences.setShowReasoning(value)
    suspend fun setThemeMode(mode: String) = preferences.setThemeMode(mode)
}
