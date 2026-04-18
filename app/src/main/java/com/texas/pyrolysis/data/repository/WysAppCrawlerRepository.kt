package com.texas.pyrolysis.data.repository

import android.content.Context
import com.texas.pyrolysis.WysAppMarketClient
import com.texas.pyrolysis.data.db.CrawlerDataDao
import com.texas.pyrolysis.data.db.CrawlerDataEntry
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
    private val crawlerDataDao: CrawlerDataDao, // 注入新 DAO
    private val context: Context
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

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

    suspend fun startCrawling(
        startId: Int? = null,
        endId: Int,
        onProgress: (current: Int, total: Int, isSuccess: Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        val actualStart = startId ?: ((crawlerDataDao.getMaxId() ?: 0) + 1)
        val total = endId - actualStart + 1
        if (total <= 0) return@withContext
        
        val semaphore = Semaphore(2) // 保持低并发，模拟人工
        var completed = 0

        supervisorScope {
            for (id in actualStart..endId) {
                launch {
                    semaphore.withPermit {
                        val success = try {
                            processSingleApp(id)
                        } catch (e: Exception) {
                            println("CAN: ID $id 彻底失败: ${e.message}")
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

    private suspend fun processSingleApp(appId: Int): Boolean {
        // 1. 先拿详情
        val detail = WysAppMarketClient.getAppInfo(appId).getOrNull() ?: return false

        // 2. 尝试拿下载源（如果失败了，我们也存，只是链接为空）
        val sources = marketRepository.getAppDownloadSources(appId.toString(), detail.verid.toLong())
            .getOrNull() ?: emptyList()

        // 3. 仿照 Log 的稳定写入方式
        val entry = CrawlerDataEntry(
            appId = appId,
            name = detail.name,
            packageName = detail.pack,
            version = detail.version,
            downloadUrlsJson = json.encodeToString(sources)
        )
        
        crawlerDataDao.insert(entry)
        return true
    }

    suspend fun exportToJson(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val data = crawlerDataDao.getAll()
            if (data.isEmpty()) throw Exception("数据库依然为空，请检查 Logcat 中的写入日志")
            
            val file = File(context.getExternalFilesDir(null), "wys_export_${System.currentTimeMillis()}.json")
            file.writeText(json.encodeToString(data))
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}