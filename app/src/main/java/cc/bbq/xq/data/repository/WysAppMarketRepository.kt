package cc.bbq.xq.data.repository

import cc.bbq.xq.WysAppMarketClient
import cc.bbq.xq.data.DeviceNameDataStore
import cc.bbq.xq.data.unified.*
import org.koin.core.annotation.Single

/**
 * [DEPRECATED] 微思应用商店数据仓库
 * 本项目已无力维持对该源的持续维护。
 */
@Single
class WysAppMarketRepository(
    private val deviceDataStore: DeviceNameDataStore 
) : IAppStoreRepository {

    private companion object {
        const val MAINTENANCE_NOTICE = "浊燃已停止对微思应用商店提供支持。再见，微思。"
    }

    override suspend fun getCategories(): Result<List<UnifiedCategory>> = 
        Result.failure(IllegalStateException(MAINTENANCE_NOTICE))

    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> = 
        Result.failure(IllegalStateException(MAINTENANCE_NOTICE))

    override suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> = 
        Result.failure(IllegalStateException(MAINTENANCE_NOTICE))
    
    override suspend fun getAppVersionsByPackageName(packageName: String): Result<List<UnifiedAppItem>> = 
        Result.failure(IllegalStateException(MAINTENANCE_NOTICE))

    override suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> = 
        Result.failure(IllegalStateException(MAINTENANCE_NOTICE))

    override suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> = 
        Result.failure(IllegalStateException(MAINTENANCE_NOTICE))
}