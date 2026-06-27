package com.vedica.labs.ind.app.chat.openmodels.domain.inference

import com.vedica.labs.ind.app.chat.openmodels.data.model.BackendType
import com.vedica.labs.ind.app.chat.openmodels.data.model.InferenceParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.min
import kotlin.random.Random

interface Tokenizer {
    fun encode(text: String): List<Int>
    fun decode(tokens: List<Int>): String
}

@Singleton
class LiteRTInferenceEngine @Inject constructor() : InferenceEngine {

    private var _isLoaded = false
    private var _currentModelId: String? = null
    private var _currentTemplate: String? = null
    private var _contextSize = 2048
    private var _numThreads = 4
    private var _vocabSize = 0
    private var _inputLength = 0

    @Volatile
    private var _generating = false

    private var interpreter: Interpreter? = null
    private var modelTokenizer: Tokenizer? = null

    override val isLoaded: Boolean get() = _isLoaded
    override val loaderName: String get() = "LiteRT (XNNPACK)"
    override val backendType: BackendType get() = BackendType.LITERT
    override val currentModelId: String? get() = _currentModelId
    override val currentTemplate: String? get() = _currentTemplate

    companion object {
        private const val TAG = "LiteRT"
        private const val CHARS_PER_TOKEN = 3.5f
        private const val RESPONSE_RESERVE_RATIO = 0.55f
        private const val EOS_TOKEN_ID = 1
    }

    override suspend fun loadModel(
        modelId: String,
        modelPath: String,
        hyperparams: Map<String, Any>?
    ): Boolean = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("loadModel: id=%s, path=%s", modelId, modelPath)
        unloadModel()

        val file = File(modelPath)
        if (!file.exists()) {
            Timber.tag(TAG).e("Model file not found: %s", modelPath)
            throw Exception("Model file not found at: $modelPath")
        }

        val fileSize = file.length()
        if (fileSize < 8192) {
            Timber.tag(TAG).w("Model file too small/corrupt: %d bytes", fileSize)
            file.delete()
            throw Exception("Model file is corrupt or incomplete. File deleted.")
        }

        _currentModelId = modelId
        _currentTemplate = resolveTemplate(modelId)
        Timber.tag(TAG).d("Using prompt template: %s", _currentTemplate)

        try {
            val threads = (hyperparams?.get("threads") as? Number)?.toInt() ?: 4
            val contextSize = (hyperparams?.get("contextSize") as? Number)?.toInt() ?: 2048

            _numThreads = threads.coerceIn(1, Runtime.getRuntime().availableProcessors())
            _contextSize = contextSize.coerceIn(128, 32768)

            Timber.tag(TAG).d("Params: threads=%d, contextSize=%d", _numThreads, _contextSize)

            val modelBuffer = loadModelFile(file)

            val options = Interpreter.Options()
                .setNumThreads(_numThreads)
                .setUseXNNPACK(true)

            val interpreter = Interpreter(modelBuffer, options)
            this@LiteRTInferenceEngine.interpreter = interpreter

            val inputTensor = interpreter.getInputTensor(0)
            val inputShape = inputTensor.shape()
            _inputLength = inputShape.fold(1) { acc, dim -> (acc * dim.coerceAtLeast(1)).coerceAtMost(16384) }

            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            val outDataType = outputTensor.dataType()
            _vocabSize = if (outputShape.isNotEmpty()) outputShape.last().coerceAtLeast(1) else 32000

            Timber.tag(TAG).d("Model loaded: input shape=%s(type=%s), output shape=%s(type=%s), inputLen=%d, vocab=%d",
                inputShape.contentToString(), inputTensor.dataType(),
                outputShape.contentToString(), outDataType,
                _inputLength, _vocabSize)

            modelTokenizer = loadTokenizer(modelId, file.parentFile ?: file.parentFile)

            _isLoaded = true
            Timber.tag(TAG).i("Model loaded successfully: %s (%s)", modelId, loaderName)
            return@withContext true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "loadModel failed for: %s", modelId)
            _currentModelId = null
            _currentTemplate = null
            interpreter?.close()
            interpreter = null
            throw e
        }
    }

    override fun generateChat(
        messages: List<ChatMessage>,
        template: String?,
        params: InferenceParams
    ): Flow<String> = callbackFlow {
        if (!_isLoaded) {
            close(Exception("Model not loaded"))
            return@callbackFlow
        }
        if (_generating) {
            close(Exception("Generation already in progress"))
            return@callbackFlow
        }
        _generating = true

        Timber.tag(TAG).d("generateChat: messages=%d, template=%s", messages.size, template)

        try {
            val effectiveTemplate = template ?: _currentTemplate ?: "chatml"
            val prompt = buildPrompt(messages, effectiveTemplate)

            val maxPromptTokens = (_contextSize * RESPONSE_RESERVE_RATIO).toInt().coerceAtLeast(64)
            val tok: Tokenizer = modelTokenizer ?: SafeTokenizer(_vocabSize)
            val inputTokens = tok.encode(prompt)
            val truncatedTokens = inputTokens.take(maxPromptTokens)

            Timber.tag(TAG).d("Prompt tokens: %d (truncated to %d)", inputTokens.size, truncatedTokens.size)

            val genJob: Job = launch(Dispatchers.IO) {
                try {
                    if (!isActive) return@launch
                    generateTokens(truncatedTokens, tok, params) { token ->
                        if (isActive && !isClosedForSend) {
                            trySend(token)
                        }
                    }
                    if (!isClosedForSend) {
                        trySend("[DONE]")
                    }
                    close()
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Generation failed")
                    if (!isClosedForSend) close(e)
                }
            }

            awaitClose {
                _generating = false
                genJob.cancel()
            }
        } catch (e: Exception) {
            _generating = false
            Timber.tag(TAG).e(e, "generateChat setup failed")
            close(e)
        }
    }.flowOn(Dispatchers.Default)

    @Suppress("UNCHECKED_CAST")
    private fun generateTokens(
        promptTokens: List<Int>,
        tok: Tokenizer,
        params: InferenceParams,
        onToken: (String) -> Unit
    ) {
        val interp = this.interpreter
            ?: throw Exception("Interpreter not initialized")

        val inputLen = _inputLength.coerceAtLeast(1)
        val maxGenTokens = params.maxTokens.coerceIn(1, 2048)
        val fullSequence = promptTokens.toMutableList()

        val inputShape = interp.getInputTensor(0).shape()
        val input = buildTensorArray<Int>(inputShape, 0)
        val output = buildTensorArray<Float>(interp.getOutputTensor(0).shape(), 0.0f)

        for (step in 0 until maxGenTokens) {
            if (!_generating) break

            val startIdx = (fullSequence.size - min(fullSequence.size, inputLen - 1))
                .coerceAtLeast(0)
            val segment = fullSequence.subList(startIdx, fullSequence.size)

            fillTensorArray(input, segment)

            zeroTensorArray(output)
            interp.run(input, output)

            val logits = extractLastLogits(output)
            val nextTokenId = sampleToken(logits, params)

            if (nextTokenId == EOS_TOKEN_ID) {
                Timber.tag(TAG).d("EOS token at step %d", step)
                break
            }

            fullSequence.add(nextTokenId)

            val decoded = tok.decode(listOf(nextTokenId))
            if (decoded.isNotEmpty()) {
                onToken(decoded)
            }
        }
    }

    // ── Generic tensor array helpers ────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun <T> buildTensorArray(shape: IntArray, fill: T): Any {
        val dims = shape.map { it.coerceAtLeast(1) }
        return when (dims.size) {
            0 -> throw Exception("Empty tensor shape")
            1 -> when (fill) {
                is Int -> IntArray(dims[0])
                is Float -> FloatArray(dims[0])
                else -> throw Exception("Unsupported element type: ${fill?.javaClass?.name}")
            }
            2 -> when (fill) {
                is Int -> Array(dims[0]) { IntArray(dims[1]) }
                is Float -> Array(dims[0]) { FloatArray(dims[1]) }
                else -> throw Exception("Unsupported element type: ${fill?.javaClass?.name}")
            }
            3 -> when (fill) {
                is Int -> Array(dims[0]) { Array(dims[1]) { IntArray(dims[2]) } }
                is Float -> Array(dims[0]) { Array(dims[1]) { FloatArray(dims[2]) } }
                else -> throw Exception("Unsupported element type: ${fill?.javaClass?.name}")
            }
            4 -> when (fill) {
                is Int -> Array(dims[0]) { Array(dims[1]) { Array(dims[2]) { IntArray(dims[3]) } } }
                is Float -> Array(dims[0]) { Array(dims[1]) { Array(dims[2]) { FloatArray(dims[3]) } } }
                else -> throw Exception("Unsupported element type: ${fill?.javaClass?.name}")
            }
            else -> throw Exception("Unsupported tensor rank: ${dims.size}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fillTensorArray(arr: Any, values: List<Int>) {
        when (arr) {
            is IntArray -> {
                arr.fill(0)
                val n = min(values.size, arr.size)
                for (i in 0 until n) arr[i] = values[i]
            }
            is Array<*> -> {
                if (arr.isNotEmpty()) {
                    val first = arr[0]
                    if (first is IntArray) {
                        (arr as Array<IntArray>).forEach { it.fill(0) }
                        val inner = arr[0]
                        val n = min(values.size, inner.size)
                        for (i in 0 until n) inner[i] = values[i]
                    } else if (first is Array<*>) {
                        val flat = flattenToIndices(arr)
                        flat.fill(0)
                        val n = min(values.size, flat.size)
                        for (i in 0 until n) flat[i] = values[i]
                        fillFromFlattened(arr, flat)
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun flattenToIndices(arr: Any): MutableList<Int> {
        val result = mutableListOf<Int>()
        when (arr) {
            is IntArray -> arr.forEach { result.add(it) }
            is Array<*> -> arr.forEach { result.addAll(flattenToIndices(it as Any)) }
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun fillFromFlattened(arr: Any, flat: List<Int>) {
        var idx = 0
        fun fill(a: Any) {
            when (a) {
                is IntArray -> for (i in a.indices) { a[i] = flat[idx]; idx++ }
                is Array<*> -> a.forEach { fill(it as Any) }
            }
        }
        fill(arr)
    }

    @Suppress("UNCHECKED_CAST")
    private fun zeroTensorArray(arr: Any) {
        when (arr) {
            is FloatArray -> arr.fill(0.0f)
            is Array<*> -> arr.forEach { zeroTensorArray(it as Any) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractLastLogits(output: Any): FloatArray {
        return when (output) {
            is FloatArray -> output
            is Array<*> -> {
                if (output.isEmpty()) FloatArray(0)
                else extractLastLogits(output[output.size - 1] as Any)
            }
            else -> FloatArray(0)
        }
    }

    private fun sampleToken(logits: FloatArray, params: InferenceParams): Int {
        val temperature = params.temperature.toFloat().coerceIn(0.01f, 2.0f)
        val topK = params.topK.coerceIn(1, _vocabSize.coerceAtLeast(1))
        val topP = params.topP.toFloat().coerceIn(0.0f, 1.0f)

        val indexed = logits.mapIndexed { idx, value -> idx to value }
            .sortedByDescending { it.second }
            .take(topK)

        val maxLogit = indexed.maxOf { it.second }
        val expSum = indexed.sumOf {
            val v = exp(((it.second - maxLogit) / temperature).toDouble())
            v
        }

        val probabilities = indexed.map { (idx, value) ->
            idx to exp(((value - maxLogit) / temperature).toDouble()) / expSum
        }

        var cumulative = 0.0
        val nucleusCandidates = mutableListOf<Pair<Int, Double>>()
        for ((idx, prob) in probabilities) {
            cumulative += prob
            nucleusCandidates.add(idx to prob)
            if (cumulative >= topP) break
        }

        if (nucleusCandidates.isEmpty()) {
            return probabilities.firstOrNull()?.first ?: 0
        }

        val totalProb = nucleusCandidates.sumOf { it.second }
        val rand = Random.nextDouble() * totalProb
        var accum = 0.0
        for ((idx, prob) in nucleusCandidates) {
            accum += prob
            if (rand <= accum) return idx
        }

        return nucleusCandidates.last().first
    }

    private fun buildPrompt(messages: List<ChatMessage>, template: String): String {
        return when (template.lowercase()) {
            "gemma" -> buildGemmaPrompt(messages)
            "phi" -> buildPhiPrompt(messages)
            "llama2", "mistral" -> buildLlama2Prompt(messages)
            else -> buildChatMLPrompt(messages)
        }
    }

    private fun buildGemmaPrompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        for (msg in messages) {
            when (msg.role) {
                "system" -> sb.append("${msg.content}\n\n")
                "user" -> sb.append("<start_of_turn>user\n${msg.content}<end_of_turn>\n")
                "assistant" -> sb.append("<start_of_turn>model\n${msg.content}<end_of_turn>\n")
            }
        }
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }

    private fun buildLlama2Prompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder("<s>")
        var systemDone = false
        for (msg in messages) {
            when (msg.role) {
                "system" -> {
                    sb.append("[INST] <<SYS>>\n${msg.content}\n<</SYS>>\n\n")
                    systemDone = true
                }
                "user" -> {
                    if (systemDone) sb.append("${msg.content} [/INST] ")
                    else { sb.append("[INST] ${msg.content} [/INST] "); systemDone = true }
                }
                "assistant" -> sb.append("${msg.content} </s><s>")
            }
        }
        return sb.toString()
    }

    private fun buildPhiPrompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        for (msg in messages) {
            when (msg.role) {
                "system" -> sb.append("${msg.content}\n\n")
                "user" -> sb.append("Question: ${msg.content}\n\n")
                "assistant" -> sb.append("Answer: ${msg.content}\n\n")
            }
        }
        sb.append("Answer: ")
        return sb.toString()
    }

    private fun buildChatMLPrompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        for (msg in messages) {
            when (msg.role) {
                "system" -> sb.append("<|im_start|>system\n${msg.content}<|im_end|>\n")
                "user" -> sb.append("<|im_start|>user\n${msg.content}<|im_end|>\n")
                "assistant" -> sb.append("<|im_start|>assistant\n${msg.content}<|im_end|>\n")
            }
        }
        sb.append("<|im_start|>assistant\n")
        return sb.toString()
    }

    private fun loadModelFile(file: File): MappedByteBuffer {
        return RandomAccessFile(file, "r").use { raf ->
            raf.channel.map(FileChannel.MapMode.READ_ONLY, 0, raf.length())
        }
    }

    private fun loadTokenizer(modelId: String, modelDir: File): Tokenizer? {
        val jsonFile = File(modelDir, "tokenizer.json")
        if (jsonFile.exists()) {
            Timber.tag(TAG).d("Loading tokenizer from %s (vocabSize=%d)", jsonFile.absolutePath, _vocabSize)
            VocabTokenizer.load(jsonFile, _vocabSize)?.let {
                Timber.tag(TAG).d("Tokenizer loaded successfully from %s", jsonFile.absolutePath)
                return it
            }
            Timber.tag(TAG).w("Failed to parse tokenizer.json, falling back to SafeTokenizer")
        } else {
            Timber.tag(TAG).d("No tokenizer.json at %s", jsonFile.absolutePath)
        }
        val spmCandidates = listOf(
            File(modelDir, "tokenizer.spm"),
            File(modelDir, "tokenizer.model"),
            File(modelDir.parentFile, "tokenizer.model"),
            File(modelDir, "$modelId.spm")
        )
        val existing = spmCandidates.firstOrNull { it.exists() }
        if (existing != null) {
            Timber.tag(TAG).d("Found tokenizer: %s (SentencePiece not supported, will use SafeTokenizer)", existing.absolutePath)
        }
        Timber.tag(TAG).w("No usable tokenizer for %s, will create SafeTokenizer on use", modelId)
        return null
    }

    private fun resolveTemplate(modelId: String): String {
        return when {
            modelId.startsWith("gemma") -> "gemma"
            modelId.startsWith("phi") -> "phi"
            modelId.startsWith("llama") || modelId.startsWith("mistral") || modelId.startsWith("tinyllama") -> "llama2"
            else -> "chatml"
        }
    }



    override suspend fun stopGeneration() {
        Timber.tag(TAG).d("stopGeneration called")
        _generating = false
    }

    override suspend fun unloadModel() {
        Timber.tag(TAG).i("Unloading model: %s", _currentModelId)
        _generating = false
        _isLoaded = false
        _currentModelId = null
        _currentTemplate = null
        interpreter?.close()
        interpreter = null
        modelTokenizer = null
    }

    override fun dispose() {
        Timber.tag(TAG).i("Dispose called")
        _generating = false
        _isLoaded = false
        _currentModelId = null
        _currentTemplate = null
        interpreter?.close()
        interpreter = null
        modelTokenizer = null
    }
}

class SafeTokenizer(private val vocabSize: Int) : Tokenizer {
    private val maxTokenId: Int = (vocabSize - 1).coerceAtLeast(3)
    private val minTokenId: Int = 3
    private val range: Int = (maxTokenId - minTokenId).coerceAtLeast(1)
    private val byteRange: Int = min(range, 256)

    @Suppress("UNUSED_PARAMETER")
    constructor(vocabSize: Int, modelPath: String) : this(vocabSize) {
        // modelPath is reserved for future SentencePiece/tokenizer.json loading
    }

    override fun encode(text: String): List<Int> {
        val bytes = text.encodeToByteArray()
        val tokens = mutableListOf<Int>()
        for (b in bytes) {
            tokens.add(((b.toInt() and 0xFF) % byteRange) + minTokenId)
        }
        return tokens
    }

    override fun decode(tokens: List<Int>): String {
        val bytes = ByteArray(tokens.size) { i ->
            val token = tokens[i]
            if (token in minTokenId..maxTokenId) {
                ((token - minTokenId) % byteRange).toByte()
            } else 0
        }
        return try {
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            String(bytes, Charsets.ISO_8859_1)
        }
    }
}
