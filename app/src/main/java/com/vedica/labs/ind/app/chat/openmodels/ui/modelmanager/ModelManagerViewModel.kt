package com.vedica.labs.ind.app.chat.openmodels.ui.modelmanager

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vedica.labs.ind.app.chat.openmodels.data.model.BackendType
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelCatalog
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelDownloadState
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelInfo
import com.vedica.labs.ind.app.chat.openmodels.data.repository.ModelRepository
import com.vedica.labs.ind.app.chat.openmodels.domain.inference.HybridModelManager
import com.vedica.labs.ind.app.chat.openmodels.domain.util.HardwareChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DownloadFilter { ALL, DOWNLOADED, NOT_DOWNLOADED }

enum class LicenseFilter { ALL, APACHE_2_0, MIT }

enum class UseCaseFilter { ALL, CHAT, CODE, REASONING, VISION, GENERAL }

data class DeviceCapabilities(
    val totalRamGb: Double = 0.0,
    val availableRamGb: Double = 0.0,
    val cores: Int = 0,
    val batteryLevel: Int = 100,
    val isCharging: Boolean = true,
    val availableStorageGb: Double = 100.0
) {
    val canDownload: Boolean get() = availableStorageGb > 1.0
    val canLoadModel: Boolean get() = availableRamGb > 0.5
}

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    application: Application,
    private val modelRepository: ModelRepository,
    private val modelManager: HybridModelManager,
    private val hardwareChecker: HardwareChecker
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTier = MutableStateFlow<Int?>(null)
    val selectedTier: StateFlow<Int?> = _selectedTier.asStateFlow()

    private val _downloadFilter = MutableStateFlow(DownloadFilter.ALL)
    val downloadFilter: StateFlow<DownloadFilter> = _downloadFilter.asStateFlow()

    private val _selectedLicense = MutableStateFlow(LicenseFilter.ALL)
    val selectedLicense: StateFlow<LicenseFilter> = _selectedLicense.asStateFlow()

    private val _selectedUseCase = MutableStateFlow(UseCaseFilter.ALL)
    val selectedUseCase: StateFlow<UseCaseFilter> = _selectedUseCase.asStateFlow()

    private val _selectedBackend = MutableStateFlow<BackendType?>(null)
    val selectedBackend: StateFlow<BackendType?> = _selectedBackend.asStateFlow()

    private val _deviceCapabilities = MutableStateFlow(DeviceCapabilities())
    val deviceCapabilities: StateFlow<DeviceCapabilities> = _deviceCapabilities.asStateFlow()

    private val _loadingModelId = MutableStateFlow<String?>(null)
    val loadingModelId: StateFlow<String?> = _loadingModelId.asStateFlow()

    val downloadedModelIds: StateFlow<List<String>> = modelRepository.downloadedModelIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<Map<String, ModelDownloadState>> = modelRepository.downloads

    val loadedModelId: StateFlow<String?> = MutableStateFlow(null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _baseFiltered: StateFlow<List<ModelInfo>> = combine(
        _searchQuery, _selectedTier, _downloadFilter, downloadedModelIds
    ) { query: String, tier: Int?, filter: DownloadFilter, downloaded: List<String> ->
        ModelCatalog.models.filter { model ->
            val matchesQuery = query.isBlank() ||
                model.id.contains(query, ignoreCase = true) ||
                model.name.contains(query, ignoreCase = true) ||
                model.description.contains(query, ignoreCase = true) ||
                model.license.contains(query, ignoreCase = true)

            val matchesTier = tier == null || model.tier == tier

            val matchesFilter = when (filter) {
                DownloadFilter.ALL -> true
                DownloadFilter.DOWNLOADED -> downloaded.contains(model.id)
                DownloadFilter.NOT_DOWNLOADED -> !downloaded.contains(model.id)
            }

            matchesQuery && matchesTier && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredModels: StateFlow<List<ModelInfo>> = combine(
        _baseFiltered, _selectedLicense, _selectedUseCase, _selectedBackend
    ) { preFiltered: List<ModelInfo>, license: LicenseFilter, useCase: UseCaseFilter, backend: BackendType? ->
        preFiltered.filter { model ->
            val matchesLicense = when (license) {
                LicenseFilter.ALL -> true
                LicenseFilter.APACHE_2_0 -> model.license == "apache-2.0"
                LicenseFilter.MIT -> model.license == "mit"
            }

            val matchesUseCase = when (useCase) {
                UseCaseFilter.ALL -> true
                UseCaseFilter.CHAT -> model.useCase == "chat"
                UseCaseFilter.CODE -> model.useCase == "code"
                UseCaseFilter.REASONING -> model.useCase == "reasoning"
                UseCaseFilter.VISION -> model.useCase == "vision"
                UseCaseFilter.GENERAL -> model.useCase == "general"
            }

            val matchesBackend = backend == null || model.backendType == backend

            matchesLicense && matchesUseCase && matchesBackend
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun canRunModel(modelId: String): Boolean {
        val model = ModelCatalog.getModelById(modelId) ?: return false
        val caps = _deviceCapabilities.value
        return caps.availableRamGb >= model.minRamGb * 0.5
    }

    fun canDownloadModel(modelId: String): Boolean {
        val model = ModelCatalog.getModelById(modelId) ?: return false
        val caps = _deviceCapabilities.value
        val hasStorage = caps.availableStorageGb >= model.sizeMb / 1024.0 * 1.5
        val hasRam = caps.availableRamGb >= model.minRamGb * 0.5
        val hasBattery = caps.batteryLevel >= 20 || caps.isCharging
        return hasStorage && hasRam && hasBattery
    }

    fun getIncompatibilityReason(modelId: String): String? {
        val model = ModelCatalog.getModelById(modelId) ?: return null
        val caps = _deviceCapabilities.value
        if (caps.availableRamGb < model.minRamGb * 0.5) {
            return "Insufficient RAM (need ${"%.1f".format(model.minRamGb)} GB)"
        }
        if (caps.availableStorageGb < model.sizeMb / 1024.0 * 1.5) {
            return "Insufficient storage"
        }
        if (caps.batteryLevel < 20 && !caps.isCharging) {
            return "Low battery (${caps.batteryLevel}%)"
        }
        return null
    }

    fun refreshDeviceCapabilities() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val diagnostics = hardwareChecker.getDiagnostics(ctx)

            val batteryLevel = getBatteryLevel(ctx)
            val isCharging = isCharging(ctx)
            val storageGb = getAvailableStorageGb()

            _deviceCapabilities.value = DeviceCapabilities(
                totalRamGb = diagnostics.totalRamGb,
                availableRamGb = diagnostics.availableRamGb,
                cores = diagnostics.cores,
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                availableStorageGb = storageGb
            )
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
    }

    private fun isCharging(context: Context): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return batteryManager?.isCharging ?: true
    }

    private fun getAvailableStorageGb(): Double {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val bytes = stat.availableBytes
            bytes.toDouble() / (1024 * 1024 * 1024)
        } catch (_: Exception) {
            100.0
        }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedTier(tier: Int?) { _selectedTier.value = tier }
    fun setDownloadFilter(filter: DownloadFilter) { _downloadFilter.value = filter }
    fun setSelectedLicense(filter: LicenseFilter) { _selectedLicense.value = filter }
    fun setSelectedUseCase(filter: UseCaseFilter) { _selectedUseCase.value = filter }
    fun setSelectedBackend(backend: BackendType?) { _selectedBackend.value = backend }

    init {
        refreshDeviceCapabilities()
    }

    fun triggerDownload(modelId: String) {
        viewModelScope.launch {
            val model = ModelCatalog.getModelById(modelId) ?: return@launch
            val totalBytes = (model.sizeMb * 1024 * 1024).toLong()
            val tokenizerUrl = model.tokenizerUrl.takeIf { it.isNotBlank() }
            modelRepository.startDownload(modelId, model.downloadUrl, totalBytes, tokenizerUrl)
        }
    }

    fun cancelDownload(modelId: String) {
        modelRepository.cancelDownload(modelId)
    }

    fun retryDownload(modelId: String) {
        modelRepository.removeDownload(modelId)
        triggerDownload(modelId)
    }

    fun clearDownloadError(modelId: String) {
        modelRepository.removeDownload(modelId)
    }

    fun loadModelToRam(modelId: String) {
        viewModelScope.launch {
            _loadingModelId.value = modelId
            _error.value = null
            try {
                modelManager.loadModelToRam(modelId)
                (loadedModelId as MutableStateFlow).value = modelId
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loadingModelId.value = null
            }
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            modelManager.unloadModel()
            (loadedModelId as MutableStateFlow).value = null
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            try {
                val path = modelRepository.getModelPath(modelId)
                val file = java.io.File(path)
                if (file.exists()) file.delete()
                val tokFile = java.io.File(file.parentFile, "tokenizer.json")
                if (tokFile.exists()) tokFile.delete()
                modelRepository.removeFromDownloaded(modelId)
                if (loadedModelId.value == modelId) {
                    unloadModel()
                }
                modelRepository.removeDownload(modelId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() { _error.value = null }
}
