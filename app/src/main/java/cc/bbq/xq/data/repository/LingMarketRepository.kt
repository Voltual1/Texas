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
                val unifiedItems = response.apps.map { it.toUnifiedAppItem() } // 现在使用 LingMarketAppMinimal 的映射
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
                val unifiedItems = response.apps.map { it.toUnifiedAppItem() } // 现在使用 LingMarketAppMinimal 的映射
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
        result.map { appDetail ->
            // 先转换为统一的模型
            val unifiedDetail = appDetail.toUnifiedAppDetail()
            
            // 为灵应用商店获取可用的下载URL
            if (unifiedDetail.store == AppStore.LING_MARKET) {
                // 尝试获取APK文件的直接下载URL
                val downloadResult = getLingMarketDownloadUrl(appDetail.apkKey)
                return@map if (downloadResult.isSuccess) {
                    // 如果成功获取直接下载URL，更新统一模型中的downloadUrl
                    val downloadUrl = downloadResult.getOrNull()
                    unifiedDetail.copy(downloadUrl = downloadUrl)
                } else {
                    // 如果失败，保持原样（仍然包含apkKey，但ViewModel需要特殊处理）
                    unifiedDetail
                }
            } else {
                unifiedDetail
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * 获取灵应用商店文件的直接下载URL
 */
private suspend fun getLingMarketDownloadUrl(fileKey: String): Result<String> {
    return try {
        val result = LingMarketClient.getFileDownloadUrl(fileKey)
        result.map { response ->
            response.url
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

    override suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> {
    return try {
        val result = LingMarketClient.getAppComments(appId, page = page, limit = 20)
        result.map { response ->
            // 直接使用 CommentListResponse，它包含 comments 和 pagination
            val comments = response.comments.map { comment ->
                UnifiedComment(
                    id = comment.id,
                    content = comment.content,
                    sendTime = comment.createdAt.toLongOrNull() ?: 0L,
                    sender = comment.user.toUnifiedUser(),
                    childCount = comment.replyCount,
                    raw = comment
                )
            }
            Pair(comments, response.pagination.pages)
        }
    } catch (e: Exception) {
        Result.failure(Exception("获取评论失败: ${e.message}"))
    }
}

    override suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> {
    return try {
        val result = if (parentCommentId == null) {
            // 发布主评论
            LingMarketClient.postAppComment(appId, content)
        } else {
            // 发布回复评论
            LingMarketClient.postCommentReply(appId, parentCommentId, content)
        }
        
        if (result.isSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("发布评论失败"))
        }
    } catch (e: Exception) {
        Result.failure(Exception("发布评论时出错: ${e.message}"))
    }
}

    override suspend fun deleteComment(commentId: String): Result<Unit> {
        return Result.failure(NotImplementedError("灵应用商店暂不支持删除评论"))
    }
    
    // 保留原有的 deleteComment 方法（仅 commentId）
override suspend fun deleteComment(commentId: String): Result<Unit> {
    return Result.failure(NotImplementedError("灵应用商店删除评论需要appId，请使用 deleteComment(appId: String, commentId: String)"))
}

// 新增：支持 appId 的评论删除方法
override suspend fun deleteComment(appId: String, commentId: String): Result<Unit> {
    return try {
        // 使用 LingMarketClient 的 deleteComment 方法
        val result = LingMarketClient.deleteComment(appId, commentId)
        if (result.isSuccess) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("删除评论失败"))
        }
    } catch (e: Exception) {
        Result.failure(Exception("删除评论时出错: ${e.message}"))
    }
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