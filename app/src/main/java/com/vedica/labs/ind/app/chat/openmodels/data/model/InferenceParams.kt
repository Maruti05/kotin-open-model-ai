package com.vedica.labs.ind.app.chat.openmodels.data.model

data class InferenceParams(
    val temperature: Double = 0.7,
    val topP: Double = 0.9,
    val topK: Int = 40,
    val maxTokens: Int = 512,
    val systemPrompt: String = "",
    val systemPromptEnabled: Boolean = true,
    val showThinking: Boolean = true,
    val showReasoning: Boolean = true
) {
    companion object {
        val PRECISE = InferenceParams(
            temperature = 0.1, topP = 0.1, topK = 5, maxTokens = 256
        )
        val BALANCED = InferenceParams(
            temperature = 0.7, topP = 0.9, topK = 40, maxTokens = 512
        )
        val CREATIVE = InferenceParams(
            temperature = 1.2, topP = 0.95, topK = 80, maxTokens = 1024
        )
    }
}
