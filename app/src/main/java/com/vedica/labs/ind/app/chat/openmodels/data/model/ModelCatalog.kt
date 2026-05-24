package com.vedica.labs.ind.app.chat.openmodels.data.model

object ModelCatalog {

    val models: List<Map<String, Any>> = listOf(
        // ULTRA TINY (< 200 MB)
        mapOf("id" to "smollm_135m_q2", "name" to "SmolLM2-135M-Q2", "sizeMb" to 80.0, "params" to "135M", "minRamGb" to 0.5, "tier" to 3, "downloadUrl" to "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q2_K.gguf", "description" to "Ultra-lightweight for sub-1GB devices", "promptTemplate" to "chatml", "contextWindow" to 2048, "maxOutputTokens" to 256),
        mapOf("id" to "smollm_135m_iq3", "name" to "SmolLM2-135M-IQ3", "sizeMb" to 100.0, "params" to "135M", "minRamGb" to 0.5, "tier" to 3, "downloadUrl" to "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-IQ3_M.gguf", "description" to "Best quality ultra-tiny model for entry devices", "promptTemplate" to "chatml", "contextWindow" to 2048, "maxOutputTokens" to 256),
        mapOf("id" to "smollm_135m_q4", "name" to "SmolLM2-135M-Q4", "sizeMb" to 120.0, "params" to "135M", "minRamGb" to 0.5, "tier" to 3, "downloadUrl" to "https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf", "description" to "Maximum quality ultra-tiny for very low-end devices", "promptTemplate" to "chatml", "contextWindow" to 2048, "maxOutputTokens" to 256),
        mapOf("id" to "tinymistral_248m_q4", "name" to "TinyMistral-248M-Q4", "sizeMb" to 180.0, "params" to "248M", "minRamGb" to 1.0, "tier" to 3, "downloadUrl" to "https://huggingface.co/TheBloke/TinyMistral-248M-GGUF/resolve/main/tinymistral-248m.Q4_K_M.gguf", "description" to "Tiny but mighty Mistral architecture", "promptTemplate" to "mistral", "contextWindow" to 2048, "maxOutputTokens" to 256),
        mapOf("id" to "mobilellm_350m_q4", "name" to "MobileLLM-350M-Q4", "sizeMb" to 190.0, "params" to "350M", "minRamGb" to 1.0, "tier" to 3, "downloadUrl" to "https://huggingface.co/TheBloke/MobileLLM-350M-GGUF/resolve/main/mobilellm-350m.Q4_K_M.gguf", "description" to "Optimized for mobile inference with low latency", "promptTemplate" to "llama2", "contextWindow" to 2048, "maxOutputTokens" to 256),

        // TINY (< 500 MB)
        mapOf("id" to "smollm_360m_q2", "name" to "SmolLM2-360M-Q2", "sizeMb" to 200.0, "params" to "360M", "minRamGb" to 1.5, "tier" to 3, "downloadUrl" to "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q2_K.gguf", "description" to "Compact 360M model ideal for 2GB RAM devices", "promptTemplate" to "chatml", "contextWindow" to 2048, "maxOutputTokens" to 384),
        mapOf("id" to "smollm_360m_q3", "name" to "SmolLM2-360M-Q3", "sizeMb" to 260.0, "params" to "360M", "minRamGb" to 1.5, "tier" to 3, "downloadUrl" to "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q3_K_S.gguf", "description" to "Good balance of quality and size for 360M class", "promptTemplate" to "chatml", "contextWindow" to 2048, "maxOutputTokens" to 384),
        mapOf("id" to "smollm_360m_q4", "name" to "SmolLM2-360M-Q4", "sizeMb" to 320.0, "params" to "360M", "minRamGb" to 2.0, "tier" to 3, "downloadUrl" to "https://huggingface.co/bartowski/SmolLM2-360M-Instruct-GGUF/resolve/main/SmolLM2-360M-Instruct-Q4_K_M.gguf", "description" to "Best quality 360M model for devices with 2GB+ RAM", "promptTemplate" to "chatml", "contextWindow" to 2048, "maxOutputTokens" to 384),
        mapOf("id" to "qwen_0_5b_q2", "name" to "Qwen-0.5B-Q2", "sizeMb" to 280.0, "params" to "0.5B", "minRamGb" to 1.5, "tier" to 3, "downloadUrl" to "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q2_k.gguf", "description" to "Qwen's efficient 0.5B architecture in ultra-compact Q2", "promptTemplate" to "chatml", "contextWindow" to 32768, "maxOutputTokens" to 512),
        mapOf("id" to "qwen2_5_0_5b_q2", "name" to "Qwen2.5-0.5B-Q2", "sizeMb" to 290.0, "params" to "0.5B", "minRamGb" to 1.5, "tier" to 3, "downloadUrl" to "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q2_k.gguf", "description" to "Qwen2.5 improved architecture at 0.5B scale", "promptTemplate" to "chatml", "contextWindow" to 32768, "maxOutputTokens" to 512),

        // SMALL (~500MB-1GB)
        mapOf("id" to "tinyllama_1_1b_q2", "name" to "TinyLlama-1.1B-Q2", "sizeMb" to 580.0, "params" to "1.1B", "minRamGb" to 2.0, "tier" to 2, "downloadUrl" to "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q2_K.gguf", "description" to "Compact 1.1B model optimized for Q2 quantization", "promptTemplate" to "llama2", "contextWindow" to 2048, "maxOutputTokens" to 384),
        mapOf("id" to "phi_1_5_q2", "name" to "Phi-1.5-Q2", "sizeMb" to 650.0, "params" to "1.3B", "minRamGb" to 2.0, "tier" to 2, "downloadUrl" to "https://huggingface.co/TheBloke/phi-1_5-GGUF/resolve/main/phi-1_5.Q2_K.gguf", "description" to "Microsoft's 1.3B code-focused Phi model at Q2", "promptTemplate" to "phi", "contextWindow" to 2048, "maxOutputTokens" to 384),
        mapOf("id" to "qwen_0_5b_q4", "name" to "Qwen-0.5B-Q4", "sizeMb" to 550.0, "params" to "0.5B", "minRamGb" to 2.0, "tier" to 2, "downloadUrl" to "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf", "description" to "Full Q4 quality from Qwen's efficient 0.5B model", "promptTemplate" to "chatml", "contextWindow" to 32768, "maxOutputTokens" to 512),
        mapOf("id" to "llama_3_2_1b_q2", "name" to "Llama-3.2-1B-Q2", "sizeMb" to 700.0, "params" to "1B", "minRamGb" to 2.5, "tier" to 2, "downloadUrl" to "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q2_K.gguf", "description" to "Meta's latest 1B instruct model at efficient Q2", "promptTemplate" to "llama2", "contextWindow" to 8192, "maxOutputTokens" to 512),
        mapOf("id" to "qwen2_5_1_5b_q2", "name" to "Qwen2.5-1.5B-Q2", "sizeMb" to 800.0, "params" to "1.5B", "minRamGb" to 2.5, "tier" to 2, "downloadUrl" to "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q2_k.gguf", "description" to "Qwen2.5 1.5B at compact Q2 for 3GB devices", "promptTemplate" to "chatml", "contextWindow" to 32768, "maxOutputTokens" to 512),
        mapOf("id" to "dolphin_3_1b_q2", "name" to "Dolphin-3.0-1B-Q2", "sizeMb" to 820.0, "params" to "1B", "minRamGb" to 2.5, "tier" to 2, "downloadUrl" to "https://huggingface.co/cognitivecomputations/dolphin-3.0-1b-gguf/resolve/main/dolphin-3.0-1b-q2_k.gguf", "description" to "Fine-tuned Llama 3.2 1B with uncensored instruct", "promptTemplate" to "chatml", "contextWindow" to 8192, "maxOutputTokens" to 512),

        // MID-RANGE (1-2GB)
        mapOf("id" to "gemma_2_2b_q2", "name" to "Gemma-2B-Q2", "sizeMb" to 1100.0, "params" to "2B", "minRamGb" to 3.0, "tier" to 2, "downloadUrl" to "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q2_K.gguf", "description" to "Google's 2B Gemma at efficient Q2 for 3GB devices", "promptTemplate" to "gemma", "contextWindow" to 8192, "maxOutputTokens" to 512),
        mapOf("id" to "gemma_2_2b_mlabonne_q2", "name" to "Gemma-2B-mlabonne-Q2", "sizeMb" to 1100.0, "params" to "2B", "minRamGb" to 3.0, "tier" to 2, "downloadUrl" to "https://huggingface.co/mlabonne/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q2_K.gguf", "description" to "Community fine-tuned Gemma 2B with improved reasoning", "promptTemplate" to "gemma", "contextWindow" to 8192, "maxOutputTokens" to 512),
        mapOf("id" to "phi_2_q2", "name" to "Phi-2-Q2", "sizeMb" to 1300.0, "params" to "2.7B", "minRamGb" to 3.5, "tier" to 2, "downloadUrl" to "https://huggingface.co/TheBloke/phi-2-GGUF/resolve/main/phi-2.Q2_K.gguf", "description" to "Microsoft's 2.7B Phi-2 at compact Q2 for 3GB+", "promptTemplate" to "phi", "contextWindow" to 2048, "maxOutputTokens" to 512),
        mapOf("id" to "gemma_2_2b_q4", "name" to "Gemma-2B-Q4", "sizeMb" to 1900.0, "params" to "2B", "minRamGb" to 4.0, "tier" to 1, "downloadUrl" to "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf", "description" to "Google's 2B Gemma at full Q4 quality for 4GB+ devices", "promptTemplate" to "gemma", "contextWindow" to 8192, "maxOutputTokens" to 512),
        mapOf("id" to "llama_3_3b_q4", "name" to "Llama-3.2-3B-Q4", "sizeMb" to 1950.0, "params" to "3B", "minRamGb" to 4.0, "tier" to 1, "downloadUrl" to "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf", "description" to "Meta's latest 3B instruct at Q4 for 4GB devices", "promptTemplate" to "llama2", "contextWindow" to 8192, "maxOutputTokens" to 512),
        mapOf("id" to "phi_3_mini_q4", "name" to "Phi-3-Mini-Q4", "sizeMb" to 2200.0, "params" to "3.8B", "minRamGb" to 4.5, "tier" to 1, "downloadUrl" to "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf", "description" to "Microsoft's 3.8B Mini at Q4 for capable mid-range", "promptTemplate" to "phi", "contextWindow" to 4096, "maxOutputTokens" to 512),

        // HIGH-END
        mapOf("id" to "mistral_7b_q4", "name" to "Mistral-7B-Q4", "sizeMb" to 4100.0, "params" to "7B", "minRamGb" to 8.0, "tier" to 1, "downloadUrl" to "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.3-GGUF/resolve/main/Mistral-7B-Instruct-v0.3.Q4_K_M.gguf", "description" to "Full 7B Mistral Q4 for devices with 8GB+ RAM", "promptTemplate" to "llama2", "contextWindow" to 32768, "maxOutputTokens" to 1024),

        // VISION/MULTIMODAL
        mapOf("id" to "smolvlm2_500m_q8", "name" to "SmolVLM2-500M-Q8", "sizeMb" to 650.0, "params" to "500M", "minRamGb" to 3.0, "tier" to 2, "downloadUrl" to "https://huggingface.co/HuggingFaceTB/SmolVLM2-500M-Instruct-GGUF/resolve/main/SmolVLM2-500M-Instruct-q8.gguf", "description" to "Multimodal VLM with vision understanding (500M params)", "promptTemplate" to "phi", "contextWindow" to 8192, "maxOutputTokens" to 512)
    )

    fun getModelById(id: String): Map<String, Any>? = models.find { it["id"] == id }

    fun getModelInfo(id: String): ModelInfo? {
        val entry = getModelById(id) ?: return null
        return ModelInfo(
            id = entry["id"] as String,
            name = entry["name"] as String,
            params = entry["params"] as String,
            sizeMb = (entry["sizeMb"] as Number).toDouble(),
            minRamGb = (entry["minRamGb"] as Number).toDouble(),
            tier = (entry["tier"] as Number).toInt(),
            description = entry["description"] as String,
            downloadUrl = entry["downloadUrl"] as String,
            promptTemplate = entry["promptTemplate"] as String,
            contextWindow = (entry["contextWindow"] as Number).toInt(),
            maxOutputTokens = (entry["maxOutputTokens"] as Number).toInt()
        )
    }
}
