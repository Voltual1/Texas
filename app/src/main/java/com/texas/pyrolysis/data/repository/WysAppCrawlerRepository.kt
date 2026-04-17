package com.texas.pyrolysis.data.repository

import android.content.Context
import com.texas.pyrolysis.WysAppMarketClient
import com.texas.pyrolysis.data.db.CrawledAppDao
import com.texas.pyrolysis.data.db.CrawledAppEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import org.koin.core.annotation.Single

@Single
class WysAppCrawlerRepository(
    private val marketRepository: WysAppMarketRepository,
    private val crawledAppDao: CrawledAppDao,
    private val context: Context
) {
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true 
    }

    /**
     * 使用二分法探测最大可用 App ID
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
            delay(200) // 礼貌爬取，避免被封
        }
        maxId
    }

    /**
     * 开始批量爬取
     */
    suspend fun startCrawling(
        startId: Int? = null,
        endId: Int,
        concurrency: Int = 3,
        onProgress: (current: Int, total: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val actualStart = startId ?: ((crawledAppDao.getMaxCrawledId() ?: 0) + 1)
        if (actualStart > endId) return@withContext
        
        val totalToCrawl = endId - actualStart + 1
        var completed = 0
        val semaphore = Semaphore(concurrency)
        
        // 使用 supervisorScope 确保单个任务失败不会导致整个爬取停止
        supervisorScope {
            for (id in actualStart..endId) {
                launch {
                    semaphore.withPermit {
                        try {
                            crawlSingleApp(id)
                        } catch (e: Exception) {
                            println("CAN: 爬取 ID $id 失败: ${e.message}")
                        } finally {
                            synchronized(this@WysAppCrawlerRepository) {
                                completed++
                                onProgress(completed, totalToCrawl)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun crawlSingleApp(appId: Int) {
        // 1. 获取详情
        val detailResult = WysAppMarketClient.getAppInfo(appId)
        val detail = detailResult.getOrNull() ?: return

        // 2. 获取下载源 (使用 repository 以复用黑名单自动切换逻辑)
        val sourcesResult = marketRepository.getAppDownloadSources(appId.toString(), 0)
        val sources = sourcesResult.getOrNull() ?: emptyList()

        // 3. 存入数据库
        val entity = CrawledAppEntity(
            appId = appId,
            name = detail.name,
            packageName = detail.pack,
            version = detail.version,
            downloadUrlsJson = json.encodeToString(sources)
        )
        crawledAppDao.insertApp(entity)
    }

    /**
     * 导出数据库为 JSON 文件
     */
    suspend fun exportToJson(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val allData = crawledAppDao.getAllApps()
            val jsonString = json.encodeToString(allData)
            
            val fileName = "wys_market_dump_${System.currentTimeMillis()}.json"
            val file = File(context.getExternalFilesDir(null), fileName)
            file.writeText(jsonString)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}