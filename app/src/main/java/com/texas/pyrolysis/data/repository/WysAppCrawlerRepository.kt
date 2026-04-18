package com.texas.pyrolysis.data.repository

import android.content.Context
import com.texas.pyrolysis.BBQApplication
import com.texas.pyrolysis.WysAppMarketClient
import com.texas.pyrolysis.data.db.LogEntry
import com.texas.pyrolysis.data.unified.UnifiedDownloadSource
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
    private val context: Context
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val CRAWLER_TYPE = "CRAWLER_WYS_DATA"

    /**
     * 二分法探测最大 ID
     */
    suspend fun findMaxAppId(low: Int = 1, high: Int = 2000): Int = withContext(Dispatchers.IO) {
        var l = low
        var r = high
        var maxId = low
        while (l <= r) {
            val mid = (l + r) / 2
            val result = WysAppMarketClient.getAppInfo(mid)
            if (result.isSuccess) {
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
     * 开始爬取
     */
    suspend fun startCrawling(
        startId: Int,
        endId: Int,
        onProgress: (current: Int, total: Int, isSuccess: Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        val total = endId - startId + 1
        if (total <= 0) return@withContext
        
        val semaphore = Semaphore(4) 
        var completed = 0

        supervisorScope {
            for (id in startId..endId) {
                launch {
                    semaphore.withPermit {
                        val success = try {
                            processAndInsert(id)
                        } catch (e: Exception) {
                            println("CAN: ID $id 抓取过程中崩溃: ${e.message}")
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

    /**
     * 核心逻辑：获取数据并直接通过 Application 单例写入数据库
     */
    private suspend fun processAndInsert(appId: Int): Boolean {
        // 1. 获取详情
        val detailResult = WysAppMarketClient.getAppInfo(appId)
        val detail = detailResult.getOrNull()
        if (detail == null) {
            println("CAN: ID $appId 详情获取失败: ${detailResult.exceptionOrNull()?.message}")
            return false
        }

        // 2. 获取下载源 (使用详情中的 verid)
        val sourcesResult = marketRepository.getAppDownloadSources(appId.toString(), detail.verid.toLong())
        val sources = sourcesResult.getOrNull() ?: emptyList()

        // 3. 构造数据
        val metadata = CrawlerMetadata(appId, detail.pack, detail.version)
        val logEntry = LogEntry(
            type = CRAWLER_TYPE,
            requestBody = json.encodeToString(metadata),
            responseBody = json.encodeToString(sources),
            status = detail.name
        )

        // 4. 【关键】直接使用项目中最稳定的调用方式
        try {
            BBQApplication.instance.database.logDao().insert(logEntry)
            println("CAN: [成功入库] ID $appId - ${detail.name}")
            return true
        } catch (e: Exception) {
            println("CAN: [数据库写入失败] ID $appId: ${e.message}")
            return false
        }
    }

    /**
     * 导出 JSON
     */
    suspend fun exportToJson(): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 直接通过单例获取 DAO 进行查询
            val dao = BBQApplication.instance.database.logDao()
            val crawlerLogs = dao.getLogsByType(CRAWLER_TYPE)
            
            println("CAN: 准备导出，查询到 $CRAWLER_TYPE 类型数据共 ${crawlerLogs.size} 条")

            if (crawlerLogs.isEmpty()) {
                return@withContext Result.failure(Exception("数据库中没有数据！(Type: $CRAWLER_TYPE)"))
            }

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

            val file = File(context.getExternalFilesDir(null), "wys_export_${System.currentTimeMillis()}.json")
            file.writeText(json.encodeToString(exportData))
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}