package com.vedica.labs.ind.app.chat.openmodels.domain.inference

import com.vedica.labs.ind.app.chat.openmodels.data.model.BackendType
import com.vedica.labs.ind.app.chat.openmodels.data.model.InferenceParams
import kotlinx.coroutines.flow.Flow

interface InferenceEngine {
    val isLoaded: Boolean
    val loaderName: String
    val backendType: BackendType
    val currentModelId: String?
    val currentTemplate: String?

    suspend fun loadModel(
        modelId: String,
        modelPath: String,
        hyperparams: Map<String, Any>? = null
    ): Boolean

    fun generateChat(
        messages: List<ChatMessage>,
        template: String?,
        params: InferenceParams
    ): Flow<String>

    suspend fun stopGeneration()
    suspend fun unloadModel()
    fun dispose()
}

data class ChatMessage(
    val role: String,
    val content: String
)
