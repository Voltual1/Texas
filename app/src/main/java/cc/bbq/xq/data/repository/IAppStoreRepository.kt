package cc.bbq.xq.data.repository

import cc.bbq.xq.data.unified.*
import java.io.File

interface IAppStoreRepository {

    // ==========================================================
    // 核心功能：给默认实现返回"不支持"
    // ==========================================================

    suspend fun getCategories(): Result<List<UnifiedCategory>> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取分类"))

    suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取应用列表"))

    suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> =
        Result.failure(UnsupportedOperationException("当前商店不支持搜索应用"))

    suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取应用详情"))
    // ==========================================================
    // 扩展功能：默认“不支持”，子类按需重写
    // ==========================================================

    suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> =
        Result.failure(UnsupportedOperationException("当前商店不支持查看评论"))

    suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> =
        Result.failure(UnsupportedOperationException("当前商店不支持发布评论"))

    suspend fun deleteComment(commentId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("当前商店不支持删除评论"))

    suspend fun deleteComment(appId: String, commentId: String): Result<Unit> =
        deleteComment(commentId) // 默认转发给单参数版本，减少子类重写负担

    suspend fun toggleFavorite(appId: String, isCurrentlyFavorite: Boolean): Result<Boolean> =
        Result.failure(UnsupportedOperationException("当前商店不支持收藏功能"))

    suspend fun deleteApp(appId: String, versionId: Long): Result<Unit> =
        Result.failure(UnsupportedOperationException("当前商店不支持卸载/删除应用"))

    suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取下载源"))

    suspend fun releaseApp(params: UnifiedAppReleaseParams): Result<Unit> =
        Result.failure(UnsupportedOperationException("当前商店不支持发布应用"))

    suspend fun getMyReviews(page: Int): Result<Pair<List<UnifiedComment>, Int>> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取我的评价"))

    suspend fun getMyComments(page: Int): Result<Pair<List<UnifiedComment>, Int>> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取我的评论"))

    suspend fun deleteReview(reviewId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("当前商店不支持删除评价"))

    // ==========================================================
    // 文件与用户相关：默认“不支持”
    // ==========================================================

    suspend fun uploadImage(file: File, type: String): Result<String> =
        Result.failure(UnsupportedOperationException("当前商店不支持上传图片"))

    suspend fun uploadApk(file: File, serviceType: String): Result<String> =
        Result.failure(UnsupportedOperationException("当前商店不支持上传 APK"))

    suspend fun getCurrentUserDetail(): Result<UnifiedUserDetail> =
        Result.failure(UnsupportedOperationException("当前商店不支持获取用户信息"))

    suspend fun updateUserProfile(params: UpdateUserProfileParams): Result<Unit> =
        Result.failure(UnsupportedOperationException("当前商店不支持修改个人资料"))

    suspend fun uploadAvatar(imageBytes: ByteArray, filename: String): Result<String> =
        Result.failure(UnsupportedOperationException("当前商店不支持上传头像"))
    suspend fun getAppVersionsByPackageName(packageName: String): Result<Pair<List<UnifiedAppItem>, Int>> = 
        Result.failure(UnsupportedOperationException("当前商店不支持获取版本列表"))

}