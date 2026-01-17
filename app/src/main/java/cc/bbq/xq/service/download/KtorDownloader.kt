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
import io.ktor.http.contentLength // 修复错误 B：手动确保导入
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import kotlin.coroutines.coroutineContext

class KtorDownloader {
    private val _status = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val status = _status.asStateFlow()

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
            // 修复错误 A：直接使用 Long.MAX_VALUE 或 0 (取决于版本) 
            // 在 Ktor 中，null 通常代表无限
            requestTimeoutMillis = null 
        }
    }

    companion object {
        private const val TAG = "KtorDownloader"
        private const val BUFFER_SIZE = 8192
    }

    fun cancel() {
        _status.value = DownloadStatus.Idle
    }

    fun close() {
        client.close()
    }

    suspend fun startDownload(config: DownloadConfig) = withContext(Dispatchers.IO) {
        try {
            _status.value = DownloadStatus.Pending
            val file = File(config.savePath, config.fileName).apply { 
                parentFile?.mkdirs() 
            }
            
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
        // 使用 try-resource 或 ensureActive 确保协程取消时及时退出
        client.get(url) {
            if (startOffset > 0) header(HttpHeaders.Range, "bytes=$startOffset-")
        }.let { response ->
            if (!response.status.isSuccess() && response.status != HttpStatusCode.PartialContent) {
                throw Exception("HTTP Error: ${response.status}")
            }

            val channel = response.bodyAsChannel()
            // 修复错误 B：确保 response.contentLength() 能被识别
            val contentLength = response.contentLength() ?: 0L
            val totalSize = contentLength + startOffset
            val startTime = System.currentTimeMillis()

            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(startOffset)
                var current = startOffset
                val buffer = ByteArray(BUFFER_SIZE)
                
                while (!channel.isClosedForRead) {
                    // 修复错误 C：使用 coroutineContext.ensureActive()
                    coroutineContext.ensureActive() 
                    
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    
                    raf.write(buffer, 0, read)
                    current += read
                    
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
        // 这里的起始点计算稍微修正一下，使速度显示更准确
        val speed = if (elapsed > 0) (current - (_lastStartSize ?: current)) / elapsed else 0f
        
        val lastStatus = _status.value
        val lastProgress = (lastStatus as? DownloadStatus.Downloading)?.progress ?: 0f

        // 限制 Flow 更新频率：进度变化 > 1% 或完成时更新
        if (progress >= 1f || progress - lastProgress > 0.01f) {
            _status.value = DownloadStatus.Downloading(
                progress = progress,
                currentSize = current,
                totalSize = total,
                speed = formatSpeed(current, startTime)
            )
        }
    }

    private var _lastStartSize: Long? = null // 用于更精确的速度计算

    private fun formatSpeed(current: Long, startTime: Long): String {
        val elapsed = (System.currentTimeMillis() - startTime) / 1000f
        if (elapsed <= 0) return "0 KB/s"
        val speedBytesPerSec = current / elapsed
        return when {
            speedBytesPerSec > 1024 * 1024 -> "%.2f MB/s".format(speedBytesPerSec / (1024 * 1024))
            else -> "%.1f KB/s".format(speedBytesPerSec / 1024)
        }
    }
}