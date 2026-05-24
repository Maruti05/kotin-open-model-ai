package com.vedica.labs.ind.app.chat.openmodels.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vedica.labs.ind.app.chat.openmodels.data.model.BenchmarkResult
import com.vedica.labs.ind.app.chat.openmodels.data.model.DiagnosticsInfo
import com.vedica.labs.ind.app.chat.openmodels.data.repository.BenchmarkRepository
import com.vedica.labs.ind.app.chat.openmodels.domain.benchmark.BenchmarkRunner
import com.vedica.labs.ind.app.chat.openmodels.domain.util.HardwareChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val hardwareChecker: HardwareChecker,
    private val benchmarkRunner: BenchmarkRunner,
    private val benchmarkRepository: BenchmarkRepository
) : AndroidViewModel(application) {

    private val _diagnostics = MutableStateFlow<DiagnosticsInfo?>(null)
    val diagnostics: StateFlow<DiagnosticsInfo?> = _diagnostics.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking: StateFlow<Boolean> = _isBenchmarking.asStateFlow()

    private val _latestBenchmark = MutableStateFlow<BenchmarkResult?>(null)
    val latestBenchmark: StateFlow<BenchmarkResult?> = _latestBenchmark.asStateFlow()

    val recentBenchmarks: StateFlow<List<BenchmarkResult>> = benchmarkRepository
        .getRecentBenchmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshDiagnostics()
        viewModelScope.launch {
            _latestBenchmark.value = benchmarkRepository.getLatestBenchmark()
        }
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val context = getApplication<Application>()
                _diagnostics.value = hardwareChecker.getDiagnostics(context)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Failed to get diagnostics"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun runBenchmark() {
        viewModelScope.launch {
            _isBenchmarking.value = true
            try {
                val info = _diagnostics.value
                val modelName = info?.let { "device-${it.deviceTier}" } ?: "unknown"
                val result = benchmarkRunner.runOnDeviceBenchmark(modelName)
                benchmarkRepository.saveBenchmark(result)
                _latestBenchmark.value = result
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isBenchmarking.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
