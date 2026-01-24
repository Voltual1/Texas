package cc.bbq.xq.data.repository

import cc.bbq.xq.SineShopClient
import cc.bbq.xq.data.unified.*
import java.io.File
import kotlin.math.ceil
import org.koin.core.annotation.Single

@Single
class SineShopRepository : IAppStoreRepository {

    private fun calculateTotalPages(totalItems: Int, pageSize: Int = 10): Int {
        if (totalItems <= 0) return 1
        return ceil(totalItems.toDouble() / pageSize).toInt()
    }

    // ==========================================================
    // 基础查询功能
    // ==========================================================

    override suspend fun getCategories(): Result<List<UnifiedCategory>> = try {
        SineShopClient.getAppTagList().map { tagList ->
            val specialCategories = listOf(
                UnifiedCategory(id = "-1", name = "最新上传"),
                UnifiedCategory(id = "-2", name = "最多下载"),
                UnifiedCategory(id = "-3", name = "我的上传"),
                UnifiedCategory(id = "-4", name = "我的收藏"),
                UnifiedCategory(id = "-5", name = "历史足迹")
            )
            specialCategories + tagList.map { it.toUnifiedCategory() }
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> = try {
        val result = when {
            userId != null -> SineShopClient.getAppsList(userId = userId.toInt(), page = page)
            categoryId == "-1" -> SineShopClient.getLatestAppsList(page = page)
            categoryId == "-2" -> SineShopClient.getMostDownloadedAppsList(page = page)
            categoryId == "-3" -> SineShopClient.getMyUploadAppsList(page = page)
            categoryId == "-4" -> SineShopClient.getMyFavouriteAppsList(page = page)
            categoryId == "-5" -> SineShopClient.getMyHistoryAppsList(page = page)
            else -> {
                categoryId?.toIntOrNull()?.let { tagId ->
                    SineShopClient.getAppsList(tag = tagId, page = page)
                } ?: Result.success(SineShopClient.AppListData(0, emptyList()))
            }
        }
        result.map { appListData ->
            Pair(appListData.list.map { it.toUnifiedAppItem() }, calculateTotalPages(appListData.total))
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> = try {
        SineShopClient.getAppsList(keyword = query, page = page).map { 
            Pair(it.list.map { item -> item.toUnifiedAppItem() }, calculateTotalPages(it.total))
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> = try {
        SineShopClient.getSineShopAppInfo(appId = appId.toInt()).map { it.toUnifiedAppDetail() }
    } catch (e: Exception) { Result.failure(e) }

    // ==========================================================
    // 评论与反馈功能
    // ==========================================================

    override suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> = try {
        SineShopClient.getSineShopAppComments(appId = appId.toInt(), page = page).map { 
            Pair(it.list.map { c -> c.toUnifiedComment() }, calculateTotalPages(it.total))
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> = try {
        val res = if (parentCommentId == null) {
            SineShopClient.postSineShopAppRootComment(appId = appId.toInt(), content = content)
        } else {
            if (mentionUserId != null) {
                SineShopClient.postSineShopAppReplyCommentWithMention(parentCommentId.toInt(), content, mentionUserId.toInt())
            } else {
                SineShopClient.postSineShopAppReplyComment(parentCommentId.toInt(), content)
            }
        }
        res.map { Unit }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun deleteComment(commentId: String): Result<Unit> = try {
        SineShopClient.deleteSineShopComment(commentId = commentId.toInt())
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getMyComments(page: Int): Result<Pair<List<UnifiedComment>, Int>> = try {
        SineShopClient.getMyComments(page = page).map { 
            Pair(it.list.map { c -> c.toUnifiedComment() }, calculateTotalPages(it.total))
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getMyReviews(page: Int): Result<Pair<List<UnifiedComment>, Int>> = try {
        SineShopClient.getMyReviews(page = page).map { 
            Pair(it.list.map { it.toUnifiedReview() }, calculateTotalPages(it.total))
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun deleteReview(reviewId: String): Result<Unit> = try {
        SineShopClient.deleteSineShopReview(reviewId = reviewId.toInt())
    } catch (e: Exception) { Result.failure(e) }

    // ==========================================================
    // 用户与下载功能
    // ==========================================================

    override suspend fun getCurrentUserDetail(): Result<UnifiedUserDetail> = try {
        SineShopClient.getUserInfo().map { it.toUnifiedUserDetail() }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updateUserProfile(params: UpdateUserProfileParams): Result<Unit> = try {
        if (params.displayName.isNullOrEmpty() && params.description.isNullOrEmpty()) {
            Result.success(Unit)
        } else {
            SineShopClient.editUserInfo(params.displayName ?: "", params.description ?: "").map { 
                if (it) Unit else throw Exception("更新失败")
            }
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun uploadAvatar(imageBytes: ByteArray, filename: String): Result<String> = try {
        SineShopClient.uploadAvatar(imageBytes, filename).map { if (it) "上传成功" else throw Exception("失败") }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> = try {
        SineShopClient.getAppDownloadSources(appId.toInt()).map { sources -> 
            sources.map { it.toUnifiedDownloadSource() }
        }
    } catch (e: Exception) { Result.failure(e) }

    // 仓库特有方法（非接口要求）
    suspend fun getAppVersionsByAppId(appId: Int, page: Int = 1): Result<Pair<List<UnifiedAppItem>, Int>> = try {
        SineShopClient.getAppVersionsByAppId(appid = appId, page = page).map { 
            Pair(it.list.map { item -> item.toUnifiedAppItem() }, calculateTotalPages(it.total))
        }
    } catch (e: Exception) { Result.failure(e) }

    // 注意：toggleFavorite, deleteApp, releaseApp, uploadImage, uploadApk, deleteComment(appId, id)
    // 这些在接口中已有默认“不支持”或“默认转发”实现的方法，全部删除了。
}