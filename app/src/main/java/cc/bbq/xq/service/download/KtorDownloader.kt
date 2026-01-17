package cc.bbq.xq.service.download

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile

class KtorDownloader {
    private val _status = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val status = _status.asStateFlow()

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
            requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
        }
    }

    companion object {
        private const val TAG = "KtorDownloader"
        private const val BUFFER_SIZE = 8192
    }

    /**
     * 对应 Service 中的 downloader.cancel()
     */
    fun cancel() {
        _status.value = DownloadStatus.Idle
    }

    /**
     * 对应 Service 中的 downloader.close()
     */
    fun close() {
        client.close()
    }

    suspend fun startDownload(config: DownloadConfig) = withContext(Dispatchers.IO) {
        try {
            _status.value = DownloadStatus.Pending
            val file = File(config.savePath, config.fileName).apply { parentFile?.mkdirs() }
            
            // 简化版逻辑：直接按单线程续传处理，如果需要多线程可在此扩展
            val currentSize = if (file.exists()) file.length() else 0L
            performDownload(config.url, file, currentSize)
            
        } catch (e: CancellationException) {
            _status.value = DownloadStatus.Idle
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Download Error", e)
            _status.value = DownloadStatus.Error(e.message ?: "Unknown Error", e)
        }
    }

    private suspend fun performDownload(url: String, file: File, startOffset: Long) {
        client.get(url) {
            if (startOffset > 0) header(HttpHeaders.Range, "bytes=$startOffset-")
        }.let { response ->
            if (!response.status.isSuccess() && response.status != HttpStatusCode.PartialContent) {
                throw Exception("HTTP Error: ${response.status}")
            }

            val channel = response.bodyAsChannel()
            val totalSize = (response.contentLength() ?: 0L) + startOffset
            val startTime = System.currentTimeMillis()

            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(startOffset)
                var current = startOffset
                val buffer = ByteArray(BUFFER_SIZE)
                
                while (!channel.isClosedForRead) {
                    ensureActive() // 关键：响应 serviceScope 的取消
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    
                    raf.write(buffer, 0, read)
                    current += read
                    
                    // 进度更新逻辑
                    updateProgress(current, totalSize, startTime)
                }
            }
            _status.value = DownloadStatus.Success(file)
        }
    }

    private fun updateProgress(current: Long, total: Long, startTime: Long) {
        if (total <= 0) return
        val progress = current.toFloat() / total
        val elapsed = (System.currentTimeMillis() - startTime) / 1000f
        val speed = if (elapsed > 0) current / elapsed else 0f
        
        // 限制 Flow 更新频率，避免 UI 抖动
        if (progress >= 1f || progress - (_status.value as? DownloadStatus.Downloading)?.progress.let { it ?: 0f } > 0.01f) {
            _status.value = DownloadStatus.Downloading(progress, current, total, "%.1f KB/s".format(speed / 1024))
        }
    }
}