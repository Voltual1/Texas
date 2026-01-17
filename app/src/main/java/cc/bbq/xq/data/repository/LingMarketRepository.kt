//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.data.repository

import cc.bbq.xq.AppStore
import cc.bbq.xq.LingMarketClient
import cc.bbq.xq.data.unified.*
import java.io.File
import org.koin.core.annotation.Single

@Single
class LingMarketRepository : IAppStoreRepository {

    // 硬编码灵应用商店的分类列表，模仿小趣空间的做法
    override suspend fun getCategories(): Result<List<UnifiedCategory>> {
        val categories = listOf(
    UnifiedCategory(id = "recent", name = "最近更新"),  // 特殊处理：最近更新没有对应的响应项
    UnifiedCategory(id = "browser", name = "浏览器"),
    UnifiedCategory(id = "Games", name = "游戏"),
    UnifiedCategory(id = "tools", name = "实用工具"),
    UnifiedCategory(id = "Apps", name = "应用商店"),
    UnifiedCategory(id = "video", name = "视频播放"),
    UnifiedCategory(id = "teach", name = "教育学习"),
    UnifiedCategory(id = "read", name = "图文阅读"),
    UnifiedCategory(id = "system", name = "系统优化"),
    UnifiedCategory(id = "file", name = "文件管理"),
    UnifiedCategory(id = "watchfaces", name = "表盘（wearOS4+）"),
    UnifiedCategory(id = "watchfacess", name = "表盘（wearOS4-）"),
    UnifiedCategory(id = "pay", name = "数字消费"),
    UnifiedCategory(id = "music", name = "音乐播放"),
    UnifiedCategory(id = "talk", name = "社交通讯"),
    UnifiedCategory(id = "walk", name = "便利出行"),
    UnifiedCategory(id = "tab", name = "输入法"),
    UnifiedCategory(id = "desktop", name = "桌面/启动器"),
    UnifiedCategory(id = "hahaha", name = "整活搞怪"),
    UnifiedCategory(id = "xposed", name = "xposed 模块"),
    UnifiedCategory(id = "Uncategorized", name = "未分类")
)
return Result.success(categories)
    }

    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
        return try {
            val result = when (categoryId) {
                "-1", null -> {
                    // 最近更新
                    LingMarketClient.getRecentlyUpdatedApps(page = page, limit = 20)
                }
                else -> {
                    // 按分类获取
                    LingMarketClient.getAppsByCategory(category = categoryId, page = page, limit = 20)
                }
            }
            
            result.map { response ->
                val unifiedItems = response.apps.map { app ->
                    UnifiedAppItem(
                        uniqueId = "ling_market-${app.id}-${app.versionCode}",
                        navigationId = app.id,
                        navigationVersionId = app.versionCode.toLong(),
                        store = AppStore.LING_MARKET,
                        name = app.name,
                        iconUrl = app.iconKey, // 使用原始 key
                        versionName = app.versionName
                    )
                }
                val totalPages = response.pagination.pages
                Pair(unifiedItems, totalPages)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
        return try {
            val result = LingMarketClient.searchApps(query = query, page = page, limit = 20)
            result.map { response ->
                val unifiedItems = response.apps.map { app ->
                    UnifiedAppItem(
                        uniqueId = "ling_market-${app.id}-${app.versionCode}",
                        navigationId = app.id,
                        navigationVersionId = app.versionCode.toLong(),
                        store = AppStore.LING_MARKET,
                        name = app.name,
                        iconUrl = app.iconKey, // 使用原始 key
                        versionName = app.versionName
                    )
                }
                val totalPages = response.pagination.pages
                Pair(unifiedItems, totalPages)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> {
        return try {
            val result = LingMarketClient.getAppDetail(appId)
            result.map { app ->
                UnifiedAppDetail(
                    id = app.id,
                    store = AppStore.LING_MARKET,
                    packageName = app.packageName,
                    name = app.name,
                    versionCode = app.versionCode.toLong(),
                    versionName = app.versionName,
                    iconUrl = app.iconKey,
                    type = app.category,
                    previews = app.screenshotKeys,
                    description = app.description,
                    updateLog = null,
                    developer = app.developer,
                    size = app.size.toString(),
                    uploadTime = app.createdAt.toLongOrNull() ?: 0L,
                    user = UnifiedUser(
                        id = app.uploader.id,
                        displayName = app.uploader.nickname,
                        avatarUrl = null
                    ),
                    tags = app.tags,
                    downloadCount = app.downloads,
                    isFavorite = false,
                    favoriteCount = 0,
                    reviewCount = app.ratingCount,
                    downloadUrl = app.apkKey,
                    raw = app
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> {
        // 灵应用商店可能不支持评论功能
        return Result.success(Pair(emptyList(), 0))
    }

    override suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> {
        return Result.failure(NotImplementedError("灵应用商店暂不支持评论功能"))
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> {
        return Result.failure(NotImplementedError("灵应用商店暂不支持删除评论"))
    }

    override suspend fun toggleFavorite(appId: String, isCurrentlyFavorite: Boolean): Result<Boolean> {
        return Result.failure(NotImplementedError("灵应用商店不支持收藏功能"))
    }

    override suspend fun deleteApp(appId: String, versionId: Long): Result<Unit> {
        return Result.failure(NotImplementedError("灵应用商店不支持删除应用"))
    }

    override suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> {
        return getAppDetail(appId, versionId).map { detail ->
            if (detail.downloadUrl != null) {
                listOf(UnifiedDownloadSource(
                    name = "默认下载源",
                    url = detail.downloadUrl,
                    isOfficial = true
                ))
            } else {
                emptyList()
            }
        }
    }

    override suspend fun releaseApp(params: UnifiedAppReleaseParams): Result<Unit> {
        return Result.failure(NotImplementedError("灵应用商店暂不支持发布应用"))
    }

    override suspend fun getMyReviews(page: Int): Result<Pair<List<UnifiedComment>, Int>> {
        return Result.failure(NotImplementedError("灵应用商店暂不支持获取我的评价"))
    }

    override suspend fun uploadImage(file: File, type: String): Result<String> {
        return Result.failure(NotImplementedError("灵应用商店暂不支持图片上传"))
    }

    override suspend fun getMyComments(page: Int): Result<Pair<List<UnifiedComment>, Int>> {
        return Result.failure(NotImplementedError("灵应用商店暂不支持获取我的评论"))
    }

    override suspend fun uploadApk(file: File, serviceType: String): Result<String> {
        return Result.failure(NotImplementedError("灵应用商店暂不支持APK上传"))
    }
    
    override suspend fun deleteReview(reviewId: String): Result<Unit> {
        return Result.failure(NotImplementedError("灵应用商店暂不支持删除评价"))
    }
}