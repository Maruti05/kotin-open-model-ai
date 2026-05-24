package com.vedica.labs.ind.app.chat.openmodels.data.repository

import com.vedica.labs.ind.app.chat.openmodels.data.local.dao.FileContextDao
import com.vedica.labs.ind.app.chat.openmodels.data.local.entity.FileContextEntity
import com.vedica.labs.ind.app.chat.openmodels.data.local.preferences.AppPreferences
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelDownloadState
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelInfo
import com.vedica.labs.ind.app.chat.openmodels.domain.download.ModelDownloader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    private val preferences: AppPreferences,
    private val fileContextDao: FileContextDao,
    private val modelDownloader: ModelDownloader
) {
    private val _downloads = MutableStateFlow<Map<String, ModelDownloadState>>(emptyMap())
    val downloads: StateFlow<Map<String, ModelDownloadState>> = _downloads

    val downloadedModelIds: Flow<List<String>> = preferences.downloadedModelIds

    suspend fun addToDownloaded(modelId: String) {
        preferences.addDownloadedModelId(modelId)
    }

    suspend fun removeFromDownloaded(modelId: String) {
        preferences.removeDownloadedModelId(modelId)
    }

    suspend fun startDownload(modelId: String, url: String, totalBytes: Long) {
        val outputFile = java.io.File(getModelPath(modelId))
        _downloads.value = _downloads.value + (modelId to ModelDownloadState(
            modelId = modelId, totalBytes = totalBytes
        ))
        try {
            modelDownloader.download(url = url, outputFile = outputFile, totalBytes = totalBytes) { progress ->
                _downloads.value = _downloads.value + (modelId to progress.copy(modelId = modelId))
            }
            _downloads.value = _downloads.value + (modelId to ModelDownloadState(
                modelId = modelId,
                downloadedBytes = totalBytes,
                totalBytes = totalBytes,
                progressPercentage = 100.0,
                status = "COMPLETED"
            ))
            addToDownloaded(modelId)
        } catch (e: Exception) {
            _downloads.value = _downloads.value + (modelId to ModelDownloadState(
                modelId = modelId,
                status = "ERROR",
                error = e.message ?: "Download failed"
            ))
        }
    }

    fun updateDownloadState(modelId: String, state: ModelDownloadState) {
        _downloads.value = _downloads.value + (modelId to state)
    }

    fun removeDownload(modelId: String) {
        _downloads.value = _downloads.value - modelId
    }

    fun getModelPath(modelId: String): String {
        val dir = java.io.File(
            android.os.Environment.getExternalStorageDirectory(),
            "Android/data/com.vedica.labs.ind.app.chat.openmodels/files/OpenModels"
        )
        return java.io.File(dir, "$modelId.gguf").absolutePath
    }

    // File context operations
    fun getAllFileContexts(): Flow<List<com.vedica.labs.ind.app.chat.openmodels.data.model.FileContext>> {
        return fileContextDao.getAllFiles().map { entities ->
            entities.map {
                com.vedica.labs.ind.app.chat.openmodels.data.model.FileContext(
                    id = it.id, filename = it.filename,
                    content = it.content, addedAt = it.addedAt
                )
            }
        }
    }

    suspend fun addFileContext(filename: String, content: String) {
        fileContextDao.insertFile(
            FileContextEntity(
                id = UUID.randomUUID().toString(),
                filename = filename,
                content = content,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteFileContext(id: String) {
        fileContextDao.deleteFileById(id)
    }
}
