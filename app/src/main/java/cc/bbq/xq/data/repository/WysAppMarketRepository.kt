package cc.bbq.xq.data.repository

import cc.bbq.xq.WysAppMarketClient
import cc.bbq.xq.WysAppMarketClient.WysAppListItem
import cc.bbq.xq.data.unified.*
import java.io.File
import kotlin.math.ceil
import org.koin.core.annotation.Single

@Single
class WysAppMarketRepository : IAppStoreRepository {

    private companion object {
        const val PAGE_SIZE = 20
    }

    // ==========================================================
    // 核心功能：微思商店是一个只读源，仅保留查询逻辑
    // ==========================================================

    override suspend fun getCategories(): Result<List<UnifiedCategory>> = try {
        // 微思应用商店的固定分类
        Result.success(listOf(
            UnifiedCategory("-1", "最新上架"),
            UnifiedCategory("-2", "最多点击"),
            UnifiedCategory("游戏娱乐", "游戏娱乐"),
            UnifiedCategory("应用商店", "应用商店"),
            UnifiedCategory("工具效率", "工具效率"),
            UnifiedCategory("视听娱乐", "视听娱乐"),
            UnifiedCategory("社交互动", "社交互动"),
            UnifiedCategory("生活服务", "生活服务"),
            UnifiedCategory("学习教育", "学习教育"),
            UnifiedCategory("系统优化", "系统优化"),
            UnifiedCategory("图书阅读", "图书阅读"),
            UnifiedCategory("摄影摄像", "摄影摄像"),
            UnifiedCategory("旅行交通", "旅行交通"),
            UnifiedCategory("金融购物", "金融购物"),
            UnifiedCategory("个性主题", "个性主题"),
            UnifiedCategory("进阶搞机", "进阶搞机"),
            UnifiedCategory("人工智能", "人工智能"),
            UnifiedCategory("其它软件", "其它软件")
        ))
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> = try {
        val result = when (categoryId) {
            "-1" -> WysAppMarketClient.getLatestApps()
            "-2" -> WysAppMarketClient.getMostViewedApps()
            else -> WysAppMarketClient.searchAppsByCategory(categoryId ?: "游戏娱乐")
        }
        
        result.map { apps ->
            // 客户端分页：微思API一次性返回所有数据
            processClientPagination(apps, page)
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> = try {
        WysAppMarketClient.searchApps(query).map { apps ->
            processClientPagination(apps, page)
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> = try {
        WysAppMarketClient.getAppInfo(appId.toInt()).map { it.toUnifiedAppDetail() }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> = try {
        // 将字符串类型的 appId 转换为 Int
        val appIdInt = appId.toIntOrNull() ?: return Result.failure(IllegalArgumentException("无效的应用ID: $appId"))
        
        // 调用 WysAppMarketClient 的 getDownloadSources 方法获取真正的下载源
        WysAppMarketClient.getDownloadSources(appIdInt).map { response ->
            // 将每个下载源转换为 UnifiedDownloadSource
            response.data.map { downloadSource ->
                UnifiedDownloadSource(
                    name = downloadSource.name,
                    url = downloadSource.url,
                    // 根据类型判断是否为官方线路，type=0 是官方线路
                    isOfficial = downloadSource.type == 0
                )
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==========================================================
    // 辅助工具：处理微思特有的客户端分页
    // ==========================================================

    private fun processClientPagination(apps: List<WysAppListItem>, page: Int): Pair<List<UnifiedAppItem>, Int> {
        val totalApps = apps.size
        val totalPages = ceil(totalApps.toDouble() / PAGE_SIZE).toInt()
        val startIndex = (page - 1) * PAGE_SIZE
        val endIndex = minOf(startIndex + PAGE_SIZE, totalApps)

        val pagedApps = if (startIndex < totalApps) {
            apps.subList(startIndex, endIndex).map { it.toUnifiedAppItem() }
        } else {
            emptyList()
        }
        return Pair(pagedApps, maxOf(totalPages, 1))
    }

    // 所有的评论、用户中心、发布、上传、删除、收藏等操作
    // 已经全部删除，自动继承接口的"不支持"Result.failure。
}