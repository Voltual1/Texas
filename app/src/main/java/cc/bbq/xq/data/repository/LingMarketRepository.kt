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
import kotlin.math.ceil
import org.koin.core.annotation.Single

@Single
class LingMarketRepository : IAppStoreRepository {

    override suspend fun getCategories(): Result<List<UnifiedCategory>> {
        return try {
            val result = LingMarketClient.getCategories(includeInactive = false)
            result.map { categories ->
                categories.map { category ->
                    UnifiedCategory(
                        id = category.id,
                        name = category.displayName,
                        icon = category.icon
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
        return try {
            val result = if (categoryId == null || categoryId == "-1") {
                // 最近更新
                LingMarketClient.getRecentlyUpdatedApps(page = page, limit = 20)
            } else {
                // 按分类获取
                LingMarketClient.getAppsByCategory(category = categoryId, page = page, limit = 20)
            }
            
            result.map { response ->
                val unifiedItems = response.apps.map { app ->
                    UnifiedAppItem(
                        uniqueId = "ling_market-${app.id}-${app.versionCode}",
                        navigationId = app.id,
                        navigationVersionId = app.versionCode.toLong(),
                        store = AppStore.LING_MARKET,
                        name = app.name,
                        iconUrl = "${LingMarketClient.BASE_URL}uploads/${app.iconKey}",
                        versionName = app.versionName
                    )
                }
                // 直接使用灵应用商店返回的 pages 字段
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
                        iconUrl = "${LingMarketClient.BASE_URL}uploads/${app.iconKey}",
                        versionName = app.versionName
                    )
                }
                // 直接使用灵应用商店返回的 pages 字段
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
                    iconUrl = "${LingMarketClient.BASE_URL}uploads/${app.iconKey}",
                    type = app.category,
                    previews = app.screenshotKeys.map { "${LingMarketClient.BASE_URL}uploads/$it" },
                    description = app.description,
                    updateLog = null, // 灵应用商店没有单独的更新日志字段
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
                    isFavorite = false, // 灵应用商店没有收藏功能
                    favoriteCount = 0,
                    reviewCount = app.ratingCount,
                    downloadUrl = "${LingMarketClient.BASE_URL}uploads/${app.apkKey}",
                    raw = app
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> {
        // 灵应用商店可能不支持评论功能，根据实际情况返回
        return try {
            val result = LingMarketClient.getAppComments(appId, page = page, limit = 20)
            result.map { response ->
                if (response.isSuccess) {
                    val comments = response.data?.map { comment ->
                        UnifiedComment(
                            id = comment.id,
                            content = comment.content,
                            sendTime = comment.createdAt.toLongOrNull() ?: 0L,
                            sender = UnifiedUser(
                                id = comment.user.id,
                                displayName = comment.user.nickname,
                                avatarUrl = comment.user.avatarUrl
                            ),
                            childCount = comment.replyCount,
                            raw = comment
                        )
                    } ?: emptyList()
                    
                    // 假设评论数量不多，只有一页
                    Pair(comments, 1)
                } else {
                    Pair(emptyList(), 0)
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("灵应用商店暂不支持评论功能"))
        }
    }

    // 以下方法灵应用商店可能不支持或需要单独实现

    override suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> {
        return if (parentCommentId == null) {
            LingMarketClient.postAppComment(appId, content).map { Unit }
        } else {
            LingMarketClient.postCommentReply(appId, parentCommentId, content).map { Unit }
        }
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> {
        // 注意：灵应用商店删除评论需要appId，这里可能无法实现
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