package com.vedica.labs.ind.app.chat.openmodels.domain.inference

import com.vedica.labs.ind.app.chat.openmodels.data.model.InferenceParams
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelFormat
import com.vedica.labs.ind.app.chat.openmodels.data.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
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
        unloadModel()

        val modelPath = modelRepository.getModelPath(modelId)
        if (modelPath.isEmpty()) {
            throw Exception("Model file path could not be resolved for \"$modelId\".")
        }

        val file = File(modelPath)
        if (!file.exists()) {
            throw Exception("Model file not found at: $modelPath")
        }

        val format = resolveFormat(modelId, modelPath)
        val engine = when (format) {
            ModelFormat.GGUF -> ggufEngine
            ModelFormat.TFLITE -> simulatedEngine
            ModelFormat.ONNX -> simulatedEngine
            ModelFormat.UNKNOWN -> simulatedEngine
        }

        try {
            val success = engine.loadModel(modelId, modelPath, hyperparams)
            if (success) {
                _activeEngine = engine
                _activeModelId = modelId
            }
            return success
        } catch (e: Exception) {
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
            throw Exception("No model loaded")
        }
        return _activeEngine!!.generateChat(
            messages = messages,
            template = template ?: _activeEngine!!.currentTemplate,
            params = params
        )
    }

    suspend fun stopGeneration() {
        _activeEngine?.stopGeneration()
    }

    suspend fun unloadModel() {
        _activeEngine?.let {
            if (it.isLoaded) {
                it.stopGeneration()
                it.unloadModel()
            }
        }
        _activeEngine = null
        _activeModelId = null
    }

    private fun resolveFormat(modelId: String, modelPath: String): ModelFormat {
        val ext = modelPath.substringAfterLast('.', "")
        val fromExt = ModelFormat.fromExtension(".$ext")
        if (fromExt != ModelFormat.UNKNOWN) return fromExt

        return try {
            val file = File(modelPath)
            if (file.exists()) {
                ModelFormat.fromFileSize(file.length())
            } else ModelFormat.GGUF
        } catch (_: Exception) {
            ModelFormat.GGUF
        }
    }
}
