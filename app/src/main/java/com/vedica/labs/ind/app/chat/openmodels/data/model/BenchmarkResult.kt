package com.vedica.labs.ind.app.chat.openmodels.data.model

data class BenchmarkResult(
    val id: String,
    val modelName: String,
    val timestamp: Long,
    val tokensPerSecond: Double,
    val promptEvalLatencyMs: Int,
    val totalGenerationLatencyMs: Int,
    val ramUsedMb: Double
)
