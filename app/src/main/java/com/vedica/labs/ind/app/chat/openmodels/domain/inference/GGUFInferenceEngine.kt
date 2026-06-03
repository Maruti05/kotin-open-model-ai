package com.vedica.labs.ind.app.chat.openmodels.domain.inference

import android.os.Debug
import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaBridge
import com.vedica.labs.ind.app.chat.openmodels.data.model.InferenceParams
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GGUFInferenceEngine @Inject constructor() : InferenceEngine {

    private var _isLoaded = false
    private var _currentModelId: String? = null
    private var _currentTemplate: String? = null
    private var _contextSize = 2048
    private var _numThreads = 4

    @Volatile
    private var _generating = false

    override val isLoaded: Boolean get() = _isLoaded
    override val loaderName: String get() = "Llamatik (llama.cpp)"
    override val currentModelId: String? get() = _currentModelId
    override val currentTemplate: String? get() = _currentTemplate

    companion object {
        private val MAGIC_GGUF = byteArrayOf(0x47, 0x47, 0x55, 0x46)
        private const val CHARS_PER_TOKEN = 3.5f
        private const val RESPONSE_RESERVE_RATIO = 0.55f
    }

    override suspend fun loadModel(
        modelId: String,
        modelPath: String,
        hyperparams: Map<String, Any>?
    ): Boolean {
        Timber.tag("GGUF").d("loadModel: id=%s, path=%s, hyperparams=%s", modelId, modelPath, hyperparams)

        unloadModel()

        val file = File(modelPath)
        if (!file.exists()) {
            Timber.tag("GGUF").e("Model file not found: %s", modelPath)
            throw Exception("Model file not found at: $modelPath")
        }

        val fileSize = file.length()
        Timber.tag("GGUF").d("Model file size: %s", formatBytes(fileSize))

        if (fileSize < 8192) {
            Timber.tag("GGUF").w("Model file too small/corrupt: %d bytes, deleting", fileSize)
            file.delete()
            throw Exception("Model file is corrupt or incomplete (${formatBytes(fileSize)}). File deleted.")
        }

        if (!isValidGguf(file)) {
            Timber.tag("GGUF").w("Invalid GGUF magic bytes, deleting file")
            file.delete()
            throw Exception("Model file is not a valid GGUF format. File deleted.")
        }

        _currentModelId = modelId
        _currentTemplate = ModelCatalog.getModelInfo(modelId)?.promptTemplate ?: "auto"
        Timber.tag("GGUF").d("Using prompt template: %s", _currentTemplate)

        try {
            val threads = (hyperparams?.get("threads") as? Number)?.toInt() ?: 4
            val contextSize = (hyperparams?.get("contextSize") as? Number)?.toInt() ?: 2048
            val gpuLayers = (hyperparams?.get("gpuLayers") as? Number)?.toInt() ?: 0

            _numThreads = threads.coerceIn(1, Runtime.getRuntime().availableProcessors())
            _contextSize = contextSize.coerceIn(128, 32768)

            Timber.tag("GGUF").d("Params: threads=%d, contextSize=%d, gpuLayers=%d", _numThreads, _contextSize, gpuLayers)
            Timber.tag("GGUF").d("Available processors: %d", Runtime.getRuntime().availableProcessors())

            logMemory("before_model_load")

            LlamaBridge.updateGenerateParams(
                temperature = 0.7f,
                maxTokens = 512,
                topP = 0.9f,
                topK = 40,
                repeatPenalty = 1.1f,
                contextLength = _contextSize,
                numThreads = _numThreads,
                useMmap = true,
                flashAttention = false,
                batchSize = 512,
                gpuLayers = gpuLayers
            )

            Timber.tag("GGUF").d("Calling LlamaBridge.initGenerateModel...")
            val success = withContext(Dispatchers.IO) {
                LlamaBridge.initGenerateModel(modelPath)
            }
            Timber.tag("GGUF").d("initGenerateModel result: %b", success)

            if (!success) {
                throw Exception("Failed to initialize model via Llamatik.")
            }

            _isLoaded = true
            logMemory("after_model_load")
            Timber.tag("GGUF").i("Model loaded successfully: %s", modelId)
            return true
        } catch (e: Exception) {
            Timber.tag("GGUF").e(e, "loadModel failed for: %s", modelId)
            _currentModelId = null
            _currentTemplate = null
            throw e
        }
    }

    override fun generateChat(
        messages: List<ChatMessage>,
        template: String?,
        params: InferenceParams
    ): Flow<String> = callbackFlow {
        if (!_isLoaded) {
            Timber.tag("GGUF").w("generateChat called but model not loaded")
            close(Exception("Model not loaded"))
            return@callbackFlow
        }
        if (_generating) {
            Timber.tag("GGUF").w("generateChat called but generation already in progress")
            close(Exception("Generation already in progress"))
            return@callbackFlow
        }
        _generating = true

        Timber.tag("GGUF").d("generateChat: messages=%d, template=%s, temp=%.2f, maxTokens=%d, topP=%.2f, topK=%d",
            messages.size, template, params.temperature, params.maxTokens, params.topP, params.topK)

        try {
            LlamaBridge.updateGenerateParams(
                temperature = params.temperature.toFloat().coerceIn(0.0f, 2.0f),
                maxTokens = params.maxTokens.coerceIn(1, _contextSize),
                topP = params.topP.toFloat().coerceIn(0.0f, 1.0f),
                topK = params.topK.coerceIn(1, 100),
                repeatPenalty = 1.1f,
                contextLength = _contextSize,
                numThreads = _numThreads,
                useMmap = true,
                flashAttention = false,
                batchSize = 512
            )

            val maxPromptTokens = (_contextSize * RESPONSE_RESERVE_RATIO).toInt().coerceAtLeast(64)
            val safeMessages = truncateMessages(messages, maxPromptTokens)

            Timber.tag("GGUF").d("Messages before truncation: %d, after: %d, maxPromptTokens: %d",
                messages.size, safeMessages.size, maxPromptTokens)

            val prompt = buildPrompt(safeMessages, template)
            Timber.tag("GGUF").d("Prompt length: %d chars (~%d tokens)", prompt.length, prompt.length / 4)

            // Reset the native KV cache between chat turns. Without this, the internal
            // key-value cache in llama.cpp accumulates unboundedly across generations,
            // eventually overflowing contextLength and corrupting native memory — causing
            // a silent SIGSEGV on the next generation. Since we send the full prompt each
            // turn (including conversation history), clearing the cache is always safe.
            Timber.tag("GGUF").d("Resetting session (KV cache)")
            try {
                LlamaBridge.sessionReset()
                Timber.tag("GGUF").d("Session reset OK")
            } catch (e: Exception) {
                Timber.tag("GGUF").w(e, "sessionReset failed (non-fatal)")
            }

            logMemory("before_generation")

            val genJob: Job = launch(Dispatchers.IO) {
                try {
                    if (!isActive) {
                        Timber.tag("GGUF").d("Generation cancelled before start")
                        return@launch
                    }

                    Timber.tag("GGUF").d("Starting LlamaBridge.generateStream...")
                    var tokenCount = 0
                    val startTime = System.currentTimeMillis()

                    LlamaBridge.generateStream(prompt, object : GenStream {
                        override fun onDelta(text: String) {
                            if (isActive && !isClosedForSend) {
                                trySend(text)
                                tokenCount++
                                if (tokenCount % 50 == 0) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    Timber.tag("GGUF").d("Streaming: %d tokens, %.1f sec elapsed", tokenCount, elapsed / 1000f)
                                }
                            }
                        }

                        override fun onComplete() {
                            val elapsed = System.currentTimeMillis() - startTime
                            Timber.tag("GGUF").d("Generation complete: %d tokens in %.2f sec (%.1f tok/s)",
                                tokenCount, elapsed / 1000f, if (elapsed > 0) tokenCount / (elapsed / 1000f) else 0f)
                            if (!isClosedForSend) {
                                trySend("[DONE]")
                            }
                            close()
                        }

                        override fun onError(message: String) {
                            Timber.tag("GGUF").e("generateStream error: %s", message)
                            close(Exception(message))
                        }
                    })
                } catch (e: Exception) {
                    Timber.tag("GGUF").e(e, "Generation thread exception")
                    if (!isClosedForSend) {
                        close(e)
                    }
                }
            }

            awaitClose {
                _generating = false
                Timber.tag("GGUF").d("Flow cancelled - cancelling generation")
                try {
                    LlamaBridge.nativeCancelGenerate()
                    Timber.tag("GGUF").d("nativeCancelGenerate called")
                } catch (e: Exception) {
                    Timber.tag("GGUF").w(e, "nativeCancelGenerate threw")
                }
                genJob.cancel()
                logMemory("after_generation")
            }
        } catch (e: Exception) {
            _generating = false
            Timber.tag("GGUF").e(e, "generateChat setup failed")
            close(e)
        }
    }.flowOn(Dispatchers.Default)

    private fun estimateTokens(text: String): Int {
        return (text.length / CHARS_PER_TOKEN).toInt().coerceAtLeast(1)
    }

    private fun truncateMessages(
        messages: List<ChatMessage>,
        maxTokens: Int
    ): List<ChatMessage> {
        // Reserve 55% of context for the response, use 45% for the prompt.
        // Drop oldest conversation turns (not system messages) to fit.
        // This prevents the prompt from exceeding the model's context window,
        // which would cause llama_decode to fail with "could not find a KV slot".
        if (messages.isEmpty()) {
            Timber.tag("GGUF").d("truncateMessages: empty input")
            return messages
        }

        val systemMessages = messages.filter { it.role == "system" }
        val historyMessages = messages.filter { it.role != "system" }

        val systemTokens = systemMessages.sumOf { estimateTokens(it.content) }
        Timber.tag("GGUF").d("truncateMessages: system=%d msgs, %d tokens; history=%d msgs, maxTokens=%d",
            systemMessages.size, systemTokens, historyMessages.size, maxTokens)

        if (systemTokens >= maxTokens) {
            Timber.tag("GGUF").w("System messages alone exceed maxTokens! systemTokens=%d, maxTokens=%d", systemTokens, maxTokens)
            return systemMessages.ifEmpty { messages.takeLast(1) }
        }

        val availableForHistory = maxTokens - systemTokens
        val result = mutableListOf<ChatMessage>()
        result.addAll(systemMessages)

        val truncated = mutableListOf<ChatMessage>()
        var historyTokens = 0
        var droppedCount = 0
        for (msg in historyMessages.reversed()) {
            val msgTokens = estimateTokens(msg.content)
            if (historyTokens + msgTokens > availableForHistory) {
                droppedCount++
                continue
            }
            truncated.add(msg)
            historyTokens += msgTokens
        }

        result.addAll(truncated.reversed())

        if (droppedCount > 0) {
            Timber.tag("GGUF").d("Truncated %d old messages to fit context (%d/%d tokens used)",
                droppedCount, systemTokens + historyTokens, maxTokens)
        }

        return result
    }

    private fun buildPrompt(messages: List<ChatMessage>, template: String? = null): String {
        try {
            val pairs = messages.map { it.role to it.content }
            val rendered = LlamaBridge.applyChatTemplate(pairs, addAssistantPrefix = true)
            if (rendered != null) {
                Timber.tag("GGUF").d("Used model chat template for prompt")
                return rendered
            }
        } catch (e: Exception) {
            Timber.tag("GGUF").w(e, "applyChatTemplate failed")
        }

        // Fallback based on template hint
        if (template == "phi") {
            Timber.tag("GGUF").d("Using Phi template fallback")
            val sb = StringBuilder()
            messages.forEach { msg ->
                when (msg.role) {
                    "system" -> sb.append("${msg.content}\n\n")
                    "user" -> sb.append("Question: ${msg.content}\n\n")
                    "assistant" -> sb.append("Answer: ${msg.content}\n\n")
                }
            }
            sb.append("Answer: ")
            return sb.toString()
        }

        // Default ChatML fallback
        Timber.tag("GGUF").d("Falling back to ChatML template")
        val sb = StringBuilder()
        messages.forEach { msg ->
            when (msg.role) {
                "system" -> sb.append("<|im_start|>system\n${msg.content}<|im_end|>\n")
                "user" -> sb.append("<|im_start|>user\n${msg.content}<|im_end|>\n")
                "assistant" -> sb.append("<|im_start|>assistant\n${msg.content}<|im_end|>\n")
            }
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    private fun isValidGguf(file: File): Boolean {
        return try {
            val raf = RandomAccessFile(file, "r")
            val magic = ByteArray(4)
            raf.read(magic)
            raf.close()
            val valid = magic.contentEquals(MAGIC_GGUF)
            if (!valid) {
                Timber.tag("GGUF").w("Invalid GGUF magic: %02x %02x %02x %02x", magic[0], magic[1], magic[2], magic[3])
            }
            valid
        } catch (e: Exception) {
            Timber.tag("GGUF").e(e, "Failed to read GGUF magic bytes")
            false
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1073741824 -> "%.1f GB".format(bytes / 1073741824.0)
        bytes >= 1048576 -> "%.1f MB".format(bytes / 1048576.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    private fun logMemory(tag: String) {
        try {
            val rt = Runtime.getRuntime()
            val maxMem = rt.maxMemory()
            val totalMem = rt.totalMemory()
            val freeMem = rt.freeMemory()
            val usedMem = totalMem - freeMem
            val nativeHeap = Debug.getNativeHeapSize()
            val nativeAllocated = Debug.getNativeHeapAllocatedSize()

            Timber.tag("GGUF").d("Memory [%s]: JVM: %s/%s used/%s max | Native: %s alloc/%s size",
                tag,
                formatBytes(usedMem), formatBytes(totalMem), formatBytes(maxMem),
                formatBytes(nativeAllocated), formatBytes(nativeHeap))
        } catch (e: Exception) {
            Timber.tag("GGUF").w("Memory log failed: %s", e.message)
        }
    }

    override suspend fun stopGeneration() {
        Timber.tag("GGUF").d("stopGeneration called")
        try {
            LlamaBridge.nativeCancelGenerate()
            Timber.tag("GGUF").d("nativeCancelGenerate OK")
        } catch (e: Exception) {
            Timber.tag("GGUF").w(e, "nativeCancelGenerate in stopGeneration")
        }
    }

    override suspend fun unloadModel() {
        Timber.tag("GGUF").i("Unloading model: %s", _currentModelId)
        try {
            LlamaBridge.shutdown()
            Timber.tag("GGUF").d("LlamaBridge.shutdown OK")
        } catch (e: Exception) {
            Timber.tag("GGUF").w(e, "shutdown threw")
        }
        _generating = false
        _isLoaded = false
        _currentModelId = null
        _currentTemplate = null
        logMemory("after_unload")
    }

    override fun dispose() {
        Timber.tag("GGUF").i("Dispose called")
        try {
            LlamaBridge.shutdown()
        } catch (_: Exception) {}
        _generating = false
        _isLoaded = false
        _currentModelId = null
        _currentTemplate = null
    }
}
