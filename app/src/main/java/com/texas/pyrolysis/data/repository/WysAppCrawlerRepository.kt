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
     * 自动探测最大 ID
     * 注意：微思的 ID 现在可能已经到了 10000 以上，建议 high 给大一点
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
            delay(100) 
        }
        maxId
    }

    suspend fun startCrawling(
        startId: Int? = null,
        endId: Int,
        concurrency: Int = 2, // 爬取下载链接较重，建议并发改小一点，避免触发高频校验
        onProgress: (current: Int, total: Int, isSuccess: Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        val actualStart = startId ?: ((crawledAppDao.getMaxCrawledId() ?: 0) + 1)
        if (actualStart > endId) return@withContext
        
        val totalToCrawl = endId - actualStart + 1
        var completed = 0
        val semaphore = Semaphore(concurrency)
        
        supervisorScope {
            for (id in actualStart..endId) {
                launch {
                    semaphore.withPermit {
                        val success = crawlSingleApp(id)
                        synchronized(this@WysAppCrawlerRepository) {
                            completed++
                            onProgress(completed, totalToCrawl, success)
                        }
                    }
                }
            }
        }
    }

    private suspend fun crawlSingleApp(appId: Int): Boolean {
        return try {
            // 1. 获取应用详情
            val detailResult = WysAppMarketClient.getAppInfo(appId)
            val detail = detailResult.getOrNull()
            if (detail == null) {
                println("CAN: [跳过] ID $appId 不存在 (API 返回 null)")
                return false
            }

            // 2. 获取下载源
            val sourcesResult = marketRepository.getAppDownloadSources(
                appId = appId.toString(), 
                versionId = 0
            )
            
            val sources = sourcesResult.getOrNull()
            if (sources == null) {
                println("CAN: [失败] ID $appId (${detail.name}) 获取下载源失败: ${sourcesResult.exceptionOrNull()?.message}")
                return false
            }

            // 3. 构造并存入数据库
            val entity = CrawledAppEntity(
                appId = appId,
                name = detail.name,
                packageName = detail.pack,
                version = detail.version,
                downloadUrlsJson = json.encodeToString(sources)
            )
            
            crawledAppDao.insertApp(entity)
            println("CAN: [成功] 已入库 ID $appId - ${detail.name} (下载源: ${sources.size}个)")
            true
        } catch (e: Exception) {
            println("CAN: [异常] ID $appId 发生崩溃: ${e.message}")
            false
        }
    }

    suspend fun exportToJson(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val allData = crawledAppDao.getAllApps()
            if (allData.isEmpty()) return@withContext Result.failure(Exception("数据库中没有数据！"))

            val jsonString = json.encodeToString(allData)
            val file = File(context.getExternalFilesDir(null), "wys_dump_${System.currentTimeMillis()}.json")
            file.writeText(jsonString)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}