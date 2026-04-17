package com.texas.pyrolysis.data.repository

import android.content.Context
import com.texas.pyrolysis.WysAppMarketClient
import com.texas.pyrolysis.data.db.CrawledAppDao
import com.texas.pyrolysis.data.db.CrawledAppEntity
import kotlinx.coroutines.*
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
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /**
     * 使用二分法探测最大可用 App ID
     */
    suspend fun findMaxAppId(low: Int = 1, high: Int = 2000): Int {
        var l = low
        var r = high
        var maxId = low

        while (l <= r) {
            val mid = (l + r) / 2
            val result = WysAppMarketClient.getAppInfo(mid)
            if (result.isSuccess) {
                maxId = mid
                l = mid + 1 // 继续往右找更大的
            } else {
                r = mid - 1 // 往左找
            }
            delay(200) // 避免请求过快
        }
        return maxId
    }

    /**
     * 开始批量爬取
     * @param startId 起始ID，如果为null则从数据库最大值+1开始
     * @param endId 结束ID
     * @param concurrency 并发数，建议不要太高，3-5即可
     */
    suspend fun startCrawling(
        startId: Int? = null,
        endId: Int,
        concurrency: Int = 3,
        onProgress: (current: Int, total: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val actualStart = startId ?: ((crawledAppDao.getMaxCrawledId() ?: 0) + 1)
        val totalToCrawl = endId - actualStart + 1
        var completed = 0

        val semaphore = kotlinx.coroutines.sync.Semaphore(concurrency)
        
        val jobs = (actualStart..endId).map { id ->
            async {
                semaphore.withPermit {
                    try {
                        crawlSingleApp(id)
                    } catch (e: Exception) {
                        println("CAN: 爬取 ID $id 失败: ${e.message}")
                    } finally {
                        completed++
                        onProgress(completed, totalToCrawl)
                    }
                }
            }
        }
        jobs.awaitAll()
    }

    private suspend fun crawlSingleApp(appId: Int) {
        // 1. 获取详情
        val detailResult = WysAppMarketClient.getAppInfo(appId)
        val detail = detailResult.getOrNull() ?: return

        // 2. 获取下载源
        // 注意：这里会触发 WysAppMarketRepository 里的机型黑名单重试逻辑
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
            
            val file = File(context.getExternalFilesDir(null), "wys_market_dump_${System.currentTimeMillis()}.json")
            file.writeText(jsonString)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}