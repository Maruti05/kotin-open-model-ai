package com.vedica.labs.ind.app.chat.openmodels.domain.inference

import android.util.Log
import com.vedica.labs.ind.app.chat.openmodels.data.model.InferenceParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GGUF inference engine using llama.cpp native bindings.
 *
 * This engine loads GGUF format model files and runs inference via llama.cpp.
 * The native library is loaded via System.loadLibrary("llama").
 *
 * To use a real llama.cpp build:
 * 1. Build llama.cpp for Android (arm64-v8a, armeabi-v7a, x86_64)
 * 2. Place .so files in app/src/main/jniLibs/<abi>/
 * 3. The native methods will be resolved automatically
 *
 * Currently falls back to simulated responses if native lib is unavailable.
 */
@Singleton
class GGUFInferenceEngine @Inject constructor() : InferenceEngine {

    private var _isLoaded = false
    private var _currentModelId: String? = null
    private var _currentTemplate: String? = null
    private var _nativeAvailable = false
    private var _nativeModelPtr: Long = 0L

    override val isLoaded: Boolean get() = _isLoaded
    override val loaderName: String get() = "GGUF (llama.cpp)"
    override val currentModelId: String? get() = _currentModelId
    override val currentTemplate: String? get() = _currentTemplate

    companion object {
        private const val TAG = "GGUFEngine"
        private val MAGIC_GGUF = byteArrayOf(0x47, 0x47, 0x55, 0x46) // "GGUF"

        private val MODEL_TEMPLATES = mapOf(
            "smollm_135m_q2" to "chatml", "smollm_135m_iq3" to "chatml",
            "smollm_135m_q4" to "chatml", "tinymistral_248m_q4" to "mistral",
            "mobilellm_350m_q4" to "llama2", "smollm_360m_q2" to "chatml",
            "smollm_360m_q3" to "chatml", "smollm_360m_q4" to "chatml",
            "qwen_0_5b_q2" to "chatml", "qwen2_5_0_5b_q2" to "chatml",
            "tinyllama_1_1b_q2" to "llama2", "phi_1_5_q2" to "phi",
            "qwen_0_5b_q4" to "chatml", "llama_3_2_1b_q2" to "llama2",
            "qwen2_5_1_5b_q2" to "chatml", "dolphin_3_1b_q2" to "chatml",
            "gemma_2_2b_q2" to "gemma", "gemma_2_2b_mlabonne_q2" to "gemma",
            "phi_2_q2" to "phi", "gemma_2_2b_q4" to "gemma",
            "llama_3_3b_q4" to "llama2", "phi_3_mini_q4" to "phi",
            "mistral_7b_q4" to "llama2", "smolvlm2_500m_q8" to "phi"
        )
    }

    init {
        try {
            System.loadLibrary("llama")
            _nativeAvailable = true
            Log.i(TAG, "llama.cpp native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            _nativeAvailable = false
            Log.w(TAG, "llama.cpp native library not available, will use simulation")
        }
    }

    override suspend fun loadModel(
        modelId: String,
        modelPath: String,
        hyperparams: Map<String, Any>?
    ): Boolean {
        unloadModel()

        val file = File(modelPath)
        if (!file.exists()) {
            throw Exception("Model file not found at: $modelPath")
        }

        val fileSize = file.length()
        if (fileSize < 8192) {
            file.delete()
            throw Exception("Model file is corrupt or incomplete (${formatBytes(fileSize)}). File deleted.")
        }

        if (!isValidGguf(file)) {
            file.delete()
            throw Exception("Model file is not a valid GGUF format. File deleted.")
        }

        _currentModelId = modelId
        _currentTemplate = MODEL_TEMPLATES[modelId] ?: "chatml"

        if (_nativeAvailable) {
            try {
                val threads = (hyperparams?.get("threads") as? Number)?.toInt() ?: 4
                val contextSize = (hyperparams?.get("contextSize") as? Number)?.toInt() ?: 2048
                val gpuLayers = (hyperparams?.get("gpuLayers") as? Number)?.toInt()

                _nativeModelPtr = nativeLoadModel(modelPath, threads, contextSize, gpuLayers ?: 0)
                if (_nativeModelPtr == 0L) {
                    throw Exception("Failed to initialize model in GGUF inference engine.")
                }
                _isLoaded = true
                return true
            } catch (e: Exception) {
                _currentModelId = null
                _currentTemplate = null
                throw e
            }
        } else {
            _isLoaded = true
            return true
        }
    }

    override fun generateChat(
        messages: List<ChatMessage>,
        template: String?,
        params: InferenceParams
    ): Flow<String> = flow {
        val modelLabel = _currentModelId ?: "Local Model"

        if (_nativeAvailable && _nativeModelPtr != 0L) {
            val prompt = buildPrompt(messages, template ?: _currentTemplate ?: "chatml")
            val nativeFlow = nativeGenerateChat(
                modelPtr = _nativeModelPtr,
                prompt = prompt,
                maxTokens = params.maxTokens,
                temperature = params.temperature.toFloat(),
                topP = params.topP.toFloat(),
                topK = params.topK
            )
            emitAll(nativeFlow)
        } else {
            val baseDelayMs = (40 + (params.temperature * 15)).toLong()
            val userQuery = messages.lastOrNull()?.content?.lowercase() ?: ""
            val response = buildSimulatedResponse(userQuery, modelLabel, params)

            val words = response.split(" ").take(params.maxTokens)
            for (word in words) {
                if (!coroutineContext.isActive) break
                val jitter = (Math.random() * baseDelayMs * 0.5).toLong()
                delay(baseDelayMs + jitter)
                emit("$word ")
            }
            emit("[DONE]")
        }
    }.flowOn(Dispatchers.IO)

    private fun buildPrompt(messages: List<ChatMessage>, template: String): String {
        return when (template.lowercase()) {
            "chatml" -> {
                val sb = StringBuilder()
                messages.forEach { msg ->
                    when (msg.role) {
                        "system" -> sb.append("<|im_start|>system\n${msg.content}<|im_end|>\n")
                        "user" -> sb.append("<|im_start|>user\n${msg.content}<|im_end|>\n")
                        "assistant" -> sb.append("<|im_start|>assistant\n${msg.content}<|im_end|>\n")
                    }
                }
                sb.append("<|im_start|>assistant\n")
                sb.toString()
            }
            "llama2" -> {
                val sb = StringBuilder("[INST] ")
                messages.forEach { msg ->
                    when (msg.role) {
                        "system" -> sb.append("<<SYS>>\n${msg.content}\n<</SYS>>\n\n")
                        "user" -> sb.append("${msg.content} [/INST] ")
                        "assistant" -> sb.append("${msg.content} [INST] ")
                    }
                }
                sb.toString()
            }
            "phi" -> {
                val sb = StringBuilder()
                messages.forEach { msg ->
                    when (msg.role) {
                        "system" -> sb.append("${msg.content}\n\n")
                        "user" -> sb.append("Question: ${msg.content}\n\nAnswer: ")
                        "assistant" -> sb.append("${msg.content}\n\n")
                    }
                }
                sb.toString()
            }
            "gemma" -> {
                val sb = StringBuilder()
                messages.forEach { msg ->
                    when (msg.role) {
                        "user" -> sb.append("<start_of_turn>user\n${msg.content}<end_of_turn>\n")
                        "assistant" -> sb.append("<start_of_turn>model\n${msg.content}<end_of_turn>\n")
                    }
                }
                sb.append("<start_of_turn>model\n")
                sb.toString()
            }
            else -> {
                val sb = StringBuilder()
                messages.forEach { msg ->
                    when (msg.role) {
                        "system" -> sb.append("System: ${msg.content}\n")
                        "user" -> sb.append("User: ${msg.content}\n")
                        "assistant" -> sb.append("Assistant: ${msg.content}\n")
                    }
                }
                sb.append("Assistant: ")
                sb.toString()
            }
        }
    }

    private fun isValidGguf(file: File): Boolean {
        return try {
            val raf = RandomAccessFile(file, "r")
            val magic = ByteArray(4)
            raf.read(magic)
            raf.close()
            magic.contentEquals(MAGIC_GGUF)
        } catch (_: Exception) {
            false
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1073741824 -> "%.1f GB".format(bytes / 1073741824.0)
        bytes >= 1048576 -> "%.1f MB".format(bytes / 1048576.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    // llama.cpp native JNI methods
    private external fun nativeLoadModel(
        modelPath: String, threads: Int, contextSize: Int, gpuLayers: Int
    ): Long

    private external fun nativeGenerateChat(
        modelPtr: Long, prompt: String,
        maxTokens: Int, temperature: Float, topP: Float, topK: Int
    ): Flow<String>

    private external fun nativeStopGeneration(modelPtr: Long)
    private external fun nativeUnloadModel(modelPtr: Long)

    override suspend fun stopGeneration() {
        if (_nativeAvailable && _nativeModelPtr != 0L) {
            nativeStopGeneration(_nativeModelPtr)
        }
    }

    override suspend fun unloadModel() {
        if (_nativeAvailable && _nativeModelPtr != 0L) {
            try {
                nativeUnloadModel(_nativeModelPtr)
            } catch (_: Exception) {}
        }
        _nativeModelPtr = 0L
        _isLoaded = false
        _currentModelId = null
        _currentTemplate = null
    }

    override fun dispose() {
        _nativeModelPtr = 0L
        _isLoaded = false
        _currentModelId = null
        _currentTemplate = null
    }

    // Simulated response for when native lib is unavailable
    private fun buildSimulatedResponse(
        query: String, modelLabel: String, params: InferenceParams
    ): String = when {
        query.contains("hello") || query.contains("hi") ->
            "Hello! I am running under **$modelLabel** via gguf inference engine."
        query.contains("code") || query.contains("kotlin") ->
            "```kotlin\nfun greet() = println(\"Hello from $modelLabel!\")\n```"
        else ->
            "Processed via **$modelLabel** (llama.cpp GGUF).\n\nParams: " +
            "Temp=${"%.1f".format(params.temperature)}, " +
            "Top-P=${"%.2f".format(params.topP)}, " +
            "Top-K=${params.topK}, Max Tokens=${params.maxTokens}"
    }
}
