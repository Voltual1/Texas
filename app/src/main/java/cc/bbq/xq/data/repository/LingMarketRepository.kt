package cc.bbq.xq.data.repository

import cc.bbq.xq.AppStore
import cc.bbq.xq.LingMarketClient
import cc.bbq.xq.data.unified.*
import java.io.File
import org.koin.core.annotation.Single

@Single
class LingMarketRepository : IAppStoreRepository {

    override suspend fun getCategories(): Result<List<UnifiedCategory>> {
        return try {
            val result = LingMarketClient.getCategories(includeInactive = false)
            result.map { categories ->
                // 添加一个特殊的"最近更新"分类作为第一个选项
                val specialCategories = listOf(
                    UnifiedCategory(id = "-1", name = "最近更新")
                )
                
                // 使用映射函数将服务器分类转换为统一分类
                val serverCategories = categories.map { it.toUnifiedCategory() }
                
                // 合并特殊分类和服务器分类
                specialCategories + serverCategories
            }
        } catch (e: Exception) {
            // 如果API失败，返回硬编码的分类作为fallback
            val categories = listOf(
                UnifiedCategory(id = "-1", name = "最近更新"),
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
            Result.success(categories)
        }
    }

    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
        return try {
            val result = when (categoryId) {
                "-1", null -> {
                    // 最近更新
                    LingMarketClient.getRecentlyUpdatedApps(page = page, limit = 20)
                }
                else -> {
                    // 按分类获取 - 使用 categoryId（即分类的 name 字段）
                    LingMarketClient.getAppsByCategory(category = categoryId, page = page, limit = 20)
                }
            }
            
            result.map { response ->
                val unifiedItems = response.apps.map { it.toUnifiedAppItem() } // 使用映射函数
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
                val unifiedItems = response.apps.map { it.toUnifiedAppItem() } // 使用映射函数
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
            result.map { it.toUnifiedAppDetail() } // 使用映射函数
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> {
        return try {
            val result = LingMarketClient.getAppComments(appId, page = page, limit = 20)
            result.map { response ->
                if (response.isSuccess) {
                    val comments = response.data?.map { comment ->
                        UnifiedComment(
                            id = comment.id,
                            content = comment.content,
                            sendTime = comment.createdAt.toLongOrNull() ?: 0L,
                            sender = comment.user.toUnifiedUser(), // 使用映射函数
                            childCount = comment.replyCount,
                            raw = comment
                        )
                    } ?: emptyList()
                    
                    Pair(comments, 1)
                } else {
                    Pair(emptyList(), 0)
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("灵应用商店暂不支持评论功能"))
        }
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