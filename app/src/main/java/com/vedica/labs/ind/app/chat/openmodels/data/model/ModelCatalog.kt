package com.vedica.labs.ind.app.chat.openmodels.data.model

object ModelCatalog {

    val models: List<ModelInfo> = listOf(
        // ====================================================================
        // LLAMA.CPP / GGUF MODELS — Apache 2.0 or MIT, < 2GB, no license gate
        // ====================================================================

        // ── TIER 3 (< 500 MB) ────────────────────────────────────────────────
        ModelInfo(
            id = "smollm_135m_q4",
            name = "SmolLM2-135M Q4",
            params = "135M",
            sizeMb = 120.0,
            minRamGb = 0.5,
            tier = 3,
            description = "Ultra-compact instruct model for sub-1GB devices; best quality-to-size ratio in the 135M class",
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf",
            architecture = "llama",
            quantization = "Q4_K_M",
            contextWindow = 2048,
            maxOutputTokens = 256,
            promptTemplate = "chatml",
            license = "apache-2.0",
            useCase = "chat",
            languages = "english",
            supportsToolCalling = true
        ),
        ModelInfo(
            id = "smollm_360m_q4",
            name = "SmolLM2-360M Q4",
            params = "360M",
            sizeMb = 320.0,
            minRamGb = 1.0,
            tier = 3,
            description = "Balanced 360M instruct model with function-calling support; ideal for 2GB RAM devices",
            downloadUrl = "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf",
            architecture = "llama",
            quantization = "Q4_K_M",
            contextWindow = 2048,
            maxOutputTokens = 384,
            promptTemplate = "chatml",
            license = "apache-2.0",
            useCase = "chat",
            languages = "english",
            supportsToolCalling = true
        ),
        ModelInfo(
            id = "qwen2_5_0_5b_q4",
            name = "Qwen2.5-0.5B Q4",
            params = "0.5B",
            sizeMb = 491.0,
            minRamGb = 1.5,
            tier = 3,
            description = "Qwen2.5 0.5B at Q4 — multilingual, 32K context, strong reasoning for its size; top-tier ultra-small model",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            architecture = "qwen2",
            quantization = "Q4_K_M",
            contextWindow = 32768,
            maxOutputTokens = 512,
            promptTemplate = "chatml",
            license = "apache-2.0",
            useCase = "chat",
            languages = "multilingual",
            supportsToolCalling = true
        ),
        ModelInfo(
            id = "smolvlm2_500m_q8",
            name = "SmolVLM2-500M Q8",
            params = "500M",
            sizeMb = 437.0,
            minRamGb = 2.0,
            tier = 3,
            description = "Vision-language model: understand images and video frames. Apache 2.0, under 500MB",
            downloadUrl = "https://huggingface.co/D-Robotics/SmolVLM2-500M-Video-Instruct-GGUF-BPU/resolve/main/SmolVLM2-500M-Video-Instruct-Q8_0.gguf",
            architecture = "llama",
            quantization = "Q8_0",
            contextWindow = 8192,
            maxOutputTokens = 512,
            promptTemplate = "phi",
            license = "apache-2.0",
            useCase = "vision",
            languages = "english",
            supportsVision = true
        ),

        // ── TIER 2 (500 MB – 1.5 GB) ────────────────────────────────────────
        ModelInfo(
            id = "tinyllama_1_1b_q4",
            name = "TinyLlama-1.1B Q4",
            params = "1.1B",
            sizeMb = 686.0,
            minRamGb = 2.0,
            tier = 2,
            description = "Compact 1.1B Llama-2 architecture at Q4; widely tested, performs well for its size",
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            architecture = "llama",
            quantization = "Q4_K_M",
            contextWindow = 2048,
            maxOutputTokens = 384,
            promptTemplate = "llama2",
            license = "apache-2.0",
            useCase = "chat",
            languages = "english"
        ),
        ModelInfo(
            id = "qwen2_5_1_5b_q4",
            name = "Qwen2.5-1.5B Q4",
            params = "1.5B",
            sizeMb = 986.0,
            minRamGb = 2.5,
            tier = 2,
            description = "Best quality-per-byte model under 1GB. 32K context, multilingual, strong reasoning",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            architecture = "qwen2",
            quantization = "Q4_K_M",
            contextWindow = 32768,
            maxOutputTokens = 512,
            promptTemplate = "chatml",
            license = "apache-2.0",
            useCase = "chat",
            languages = "multilingual",
            supportsToolCalling = true
        ),
        ModelInfo(
            id = "phi_1_5_q2",
            name = "Phi-1.5 Q2",
            params = "1.3B",
            sizeMb = 582.0,
            minRamGb = 2.0,
            tier = 2,
            description = "Microsoft's code-focused 1.3B Phi at Q2. MIT license, strong at code and reasoning benchmarks",
            downloadUrl = "https://huggingface.co/tensorblock/phi-1_5-GGUF/resolve/main/phi-1_5-Q2_K.gguf",
            architecture = "phi",
            quantization = "Q2_K",
            contextWindow = 2048,
            maxOutputTokens = 384,
            promptTemplate = "phi",
            license = "mit",
            useCase = "code",
            languages = "english"
        ),

        // ── TIER 1 (1.5 GB – 2 GB) ─────────────────────────────────────────
        ModelInfo(
            id = "phi_2_q2",
            name = "Phi-2 Q2",
            params = "2.7B",
            sizeMb = 1300.0,
            minRamGb = 3.0,
            tier = 1,
            description = "Microsoft's 2.7B Phi-2 at Q2 — MIT licensed, excellent reasoning and math capabilities",
            downloadUrl = "https://huggingface.co/TheBloke/phi-2-GGUF/resolve/main/phi-2.Q2_K.gguf",
            architecture = "phi",
            quantization = "Q2_K",
            contextWindow = 2048,
            maxOutputTokens = 512,
            promptTemplate = "phi",
            license = "mit",
            useCase = "reasoning",
            languages = "english"
        ),
        ModelInfo(
            id = "phi_3_5_mini_q2",
            name = "Phi-3.5-mini Q2",
            params = "3.8B",
            sizeMb = 1453.0,
            minRamGb = 3.5,
            tier = 1,
            description = "MIT-licensed 3.8B with massive 128K context. Best reasoning model under 1.5GB",
            downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q2_K.gguf",
            architecture = "phi3",
            quantization = "Q2_K",
            contextWindow = 131072,
            maxOutputTokens = 1024,
            promptTemplate = "phi3",
            license = "mit",
            useCase = "reasoning",
            languages = "multilingual"
        ),
        ModelInfo(
            id = "dolphin_2_6_phi_2_q4",
            name = "Dolphin-2.6-Phi-2 Q4",
            params = "2.7B",
            sizeMb = 1771.0,
            minRamGb = 3.5,
            tier = 1,
            description = "Uncensored fine-tune of Phi-2 at Q4. MIT license, strong coding & creative writing",
            downloadUrl = "https://huggingface.co/TheBloke/dolphin-2_6-phi-2-GGUF/resolve/main/dolphin-2_6-phi-2.Q4_K_M.gguf",
            architecture = "phi",
            quantization = "Q4_K_M",
            contextWindow = 4096,
            maxOutputTokens = 512,
            promptTemplate = "chatml",
            license = "mit",
            useCase = "chat",
            languages = "english",
            supportsToolCalling = true
        ),

        // ====================================================================
        // LITERT / TFLITE MODELS — Apache 2.0 or MIT, < 2GB, no license gate
        // ====================================================================

        // ── TIER 3 (< 500 MB) ──────────────────────────────────────────────
        ModelInfo(
            id = "gpt2_8bit",
            name = "GPT-2 (8-bit TFLite)",
            params = "124M",
            sizeMb = 119.0,
            minRamGb = 0.5,
            tier = 3,
            description = "Classic GPT-2 124M in 8-bit quantized TFLite. MIT license, runs anywhere. Note: short 64-token context",
            downloadUrl = "https://s3.amazonaws.com/models.huggingface.co/bert/gpt2-64-8bits.tflite",
            tokenizerUrl = "https://huggingface.co/openai-community/gpt2/resolve/main/tokenizer.json",
            backendType = BackendType.LITERT,
            architecture = "gpt2",
            quantization = "8-bit",
            contextWindow = 64,
            maxOutputTokens = 64,
            promptTemplate = "chatml",
            license = "mit",
            useCase = "general",
            languages = "english"
        ),
        ModelInfo(
            id = "smollm_135m_lrt_q8",
            name = "SmolLM-135M LiteRT Q8",
            params = "135M",
            sizeMb = 166.0,
            minRamGb = 0.5,
            tier = 3,
            description = "SmolLM-135M-Instruct quantized to Q8 TFLite. Apache 2.0, instruct-tuned, ideal for LiteRT runtime",
            downloadUrl = "https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.tflite",
            tokenizerUrl = "https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/tokenizer.json",
            backendType = BackendType.LITERT,
            architecture = "llama",
            quantization = "Q8",
            contextWindow = 2048,
            maxOutputTokens = 256,
            promptTemplate = "chatml",
            license = "apache-2.0",
            useCase = "chat",
            languages = "english"
        ),

        // ── TIER 2 (500 MB – 1.5 GB) ───────────────────────────────────────
        ModelInfo(
            id = "gpt2_fp16",
            name = "GPT-2 (FP16 TFLite)",
            params = "124M",
            sizeMb = 237.0,
            minRamGb = 1.0,
            tier = 2,
            description = "GPT-2 124M FP16 precision TFLite. MIT license. Short 64-token context, useful for experimentation",
            downloadUrl = "https://s3.amazonaws.com/models.huggingface.co/bert/gpt2-64-fp16.tflite",
            tokenizerUrl = "https://huggingface.co/openai-community/gpt2/resolve/main/tokenizer.json",
            backendType = BackendType.LITERT,
            architecture = "gpt2",
            quantization = "FP16",
            contextWindow = 64,
            maxOutputTokens = 64,
            promptTemplate = "chatml",
            license = "mit",
            useCase = "general",
            languages = "english"
        ),
        ModelInfo(
            id = "tinyllama_1_1b_lrt_q8",
            name = "TinyLlama-1.1B LiteRT Q8",
            params = "1.1B",
            sizeMb = 1150.0,
            minRamGb = 2.5,
            tier = 2,
            description = "TinyLlama-1.1B-Chat Q8 via LiteRT. Apache 2.0, best LLM capability under 1.2GB for TFLite runtime",
            downloadUrl = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0/resolve/main/TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.tflite",
            tokenizerUrl = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0/resolve/main/tokenizer.json",
            backendType = BackendType.LITERT,
            architecture = "llama",
            quantization = "Q8",
            contextWindow = 2048,
            maxOutputTokens = 384,
            promptTemplate = "llama2",
            license = "apache-2.0",
            useCase = "chat",
            languages = "english"
        )
    )

    fun getModelById(id: String): ModelInfo? = models.find { it.id == id }

    fun getBackendType(id: String): BackendType {
        return getModelById(id)?.backendType ?: BackendType.LLAMA_CPP
    }

    fun getModelInfo(id: String): ModelInfo? = getModelById(id)
}
