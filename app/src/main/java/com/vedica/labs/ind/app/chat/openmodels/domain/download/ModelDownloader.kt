package com.vedica.labs.ind.app.chat.openmodels.domain.download

import com.vedica.labs.ind.app.chat.openmodels.data.model.ModelDownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloader @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val activeCalls = ConcurrentHashMap<String, Call>()

    fun cancelDownload(modelId: String) {
        activeCalls[modelId]?.cancel()
    }

    suspend fun download(
        modelId: String,
        url: String,
        outputFile: File,
        totalBytes: Long,
        onProgress: (ModelDownloadState) -> Unit
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val call = client.newCall(request)
        activeCalls[modelId] = call
        try {
            val response = call.execute()

            if (!response.isSuccessful) {
                throw Exception("Server returned HTTP ${response.code}")
            }

            val body = response.body ?: throw Exception("Empty response body")
            val contentLength = body.contentLength()
            val actualTotal = if (contentLength > 0) contentLength else totalBytes
            val inputStream = body.byteStream()
            val buffer = ByteArray(8192)
            var downloadedBytes = 0L
            val startTime = System.currentTimeMillis()

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { output ->
                var bytesRead = inputStream.read(buffer)
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                    val speedMbps = if (elapsedSeconds > 0) {
                        (downloadedBytes.toDouble() * 8 / (1024 * 1024)) / elapsedSeconds
                    } else 0.0

                    onProgress(
                        ModelDownloadState(
                            modelId = modelId,
                            progressPercentage = (downloadedBytes.toDouble() / actualTotal.toDouble() * 100)
                                .let { (it * 10).toInt() / 10.0 },
                            downloadedBytes = downloadedBytes,
                            totalBytes = actualTotal,
                            downloadSpeedMbps = (speedMbps * 10).toInt() / 10.0,
                            status = "DOWNLOADING"
                        )
                    )

                    bytesRead = inputStream.read(buffer)
                }
            }
            inputStream.close()
        } catch (e: IOException) {
            if (call.isCanceled()) return@withContext
            throw e
        } finally {
            activeCalls.remove(modelId)
        }
    }
}
