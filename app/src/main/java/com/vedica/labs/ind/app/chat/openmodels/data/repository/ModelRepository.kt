package com.vedica.labs.ind.app.chat.openmodels.data.repository

import com.vedica.labs.ind.app.chat.openmodels.data.local.dao.FileContextDao
import com.vedica.labs.ind.app.chat.openmodels.data.local.preferences.AppPreferences
import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelDownloadState
import com.vedica.labs.ind.app.chat.openmodels.domain.download.ModelDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepository @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
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

    private val _cancelledDownloads = mutableSetOf<String>()

    suspend fun startDownload(modelId: String, url: String, totalBytes: Long) {
        val outputFile = java.io.File(getModelPath(modelId))
        _downloads.value += (modelId to ModelDownloadState(
                    modelId = modelId, totalBytes = totalBytes
                ))
        try {
            modelDownloader.download(
                modelId = modelId,
                url = url,
                outputFile = outputFile,
                totalBytes = totalBytes
            ) { progress ->
                if (modelId !in _cancelledDownloads) {
                    _downloads.value += (modelId to progress.copy(modelId = modelId))
                }
            }
            if (modelId in _cancelledDownloads) {
                _cancelledDownloads.remove(modelId)
                return
            }
            _downloads.value += (modelId to ModelDownloadState(
                            modelId = modelId,
                            downloadedBytes = totalBytes,
                            totalBytes = totalBytes,
                            progressPercentage = 100.0,
                            status = "COMPLETED"
                        ))
            addToDownloaded(modelId)
        } catch (e: Exception) {
            if (modelId in _cancelledDownloads) {
                _cancelledDownloads.remove(modelId)
                removeDownload(modelId)
                return
            }
            _downloads.value += (modelId to ModelDownloadState(
                            modelId = modelId,
                            status = "ERROR",
                            error = e.message ?: "Download failed"
                        ))
        }
    }

    fun cancelDownload(modelId: String) {
        _cancelledDownloads.add(modelId)
        modelDownloader.cancelDownload(modelId)
        val file = java.io.File(getModelPath(modelId))
        if (file.exists()) file.delete()
        removeDownload(modelId)
    }

    fun removeDownload(modelId: String) {
        _downloads.value -= modelId
    }

    fun getModelPath(modelId: String): String {
        val dir = context.getExternalFilesDir("OpenModels")!!
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

}
