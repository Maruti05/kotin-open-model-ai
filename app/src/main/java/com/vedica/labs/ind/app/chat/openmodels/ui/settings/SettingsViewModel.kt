package com.vedica.labs.ind.app.chat.openmodels.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vedica.labs.ind.app.chat.openmodels.data.model.InferenceParams
import com.vedica.labs.ind.app.chat.openmodels.data.model.PromptPreset
import com.vedica.labs.ind.app.chat.openmodels.data.repository.SettingsRepository
import com.vedica.labs.ind.app.chat.openmodels.data.repository.ModelRepository
import com.vedica.labs.ind.app.chat.openmodels.domain.util.PromptTemplateService

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val promptTemplateService: PromptTemplateService,
    private val modelRepository: ModelRepository
) : ViewModel() {

    val inferenceParams: StateFlow<InferenceParams> = settingsRepository
        .getInferenceParams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InferenceParams())

    val themeMode: StateFlow<String> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dark")

    val presets: List<PromptPreset> = promptTemplateService.presets

    private val _activePresetId = MutableStateFlow<String?>(null)
    val activePresetId: StateFlow<String?> = _activePresetId.asStateFlow()

    fun setTemperature(value: Double) {
        viewModelScope.launch { settingsRepository.setTemperature(value) }
        _activePresetId.value = null
    }

    fun setTopP(value: Double) {
        viewModelScope.launch { settingsRepository.setTopP(value) }
        _activePresetId.value = null
    }

    fun setTopK(value: Int) {
        viewModelScope.launch { settingsRepository.setTopK(value) }
        _activePresetId.value = null
    }

    fun setMaxTokens(value: Int) {
        viewModelScope.launch { settingsRepository.setMaxTokens(value) }
        _activePresetId.value = null
    }

    fun setSystemPrompt(value: String) {
        viewModelScope.launch { settingsRepository.setSystemPrompt(value) }
    }

    fun setSystemPromptEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setSystemPromptEnabled(value) }
    }

    fun setShowThinking(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowThinking(value) }
    }

    fun setShowReasoning(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowReasoning(value) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun applyPreset(preset: PromptPreset) {
        viewModelScope.launch {
            settingsRepository.setSystemPrompt(preset.systemPrompt)
            _activePresetId.value = preset.id
        }
    }

    fun applyInferencePreset(preset: InferenceParams) {
        viewModelScope.launch {
            settingsRepository.setTemperature(preset.temperature)
            settingsRepository.setTopP(preset.topP)
            settingsRepository.setTopK(preset.topK)
            settingsRepository.setMaxTokens(preset.maxTokens)
        }
    }
}
