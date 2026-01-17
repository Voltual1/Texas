package cc.bbq.xq.service.download

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong

class KtorDownloader(
    private val client: HttpClient = defaultClient
) {
    private val _status = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val status = _status.asStateFlow()

    companion object {
        private const val TAG = "KtorDownloader"
        private const val BUFFER_SIZE = 8192

        private val defaultClient by lazy {
            HttpClient(OkHttp) {
                install(HttpTimeout) {
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 60_000
                    requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                }
            }
        }
    }

    suspend fun startDownload(config: DownloadConfig) {
        withContext(Dispatchers.IO) {
            try {
                _status.value = DownloadStatus.Pending
                val file = File(config.savePath, config.fileName).apply { parentFile?.mkdirs() }
                
                // 1. 获取元数据
                val info = fetchFileInfo(config.url)
                val totalSize = info.contentLength
                val canRange = info.acceptRanges && totalSize > 0

                // 2. 检查本地进度
                val currentSize = if (file.exists()) file.length() else 0L

                // 3. 策略路由
                when {
                    totalSize > 0 && currentSize >= totalSize -> {
                        _status.value = DownloadStatus.Success(file)
                    }
                    canRange && config.threadCount > 1 -> {
                        performMultiThreadDownload(config, file, totalSize)
                    }
                    else -> {
                        performSimpleDownload(config.url, file, totalSize, if (canRange) currentSize else 0L)
                    }
                }
            } catch (e: CancellationException) {
                _status.value = DownloadStatus.Idle
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download Failed", e)
                _status.value = DownloadStatus.Error(e.message ?: "Unknown Error", e)
            }
        }
    }

    /**
     * 多线程分块下载核心
     */
    private suspend fun performMultiThreadDownload(config: DownloadConfig, file: File, totalSize: Long) = coroutineScope {
        val downloadedCounter = AtomicLong(0) // 这里建议记录本次下载的增量，或者根据文件现有大小初始化
        val startTime = System.currentTimeMillis()
        
        // 初始化文件大小
        RandomAccessFile(file, "rw").use { it.setLength(totalSize) }

        val chunkSize = totalSize / config.threadCount
        val jobs = (0 until config.threadCount).map { i ->
            val start = i * chunkSize
            val end = if (i == config.threadCount - 1) totalSize - 1 else (i + 1) * chunkSize - 1
            
            async(Dispatchers.IO) {
                downloadSegment(config.url, file, start, end) { bytes ->
                    val current = downloadedCounter.addAndGet(bytes.toLong())
                    // 这里的进度计算需要加上已有的起始大小，此处演示简化版
                    updateProgress(current, totalSize, startTime)
                }
            }
        }
        jobs.awaitAll()
        _status.value = DownloadStatus.Success(file)
    }

    /**
     * 单个分块下载逻辑
     */
    private suspend fun downloadSegment(
        url: String, 
        file: File, 
        start: Long, 
        end: Long, 
        onProgress: (Int) -> Unit
    ) {
        client.get(url) {
            header(HttpHeaders.Range, "bytes=$start-$end")
        }.let { response ->
            if (response.status != HttpStatusCode.PartialContent && !response.status.isSuccess()) {
                throw Exception("Segment fail: ${response.status}")
            }

            val channel = response.bodyAsChannel()
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(start)
                val buffer = ByteArray(BUFFER_SIZE)
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    raf.write(buffer, 0, read)
                    onProgress(read)
                }
            }
        }
    }

    /**
     * 普通单线程下载（含续传）
     */
    private suspend fun performSimpleDownload(url: String, file: File, total: Long, startOffset: Long) {
        val startTime = System.currentTimeMillis()
        client.get(url) {
            if (startOffset > 0) header(HttpHeaders.Range, "bytes=$startOffset-")
        }.let { response ->
            val channel = response.bodyAsChannel()
            val mode = if (startOffset > 0) "rw" else "rwd"
            
            RandomAccessFile(file, mode).use { raf ->
                raf.seek(startOffset)
                var current = startOffset
                val buffer = ByteArray(BUFFER_SIZE)
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    raf.write(buffer, 0, read)
                    current += read
                    updateProgress(current, total, startTime)
                }
            }
        }
        _status.value = DownloadStatus.Success(file)
    }

    private suspend fun fetchFileInfo(url: String): FileMetadata {
        return try {
            val resp = client.head(url)
            FileMetadata(
                contentLength = resp.contentLength() ?: -1L,
                acceptRanges = resp.headers[HttpHeaders.AcceptRanges] == "bytes" || resp.status == HttpStatusCode.PartialContent
            )
        } catch (e: Exception) {
            FileMetadata()
        }
    }

    private fun updateProgress(current: Long, total: Long, startTime: Long) {
        if (total <= 0) return
        val progress = current.toFloat() / total
        val elapsed = (System.currentTimeMillis() - startTime) / 1000f
        val speed = if (elapsed > 0) current / elapsed else 0f
        
        // 简单的频率限制，避免 UI 刷新过快
        val last = _status.value
        if (last !is DownloadStatus.Downloading || (progress - last.progress) > 0.005 || progress >= 1f) {
            _status.value = DownloadStatus.Downloading(progress, current, total, formatSpeed(speed))
        }
    }

    private fun formatSpeed(bytesPerSec: Float): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> "%.2f MB/s".format(bytesPerSec / (1024 * 1024))
            bytesPerSec >= 1024 -> "%.1f KB/s".format(bytesPerSec / 1024)
            else -> "%.0f B/s".format(bytesPerSec)
        }
    }
}

private data class FileMetadata(val contentLength: Long = -1, val acceptRanges: Boolean = false)