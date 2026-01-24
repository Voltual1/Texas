package cc.bbq.xq.data.repository

import cc.bbq.xq.AppStore
import cc.bbq.xq.WysAppMarketClient
import cc.bbq.xq.data.unified.*
import java.io.File
import kotlin.math.ceil

class WysAppMarketRepository : IAppStoreRepository {

    // 每页显示的应用数量
    private companion object {
        const val PAGE_SIZE = 20
    }

    override suspend fun getCategories(): Result<List<UnifiedCategory>> {
        return try {
            // 微思应用商店的固定分类
            val categories = listOf(
                UnifiedCategory(id = "-1", name = "最新上架"),
                UnifiedCategory(id = "-2", name = "最多点击"),
                // 添加所有已知分类
                UnifiedCategory(id = "游戏娱乐", name = "游戏娱乐"),
                UnifiedCategory(id = "应用商店", name = "应用商店"),
                UnifiedCategory(id = "工具效率", name = "工具效率"),
                UnifiedCategory(id = "视听娱乐", name = "视听娱乐"),
                UnifiedCategory(id = "社交互动", name = "社交互动"),
                UnifiedCategory(id = "生活服务", name = "生活服务"),
                UnifiedCategory(id = "学习教育", name = "学习教育"),
                UnifiedCategory(id = "系统优化", name = "系统优化"),
                UnifiedCategory(id = "图书阅读", name = "图书阅读"),
                UnifiedCategory(id = "摄影摄像", name = "摄影摄像"),
                UnifiedCategory(id = "旅行交通", name = "旅行交通"),
                UnifiedCategory(id = "金融购物", name = "金融购物"),
                UnifiedCategory(id = "个性主题", name = "个性主题"),
                UnifiedCategory(id = "进阶搞机", name = "进阶搞机"),
                UnifiedCategory(id = "人工智能", name = "人工智能"),
                UnifiedCategory(id = "其它软件", name = "其它软件")
            )
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(Exception("获取分类失败: ${e.message}"))
        }
    }

    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
        return try {
            val result = when (categoryId) {
                "-1" -> WysAppMarketClient.getLatestApps()
                "-2" -> WysAppMarketClient.getMostViewedApps()
                else -> {
                    // 按分类搜索
                    val categoryName = categoryId ?: "游戏娱乐" // 默认分类
                    WysAppMarketClient.searchAppsByCategory(categoryName)
                }
            }
            
            result.map { apps ->
                // 客户端分页：微思API一次性返回所有数据，我们需要在客户端进行分页
                val totalApps = apps.size
                val totalPages = ceil(totalApps.toDouble() / PAGE_SIZE).toInt()
                
                // 计算当前页的数据范围
                val startIndex = (page - 1) * PAGE_SIZE
                val endIndex = minOf(startIndex + PAGE_SIZE, totalApps)
                
                val pagedApps = if (startIndex < totalApps) {
                    apps.subList(startIndex, endIndex).map { it.toUnifiedAppItem() }
                } else {
                    emptyList()
                }
                
                Pair(pagedApps, maxOf(totalPages, 1))
            }
        } catch (e: Exception) {
            Result.failure(Exception("获取应用列表失败: ${e.message}"))
        }
    }

    override suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> {
        return try {
            val result = WysAppMarketClient.searchApps(query)
            
            result.map { apps ->
                // 客户端分页
                val totalApps = apps.size
                val totalPages = ceil(totalApps.toDouble() / PAGE_SIZE).toInt()
                
                // 计算当前页的数据范围
                val startIndex = (page - 1) * PAGE_SIZE
                val endIndex = minOf(startIndex + PAGE_SIZE, totalApps)
                
                val pagedApps = if (startIndex < totalApps) {
                    apps.subList(startIndex, endIndex).map { it.toUnifiedAppItem() }
                } else {
                    emptyList()
                }
                
                Pair(pagedApps, maxOf(totalPages, 1))
            }
        } catch (e: Exception) {
            Result.failure(Exception("搜索应用失败: ${e.message}"))
        }
    }

    override suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> {
        return try {
            val result = WysAppMarketClient.getAppInfo(appId.toInt())
            result.map { appDetail ->
                appDetail.toUnifiedAppDetail()
            }
        } catch (e: Exception) {
            Result.failure(Exception("获取应用详情失败: ${e.message}"))
        }
    }

    override suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> {
        // 微思应用商店不支持评论功能
        return Result.success(Pair(emptyList(), 0))
    }

    override suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> {
        return Result.failure(NotImplementedError("微思应用商店不支持评论功能"))
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> {
        return Result.failure(NotImplementedError("微思应用商店不支持评论功能"))
    }

    override suspend fun deleteComment(appId: String, commentId: String): Result<Unit> {
        return Result.failure(NotImplementedError("微思应用商店不支持评论功能"))
    }

    override suspend fun toggleFavorite(appId: String, isCurrentlyFavorite: Boolean): Result<Boolean> {
        return Result.failure(NotImplementedError("微思应用商店不支持收藏功能"))
    }

    override suspend fun deleteApp(appId: String, versionId: Long): Result<Unit> {
        return Result.failure(NotImplementedError("微思应用商店不支持删除应用"))
    }

    override suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> {
        return getAppDetail(appId, versionId).map { detail ->
            if (detail.downloadUrl != null) {
                listOf(UnifiedDownloadSource(
                    name = "官方下载源",
                    url = detail.downloadUrl,
                    isOfficial = true
                ))
            } else {
                emptyList()
            }
        }
    }

    override suspend fun releaseApp(params: UnifiedAppReleaseParams): Result<Unit> {
        return Result.failure(NotImplementedError("微思应用商店不支持发布应用"))
    }

    override suspend fun getMyReviews(page: Int): Result<Pair<List<UnifiedComment>, Int>> {
        return Result.success(Pair(emptyList(), 0))
    }

    override suspend fun uploadImage(file: File, type: String): Result<String> {
        return Result.failure(NotImplementedError("微思应用商店不支持图片上传"))
    }

    override suspend fun getMyComments(page: Int): Result<Pair<List<UnifiedComment>, Int>> {
        return Result.success(Pair(emptyList(), 0))
    }

    override suspend fun uploadApk(file: File, serviceType: String): Result<String> {
        return Result.failure(NotImplementedError("微思应用商店不支持APK上传"))
    }

    override suspend fun deleteReview(reviewId: String): Result<Unit> {
        return Result.failure(NotImplementedError("微思应用商店不支持评价功能"))
    }

    override suspend fun getCurrentUserDetail(): Result<UnifiedUserDetail> {
        return Result.failure(NotImplementedError("微思应用商店不支持用户功能"))
    }

    override suspend fun updateUserProfile(params: UpdateUserProfileParams): Result<Unit> {
        return Result.failure(NotImplementedError("微思应用商店不支持用户功能"))
    }

    override suspend fun uploadAvatar(imageBytes: ByteArray, filename: String): Result<String> {
        return Result.failure(NotImplementedError("微思应用商店不支持用户功能"))
    }
}