package com.vedica.labs.ind.app.chat.openmodels.domain.inference

import com.vedica.labs.ind.app.chat.openmodels.data.model.InferenceParams
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelFormat
import com.vedica.labs.ind.app.chat.openmodels.data.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HybridModelManager @Inject constructor(
    private val simulatedEngine: SimulatedInferenceEngine,
    private val ggufEngine: GGUFInferenceEngine,
    private val modelRepository: ModelRepository
) {
    private var _activeEngine: InferenceEngine? = null
    private var _activeModelId: String? = null

    val isLoaded: Boolean get() = _activeEngine?.isLoaded == true
    val activeModelId: String? get() = _activeModelId
    val currentTemplate: String? get() = _activeEngine?.currentTemplate

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

        val format = resolveFormat(modelPath)
        Timber.tag("HybridMgr").d("Resolved format: %s for %s", format, modelId)
        val engine = when (format) {
            ModelFormat.GGUF -> {
                Timber.tag("HybridMgr").d("Using GGUF engine")
                ggufEngine
            }
            ModelFormat.TFLITE -> {
                Timber.tag("HybridMgr").d("Using simulated engine (TFLITE)")
                simulatedEngine
            }
            ModelFormat.ONNX -> {
                Timber.tag("HybridMgr").d("Using simulated engine (ONNX)")
                simulatedEngine
            }
            ModelFormat.UNKNOWN -> {
                Timber.tag("HybridMgr").d("Using simulated engine (UNKNOWN)")
                simulatedEngine
            }
        }

        try {
            val success = engine.loadModel(modelId, modelPath, hyperparams)
            if (success) {
                _activeEngine = engine
                _activeModelId = modelId
                Timber.tag("HybridMgr").i("Model loaded: %s via %s", modelId, engine.loaderName)
            }
            return success
        } catch (e: Exception) {
            Timber.tag("HybridMgr").e(e, "Failed to load model: %s", modelId)
            _activeEngine = null
            _activeModelId = null
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
        Timber.tag("HybridMgr").i("Unloading model: %s", _activeModelId)
        _activeEngine?.let {
            if (it.isLoaded) {
                it.stopGeneration()
                it.unloadModel()
            }
        }
        _activeEngine = null
        _activeModelId = null
    }

    private fun resolveFormat(modelPath: String): ModelFormat {
        val ext = modelPath.substringAfterLast('.', "")
        val fromExt = ModelFormat.fromExtension(".$ext")
        if (fromExt != ModelFormat.UNKNOWN) {
            Timber.tag("HybridMgr").d("Format from extension '%s': %s", ext, fromExt)
            return fromExt
        }

        return try {
            val file = File(modelPath)
            if (file.exists()) {
                val sizeFormat = ModelFormat.fromFileSize()
                Timber.tag("HybridMgr").d("Format from file size: %s", sizeFormat)
                sizeFormat
            } else {
                Timber.tag("HybridMgr").d("File not found, defaulting to GGUF")
                ModelFormat.GGUF
            }
        } catch (e: Exception) {
            Timber.tag("HybridMgr").w(e, "Format resolution fallback to GGUF")
            ModelFormat.GGUF
        }
    }
}
