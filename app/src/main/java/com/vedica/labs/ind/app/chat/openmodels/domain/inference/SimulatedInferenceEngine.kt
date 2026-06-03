package com.vedica.labs.ind.app.chat.openmodels.domain.inference

import com.vedica.labs.ind.app.chat.openmodels.data.model.InferenceParams
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimulatedInferenceEngine @Inject constructor() : InferenceEngine {

    private var _isLoaded = false
    private var _currentModelId: String? = null
    private var _currentTemplate: String? = null

    override val isLoaded: Boolean get() = _isLoaded
    override val loaderName: String get() = "Simulated Engine"
    override val currentModelId: String? get() = _currentModelId
    override val currentTemplate: String? get() = _currentTemplate

    companion object {
        private val MODEL_TEMPLATES = mapOf(
            "smollm_135m_q4" to "chatml", "smollm_360m_q4" to "chatml",
            "qwen_0_5b_q2" to "chatml", "qwen_0_5b_q4" to "chatml",
            "phi_1_5_q2" to "phi", "gemma_2_2b_q2" to "gemma",
            "phi_2_q2" to "phi", "gemma_2_2b_q4" to "gemma",
            "llama_3_3b_q4" to "llama2", "phi_3_mini_q4" to "phi",
            "mistral_7b_q4" to "llama2"
        )

        private val MODEL_LABELS = mapOf(
            "smollm_135m_q4" to "SmolLM2-135M (Local)",
            "smollm_360m_q4" to "SmolLM2-360M (Local)",
            "qwen_0_5b_q2" to "Qwen-0.5B-Q2 (Local)",
            "qwen_0_5b_q4" to "Qwen-0.5B (Local)",
            "phi_1_5_q2" to "Phi-1.5-Q2 (Local)",
            "gemma_2_2b_q2" to "Gemma-2B-Q2 (Local)",
            "phi_2_q2" to "Phi-2-Q2 (Local)",
            "gemma_2_2b_q4" to "Gemma-2B-Q4 (Local)",
            "llama_3_3b_q4" to "Llama-3-3B (Local)",
            "phi_3_mini_q4" to "Phi-3-Mini (Local)",
            "mistral_7b_q4" to "Mistral-7B (Local)"
        )
    }

    override suspend fun loadModel(
        modelId: String,
        modelPath: String,
        hyperparams: Map<String, Any>?
    ): Boolean {
        _currentModelId = modelId
        _currentTemplate = MODEL_TEMPLATES[modelId] ?: "chatml"
        _isLoaded = true
        return true
    }

    override fun generateChat(
        messages: List<ChatMessage>,
        template: String?,
        params: InferenceParams
    ): Flow<String> = flow {
        val modelLabel = MODEL_LABELS[_currentModelId] ?: "Offline LLM Core"
        val baseDelayMs = (40 + (params.temperature * 15)).toLong()

        val userQuery = messages.lastOrNull()?.content?.lowercase() ?: ""

        val response = buildSimulatedResponse(userQuery, modelLabel, params)

        val words = response.split(" ").take(params.maxTokens)
        for (word in words) {
            if (!currentCoroutineContext().isActive) break
            val jitter = (Math.random() * baseDelayMs * 0.5).toLong()
            delay(baseDelayMs + jitter)
            emit("$word ")
        }
        emit("[DONE]")
    }

    private fun buildSimulatedResponse(
        query: String, modelLabel: String, params: InferenceParams
    ): String = when {
        query.contains("hello") || query.contains("hi") -> {
            "Hello! I am your fully local on-device assistant running under **$modelLabel**. " +
            "Since I am running entirely in your physical memory, your conversations are 100% private. " +
            "Current inference params: Temperature=${"%.1f".format(params.temperature)}, " +
            "Top-P=${"%.2f".format(params.topP)}, Top-K=${params.topK}, Max tokens=${params.maxTokens}. " +
            "How can I assist you with code generation, logical reasoning, or data analysis today?"
        }
        query.contains("system") || query.contains("hardware") || query.contains("ram") -> {
            "### Local Hardware Diagnostics Report\n\n" +
            "Here are the active on-device metrics scanned via our native Kotlin subsystem:\n\n" +
            "- **Active Model Core:** `$modelLabel`\n" +
            "- **Inference Temperature:** `" + "%.1f".format(params.temperature) + "`\n" +
            "- **Top-P Sampling:** `" + "%.2f".format(params.topP) + "`\n" +
            "- **Top-K:** `" + params.topK + "`\n" +
            "- **Max Output Tokens:** `" + params.maxTokens + "`\n\n" +
            "The system is using optimal thread boundaries to avoid blocking your UI's buttery smooth rendering."
        }
        query.contains("code") || query.contains("program") || query.contains("flutter") || query.contains("kotlin") || query.contains("dart") -> {
            "Sure! Here is a clean, optimized Kotlin/Compose function showcasing a glassmorphic container:\n\n" +
            "```kotlin\n" +
            "@Composable\n" +
            "fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {\n" +
            "    Box(\n" +
            "        modifier = modifier\n" +
            "            .clip(RoundedCornerShape(24.dp))\n" +
            "            .background(Color.White.copy(alpha = 0.08f))\n" +
            "            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))\n" +
            "    ) { content() }\n" +
            "}\n" +
            "```\n\n" +
            "This creates a premium glassmorphic aesthetic with the Material3 design system."
        }
        query.contains("help") || query.contains("what can you do") -> {
            "I can perform a wide range of tasks offline, including:\n\n" +
            "1. **Local Telemetry & Code Audits**: Query local memory usage and execute diagnostics.\n" +
            "2. **Algorithm Implementation**: Write clean, optimized code blocks in Kotlin, Dart, Python, C++.\n" +
            "3. **Markdown Text Formatting**: Author structured lists, tables, and documentation.\n" +
            "4. **System Hyperparameter Tuning**: Adjust parameters like temperature, top_p, max_tokens.\n\n" +
            "No data ever leaves this device, protecting your IP and data security completely."
        }
        else -> {
            "I have processed your query using **$modelLabel** inference pipeline:\n\n" +
            "**Active Hyperparameters:**\n" +
            "- Temperature: `" + "%.1f".format(params.temperature) + "`\n" +
            "- Top-P: `" + "%.2f".format(params.topP) + "`\n" +
            "- Top-K: `" + params.topK + "`\n" +
            "- Max Tokens: `" + params.maxTokens + "`\n\n" +
            "1. **Local Processing Context**: The offline weights were loaded into system RAM.\n" +
            "2. **Security & Speed Benefits**: High-performance offline responses with zero internet reliance.\n" +
            "3. **State Management**: Your chat history is saved in a paginated SQLite DB.\n\n" +
            "Is there anything specific you'd like to implement, debug, or write next?"
        }
    }

    override suspend fun stopGeneration() {
        // No-op for simulated engine
    }

    override suspend fun unloadModel() {
        _isLoaded = false
        _currentModelId = null
        _currentTemplate = null
    }

    override fun dispose() {
        _isLoaded = false
        _currentModelId = null
        _currentTemplate = null
    }
}
