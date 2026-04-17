package com.texas.pyrolysis.data.repository

import com.texas.pyrolysis.WysAppMarketClient
import com.texas.pyrolysis.WysAppMarketClient.WysAppListItem
import com.texas.pyrolysis.data.DeviceNameDataStore
import com.texas.pyrolysis.data.unified.*
import java.io.File
import kotlin.math.ceil
import kotlinx.coroutines.flow.first 
import org.koin.core.annotation.Single

@Single
class WysAppMarketRepository(
    private val deviceDataStore: DeviceNameDataStore // 1. 构造函数注入
) : IAppStoreRepository {

    private companion object {
        const val PAGE_SIZE = 20
    }
    
    // 提取一个私有辅助方法，用于获取当前机型名
    private suspend fun getCurrentDeviceModel(): String {
        // 获取当前的设备列表
        val deviceList = deviceDataStore.deviceListFlow.first()
        
        // 逻辑：
        // 1. 如果列表不为空，随机抽取一个
        // 2. 抽取后，检查其 model 是否有效
        // 3. 如果列表为空或 model 无效，则返回默认值 "Android"
        return deviceList.shuffled()
            .firstOrNull()
            ?.model
            ?.takeIf { it.isNotBlank() } 
            ?: "Android"
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
    
    override suspend fun getAppVersionsByPackageName(packageName: String): Result<List<UnifiedAppItem>> = try {
    WysAppMarketClient.getAppVersionsByPackage(packageName = packageName).map { apps ->
        // 这里没有 page 参数，微思通常是一次性返回全部，所以直接转 list 即可
        apps.map { it.toUnifiedAppItem() }
    }
} catch (e: Exception) { Result.failure(e) }

    override suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> = try {
        WysAppMarketClient.getAppInfo(appId.toInt()).map { it.toUnifiedAppDetail() }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> = try {
    val appIdInt = appId.toIntOrNull() ?: return Result.failure(IllegalArgumentException("ID错误"))
    
    val currentModel = getCurrentDeviceModel()
    val startKeyResult = WysAppMarketClient.getStartKey(deviceModel = currentModel)
    val startKey = startKeyResult.getOrThrow()

    WysAppMarketClient.getDownloadSources(
        appId = appIdInt,
        startKey = startKey,
        deviceModel = currentModel
    ).map { response ->
        val sources = response.data        
        if (sources.size == 1) {
            val firstSource = sources[0]
            val isSlowIp = firstSource.url.contains("111.229.138.199")
            val isFakeFastName = firstSource.name.contains("极速")
            
            if (isSlowIp && isFakeFastName) {
            deviceDataStore.applyEmergencyRandomModel()
                throw IllegalStateException("检测到由于机型名【$currentModel】被服务器拉入黑名单，服务器故意把备用线路当作极速路线返回给客户端导致限速。已自动更改机型请再试一下。")                
            }
        }
        sources.map { it.toUnifiedDownloadSource() }
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