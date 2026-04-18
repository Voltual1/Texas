package com.texas.pyrolysis.data.repository

import android.content.Context
import com.texas.pyrolysis.WysAppMarketClient
import com.texas.pyrolysis.data.db.LogRepository
import com.texas.pyrolysis.data.db.LogEntry
import com.texas.pyrolysis.BBQApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.io.File
import org.koin.core.annotation.Single

@Serializable
data class CrawlerMetadata(
    val appId: Int,
    val packageName: String,
    val version: String
)

@Single
class WysAppCrawlerRepository(
    private val marketRepository: WysAppMarketRepository,
    private val logRepository: LogRepository,
    private val context: Context
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val CRAWLER_TYPE = "CRAWLER_WYS_DATA"

    /**
     * 二分法探测最大 ID
     */
    suspend fun findMaxAppId(low: Int = 1, high: Int = 30000): Int = withContext(Dispatchers.IO) {
        var l = low
        var r = high
        var maxId = low
        while (l <= r) {
            val mid = (l + r) / 2
            if (WysAppMarketClient.getAppInfo(mid).isSuccess) {
                maxId = mid
                l = mid + 1
            } else {
                r = mid - 1
            }
            delay(50)
        }
        maxId
    }

    /**
     * 开始爬取并存入 Log 表
     */
    suspend fun startCrawling(
        startId: Int,
        endId: Int,
        onProgress: (current: Int, total: Int, isSuccess: Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        val total = endId - startId + 1
        if (total <= 0) return@withContext
        
        val semaphore = Semaphore(2)
        var completed = 0

        supervisorScope {
            for (id in startId..endId) {
                launch {
                    semaphore.withPermit {
                        val success = try {
                            processAndLog(id)
                        } catch (e: Exception) {
                            println("CAN: ID $id 失败: ${e.message}")
                            false
                        }
                        synchronized(this@WysAppCrawlerRepository) {
                            completed++
                            onProgress(completed, total, success)
                        }
                    }
                }
            }
        }
    }

    private suspend fun processAndLog(appId: Int): Boolean {
        // 1. 获取详情
        val detail = WysAppMarketClient.getAppInfo(appId).getOrNull() ?: return false

        // 2. 获取下载源
        val sources = marketRepository.getAppDownloadSources(appId.toString(), detail.verid.toLong())
            .getOrNull() ?: emptyList()

        // 3. 构造元数据
        val metadata = CrawlerMetadata(appId, detail.pack, detail.version)

        // 4. 直接调用 LogRepository 存入 bot_logs 表
        logRepository.insertLog(
            type = CRAWLER_TYPE,
            requestBody = json.encodeToString(metadata),
            responseBody = json.encodeToString(sources),
            status = detail.name // 状态字段存应用名，方便查阅
        )
        return true
    }

    /**
     * 从 Log 表中筛选出爬虫数据并导出
     */
    suspend fun exportToJson(): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 直接从数据库获取指定类型的日志
            val dao = BBQApplication.instance.database.logDao()
            val crawlerLogs = dao.getLogsByType(CRAWLER_TYPE)
            
            if (crawlerLogs.isEmpty()) {
                return@withContext Result.failure(Exception("日志表中没有找到爬虫数据！"))
            }

            // 将 LogEntry 列表转换为更友好的导出格式
            val exportData = crawlerLogs.map { log ->
                val meta = try { json.decodeFromString<CrawlerMetadata>(log.requestBody) } catch(e: Exception) { null }
                mapOf(
                    "appId" to meta?.appId,
                    "name" to log.status,
                    "packageName" to meta?.packageName,
                    "version" to meta?.version,
                    "downloadSources" to log.responseBody,
                    "crawlTime" to log.formattedTime()
                )
            }

            val file = File(context.getExternalFilesDir(null), "wys_log_export_${System.currentTimeMillis()}.json")
            file.writeText(json.encodeToString(exportData))
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}