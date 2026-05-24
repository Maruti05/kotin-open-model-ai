package com.vedica.labs.ind.app.chat.openmodels.data.local.dao

import androidx.room.*
import com.vedica.labs.ind.app.chat.openmodels.data.local.entity.FileContextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileContextDao {
    @Query("SELECT * FROM file_contexts ORDER BY addedAt DESC")
    fun getAllFiles(): Flow<List<FileContextEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileContextEntity)

    @Delete
    suspend fun deleteFile(file: FileContextEntity)

    @Query("DELETE FROM file_contexts WHERE id = :fileId")
    suspend fun deleteFileById(fileId: String)
}
