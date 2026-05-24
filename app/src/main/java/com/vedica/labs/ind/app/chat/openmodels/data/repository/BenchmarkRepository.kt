package com.vedica.labs.ind.app.chat.openmodels.data.repository

import com.vedica.labs.ind.app.chat.openmodels.data.local.dao.BenchmarkDao
import com.vedica.labs.ind.app.chat.openmodels.data.local.entity.BenchmarkEntity
import com.vedica.labs.ind.app.chat.openmodels.data.model.BenchmarkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BenchmarkRepository @Inject constructor(
    private val benchmarkDao: BenchmarkDao
) {
    fun getRecentBenchmarks(): Flow<List<BenchmarkResult>> =
        benchmarkDao.getRecentBenchmarks().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getLatestBenchmark(): BenchmarkResult? =
        benchmarkDao.getLatestBenchmark()?.toDomain()

    suspend fun saveBenchmark(result: BenchmarkResult) {
        benchmarkDao.insertBenchmark(result.toEntity())
    }

    suspend fun saveBenchmark(
        modelName: String,
        tokensPerSecond: Double,
        promptEvalLatencyMs: Int,
        totalGenerationLatencyMs: Int,
        ramUsedMb: Double
    ): BenchmarkResult {
        val result = BenchmarkResult(
            id = UUID.randomUUID().toString(),
            modelName = modelName,
            timestamp = System.currentTimeMillis(),
            tokensPerSecond = tokensPerSecond,
            promptEvalLatencyMs = promptEvalLatencyMs,
            totalGenerationLatencyMs = totalGenerationLatencyMs,
            ramUsedMb = ramUsedMb
        )
        benchmarkDao.insertBenchmark(result.toEntity())
        return result
    }

    private fun BenchmarkEntity.toDomain() = BenchmarkResult(
        id = id, modelName = modelName, timestamp = timestamp,
        tokensPerSecond = tokensPerSecond,
        promptEvalLatencyMs = promptEvalLatencyMs,
        totalGenerationLatencyMs = totalGenerationLatencyMs,
        ramUsedMb = ramUsedMb
    )

    private fun BenchmarkResult.toEntity() = BenchmarkEntity(
        id = id, modelName = modelName, timestamp = timestamp,
        tokensPerSecond = tokensPerSecond,
        promptEvalLatencyMs = promptEvalLatencyMs,
        totalGenerationLatencyMs = totalGenerationLatencyMs,
        ramUsedMb = ramUsedMb
    )
}
