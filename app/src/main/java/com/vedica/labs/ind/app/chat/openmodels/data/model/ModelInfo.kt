package com.vedica.labs.ind.app.chat.openmodels.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val params: String,
    val sizeMb: Double,
    val sizeGb: Double = sizeMb / 1024.0,
    val minRamGb: Double,
    val tier: Int,
    val description: String,
    val downloadUrl: String,
    val format: ModelFormat = ModelFormat.GGUF,
    val architecture: String = "",
    val quantization: String = "",
    val contextWindow: Int = 2048,
    val maxOutputTokens: Int = 512,
    val promptTemplate: String = "chatml",
    val license: String = "",
    val supportsVision: Boolean = false,
    val supportsToolCalling: Boolean = false
)
