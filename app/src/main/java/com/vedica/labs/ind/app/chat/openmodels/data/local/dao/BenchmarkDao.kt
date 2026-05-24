package com.vedica.labs.ind.app.chat.openmodels.data.local.dao

import androidx.room.*
import com.vedica.labs.ind.app.chat.openmodels.data.local.entity.BenchmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkDao {
    @Query("SELECT * FROM benchmarks ORDER BY timestamp DESC LIMIT 10")
    fun getRecentBenchmarks(): Flow<List<BenchmarkEntity>>

    @Query("SELECT * FROM benchmarks ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBenchmark(): BenchmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmark(benchmark: BenchmarkEntity)
}
