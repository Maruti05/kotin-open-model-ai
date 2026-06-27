package com.vedica.labs.ind.app.chat.openmodels.domain.inference

import com.vedica.labs.ind.app.chat.openmodels.data.model.BackendType
import com.vedica.labs.ind.app.chat.openmodels.data.model.InferenceParams
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelCatalog
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelFormat
import com.vedica.labs.ind.app.chat.openmodels.data.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HybridModelManager @Inject constructor(
    private val simulatedEngine: SimulatedInferenceEngine,
    private val ggufEngine: GGUFInferenceEngine,
    private val liteRtEngine: LiteRTInferenceEngine,
    private val modelRepository: ModelRepository
) {
    private var _activeEngine: InferenceEngine? = null
    private val _activeModelId = MutableStateFlow<String?>(null)

    val isLoaded: Boolean get() = _activeEngine?.isLoaded == true
    val activeModelId: StateFlow<String?> = _activeModelId.asStateFlow()
    val activeModelIdValue: String? get() = _activeModelId.value
    val currentTemplate: String? get() = _activeEngine?.currentTemplate
    val activeBackendType: BackendType? get() = _activeEngine?.backendType

    suspend fun loadModelToRam(
        modelId: String,
        hyperparams: Map<String, Any>? = null
    ): Boolean {
        Timber.tag("HybridMgr").d("loadModelToRam: id=%s, hyperparams=%s", modelId, hyperparams)
        unloadModel()

        val modelPath = modelRepository.getModelPath(modelId)
        if (modelPath.isEmpty()) {
            Timber.tag("HybridMgr").e("Model path empty for: %s", modelId)
            throw Exception("Model file path could not be resolved for \"$modelId\".")
        }

        val file = File(modelPath)
        if (!file.exists()) {
            Timber.tag("HybridMgr").e("Model file not found: %s", modelPath)
            throw Exception("Model file not found at: $modelPath")
        }

        val backendType = resolveBackendType(modelId, modelPath)
        Timber.tag("HybridMgr").d("Resolved backend: %s for %s", backendType, modelId)

        val engine = when (backendType) {
            BackendType.LITERT -> {
                Timber.tag("HybridMgr").d("Using LiteRT engine for %s", modelId)
                liteRtEngine
            }
            BackendType.LLAMA_CPP -> {
                Timber.tag("HybridMgr").d("Using GGUF engine for %s", modelId)
                ggufEngine
            }
        }

        try {
            val success = engine.loadModel(modelId, modelPath, hyperparams)
            if (success) {
                _activeEngine = engine
                _activeModelId.value = modelId
                Timber.tag("HybridMgr").i("Model loaded: %s via %s (%s)", modelId, engine.loaderName, backendType.engineName)
            }
            return success
        } catch (e: Exception) {
            Timber.tag("HybridMgr").e(e, "Failed to load model: %s", modelId)
            _activeEngine = null
            _activeModelId.value = null
            throw e
        }
    }

    fun generateChat(
        messages: List<ChatMessage>,
        template: String?,
        params: InferenceParams
    ): Flow<String> {
        if (_activeEngine == null || !_activeEngine!!.isLoaded) {
            Timber.tag("HybridMgr").w("generateChat called but no model loaded")
            throw Exception("No model loaded")
        }
        Timber.tag("HybridMgr").d("generateChat: msgs=%d, engine=%s", messages.size, _activeEngine!!.loaderName)
        return _activeEngine!!.generateChat(
            messages = messages,
            template = template ?: _activeEngine!!.currentTemplate,
            params = params
        )
    }

    suspend fun stopGeneration() {
        Timber.tag("HybridMgr").d("stopGeneration")
        _activeEngine?.stopGeneration()
    }

    suspend fun unloadModel() {
        Timber.tag("HybridMgr").i("Unloading model: %s", _activeModelId.value)
        _activeEngine?.let {
            if (it.isLoaded) {
                it.stopGeneration()
                it.unloadModel()
            }
        }
        _activeEngine = null
        _activeModelId.value = null
    }

    private fun resolveBackendType(modelId: String, modelPath: String): BackendType {
        val catalogBackend = ModelCatalog.getBackendType(modelId)
        if (catalogBackend != BackendType.LLAMA_CPP) {
            Timber.tag("HybridMgr").d("Backend from catalog: %s", catalogBackend)
            return catalogBackend
        }

        val ext = modelPath.substringAfterLast('.', "")
        val fromExt = BackendType.fromExtension(".$ext")
        if (fromExt != BackendType.LLAMA_CPP) {
            Timber.tag("HybridMgr").d("Backend from extension '%s': %s", ext, fromExt)
            return fromExt
        }

        return try {
            val format = ModelFormat.fromFileSignature(modelPath)
            when (format) {
                ModelFormat.LITERT -> BackendType.LITERT
                else -> BackendType.LLAMA_CPP
            }
        } catch (e: Exception) {
            Timber.tag("HybridMgr").w(e, "Backend resolution fallback to LLAMA_CPP")
            BackendType.LLAMA_CPP
        }
    }
}
