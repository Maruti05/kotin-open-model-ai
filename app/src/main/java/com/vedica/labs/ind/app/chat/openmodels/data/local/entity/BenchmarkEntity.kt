package com.vedica.labs.ind.app.chat.openmodels.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "benchmarks")
data class BenchmarkEntity(
    @PrimaryKey val id: String,
    val modelName: String,
    val timestamp: Long,
    val tokensPerSecond: Double,
    val promptEvalLatencyMs: Int,
    val totalGenerationLatencyMs: Int,
    val ramUsedMb: Double
)
