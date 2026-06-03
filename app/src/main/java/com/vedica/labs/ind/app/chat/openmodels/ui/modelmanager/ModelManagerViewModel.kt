package com.vedica.labs.ind.app.chat.openmodels.ui.modelmanager

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelCatalog
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelDownloadState
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

    val filteredModels: StateFlow<List<Map<String, Any>>> = combine(
        _searchQuery, _selectedTier, _downloadFilter, downloadedModelIds
    ) { query, tier, filter, downloaded ->
        val catalog = ModelCatalog.models
        catalog.filter { model ->
            val id = model["id"] as? String ?: return@filter false
            val matchesQuery = query.isBlank() ||
                id.contains(query, ignoreCase = true) ||
                (model["name"] as? String)?.contains(query, ignoreCase = true) == true
            val matchesTier = tier == null || (model["tier"] as? Int) == tier
            val matchesFilter = when (filter) {
                DownloadFilter.ALL -> true
                DownloadFilter.DOWNLOADED -> downloaded.contains(id)
                DownloadFilter.NOT_DOWNLOADED -> !downloaded.contains(id)
            }
            matchesQuery && matchesTier && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun canRunModel(modelId: String): Boolean {
        val model = ModelCatalog.getModelById(modelId) ?: return false
        val minRam = (model["minRamGb"] as? Number)?.toDouble() ?: return true
        val caps = _deviceCapabilities.value
        return caps.availableRamGb >= minRam * 0.5
    }

    fun canDownloadModel(modelId: String): Boolean {
        val model = ModelCatalog.getModelById(modelId) ?: return false
        val sizeMb = (model["sizeMb"] as? Number)?.toDouble() ?: return true
        val minRam = (model["minRamGb"] as? Number)?.toDouble() ?: return true
        val caps = _deviceCapabilities.value
        val hasStorage = caps.availableStorageGb >= sizeMb / 1024.0 * 1.5
        val hasRam = caps.availableRamGb >= minRam * 0.5
        val hasBattery = caps.batteryLevel >= 20 || caps.isCharging
        return hasStorage && hasRam && hasBattery
    }

    fun getIncompatibilityReason(modelId: String): String? {
        val model = ModelCatalog.getModelById(modelId) ?: return null
        val minRam = (model["minRamGb"] as? Number)?.toDouble() ?: return null
        val sizeMb = (model["sizeMb"] as? Number)?.toDouble() ?: 0.0
        val caps = _deviceCapabilities.value
        if (caps.availableRamGb < minRam * 0.5) {
            return "Insufficient RAM (need ${"%.1f".format(minRam)} GB)"
        }
        if (caps.availableStorageGb < sizeMb / 1024.0 * 1.5) {
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

    init {
        refreshDeviceCapabilities()
    }

    fun triggerDownload(modelId: String) {
        viewModelScope.launch {
            val model = ModelCatalog.getModelById(modelId) ?: return@launch
            val downloadUrl = model["downloadUrl"] as? String ?: return@launch
            val totalBytes = ((model["sizeMb"] as? Number)?.toDouble() ?: 1500.0) * 1024 * 1024
            modelRepository.startDownload(modelId, downloadUrl, totalBytes.toLong())
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
