package cc.bbq.xq.data.repository

import cc.bbq.xq.AuthManager
import cc.bbq.xq.BBQApplication
import cc.bbq.xq.KtorClient
import cc.bbq.xq.data.unified.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.flow.first
import cc.bbq.xq.AppStore
import io.ktor.client.call.*
import java.io.File
import org.koin.core.annotation.Single

@Single
class XiaoQuRepository(private val apiClient: KtorClient.ApiService) : IAppStoreRepository {

    private suspend fun getToken(): String =
        AuthManager.getCredentials(BBQApplication.instance).first()?.token ?: ""

    // ==========================================================
    // 用户相关
    // ==========================================================

    override suspend fun getCurrentUserDetail(): Result<UnifiedUserDetail> = try {
        val token = getToken().takeIf { it.isNotEmpty() } ?: throw Exception("未登录")
        apiClient.getUserInfo(token).map { response ->
            if (response.code == 1) {
                val d = response.data
                UnifiedUserDetail(
                    id = d.id, username = d.username, displayName = d.nickname,
                    avatarUrl = d.usertx, hierarchy = d.hierarchy, money = d.money,
                    followersCount = d.followerscount, fansCount = d.fanscount,
                    postCount = d.postcount, likeCount = d.likecount,
                    store = AppStore.XIAOQU_SPACE, raw = d
                )
            } else throw Exception(response.msg)
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun updateUserProfile(params: UpdateUserProfileParams): Result<Unit> = try {
        val token = getToken().takeIf { it.isNotEmpty() } ?: throw Exception("未登录")
        
        // 分别处理昵称和QQ更新
        params.nickname?.let { 
            apiClient.modifyUserInfo(token, it, null).getOrThrow().let { res ->
                if (res.code != 1) throw Exception("昵称修改失败")
            }
        }
        params.qqNumber?.let { 
            apiClient.modifyUserInfo(token, null, it).getOrThrow().let { res ->
                if (res.code != 1) throw Exception("QQ号修改失败")
            }
        }
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun uploadAvatar(imageBytes: ByteArray, filename: String): Result<String> = try {
        val token = getToken().takeIf { it.isNotEmpty() } ?: throw Exception("未登录")
        apiClient.uploadAvatar(1, token, imageBytes, filename).map { 
            if (it.code == 1) "上传成功" else throw Exception(it.msg)
        }
    } catch (e: Exception) { Result.failure(e) }

    // ==========================================================
    // 应用查询
    // ==========================================================

    override suspend fun getCategories(): Result<List<UnifiedCategory>> = Result.success(listOf(
        UnifiedCategory("null_null", "最新分享"),
        UnifiedCategory("45_47", "影音阅读"),
        UnifiedCategory("45_55", "音乐听歌"),
        UnifiedCategory("45_61", "休闲娱乐"),
        UnifiedCategory("45_58", "文件管理"),
        UnifiedCategory("45_59", "图像摄影"),
        UnifiedCategory("45_53", "输入方式"),
        UnifiedCategory("45_54", "生活出行"),
        UnifiedCategory("45_50", "社交通讯"),
        UnifiedCategory("45_56", "上网浏览"),
        UnifiedCategory("45_60", "其他类型"),
        UnifiedCategory("45_62", "跑酷竞技")
    ))

    override suspend fun getApps(categoryId: String?, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> = try {
        val (catId, subCatId) = parseCategory(categoryId)
        apiClient.getAppsList(
            limit = if (userId != null) 12 else 9, page = page, sortOrder = "desc",
            categoryId = catId, subCategoryId = subCatId, userId = userId?.toLongOrNull()
        ).map { res ->
            if (res.code == 1) {
                Pair(res.data.list.map { it.toUnifiedAppItem() }, maxOf(res.data.pagecount, 1))
            } else throw Exception(res.msg)
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun searchApps(query: String, page: Int, userId: String?): Result<Pair<List<UnifiedAppItem>, Int>> = try {
        apiClient.getAppsList(limit = 20, page = page, appName = query, sortOrder = "desc", userId = userId?.toLongOrNull()).map { res ->
            if (res.code == 1) {
                Pair(res.data.list.map { it.toUnifiedAppItem() }, maxOf(res.data.pagecount, 1))
            } else throw Exception(res.msg)
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun getAppDetail(appId: String, versionId: Long): Result<UnifiedAppDetail> = try {
        apiClient.getAppsInformation(getToken(), appId.toLong(), versionId).map { 
            if (it.code == 1) it.data.toUnifiedAppDetail() else throw Exception(it.msg)
        }
    } catch (e: Exception) { Result.failure(e) }

    // ==========================================================
    // 评论与互动
    // ==========================================================

    override suspend fun getAppComments(appId: String, versionId: Long, page: Int): Result<Pair<List<UnifiedComment>, Int>> = try {
        apiClient.getAppsCommentList(appId.toLong(), versionId, 20, page, "desc").map { res ->
            if (res.code == 1) {
                Pair(res.data.list.map { it.toUnifiedComment() }, maxOf(res.data.pagecount, 1))
            } else throw Exception(res.msg)
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun postComment(appId: String, versionId: Long, content: String, parentCommentId: String?, mentionUserId: String?): Result<Unit> = try {
        apiClient.postAppComment(getToken(), content, appId.toLong(), versionId, parentCommentId?.toLongOrNull() ?: 0L, null).map { 
            if (it.code == 1) Unit else throw Exception(it.msg)
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun deleteComment(commentId: String): Result<Unit> = try {
        apiClient.deleteAppComment(getToken(), commentId.toLong()).map { 
            if (it.code == 1) Unit else throw Exception(it.msg)
        }
    } catch (e: Exception) { Result.failure(e) }

    // ==========================================================
    // 应用管理与上传
    // ==========================================================

    override suspend fun deleteApp(appId: String, versionId: Long): Result<Unit> = try {
        val token = getToken().takeIf { it.isNotEmpty() } ?: throw Exception("未登录")
        apiClient.deleteApp(token, appId.toLong(), versionId).map { 
            if (it.code == 1) Unit else throw Exception(it.msg)
        }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun releaseApp(params: UnifiedAppReleaseParams): Result<Unit> = try {
        val token = getToken().takeIf { it.isNotEmpty() } ?: throw Exception("未登录")
        val introImg = params.introImages?.joinToString(",") ?: ""
        val res = if (params.isUpdate) {
            apiClient.updateApp(token, params.appId ?: 0L, params.appName, params.iconUrl, params.sizeInMb.toString(), params.introduce ?: "", introImg, params.apkUrl ?: "", params.explain, params.versionName, params.isPay ?: 0, params.payMoney, params.categoryId ?: 0, params.subCategoryId ?: 0)
        } else {
            apiClient.releaseApp(token, params.appName, params.iconUrl, params.sizeInMb.toString(), params.introduce ?: "", introImg, params.apkUrl ?: "", params.explain, params.versionName, params.isPay ?: 0, params.payMoney, params.categoryId ?: 0, params.subCategoryId ?: 0)
        }
        res.map { if (it.code == 1) Unit else throw Exception(it.msg) }
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun uploadImage(file: File, type: String): Result<String> = uploadBinary(file, "image/*")

    override suspend fun uploadApk(file: File, serviceType: String): Result<String> = when (serviceType) {
        "KEYUN" -> uploadBinary(file, "application/octet-stream")
        "WANYUEYUN" -> uploadToWanyueyun(file)
        else -> Result.failure(Exception("不支持的服务类型"))
    }

    override suspend fun getAppDownloadSources(appId: String, versionId: Long): Result<List<UnifiedDownloadSource>> = 
        getAppDetail(appId, versionId).map { detail ->
            detail.downloadUrl?.let { listOf(UnifiedDownloadSource("默认下载", it, true)) } ?: emptyList()
        }

    // ==========================================================
    // 私有辅助方法
    // ==========================================================

    private fun parseCategory(id: String?): Pair<Int?, Int?> {
        if (id == null || id == "null_null") return null to null
        val parts = id.split("_")
        return parts.getOrNull(0)?.toIntOrNull() to parts.getOrNull(1)?.toIntOrNull()
    }

    private suspend fun uploadBinary(file: File, contentType: String): Result<String> = try {
        val response = KtorClient.uploadHttpClient.submitFormWithBinaryData(url = "api.php", formData = formData {
            append("file", InputProvider { file.inputStream().asInput() }, Headers.build {
                append(HttpHeaders.ContentType, contentType)
                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
            })
        })
        val body: KtorClient.UploadResponse = response.body()
        if (body.code == 0 && !body.downurl.isNullOrBlank()) Result.success(body.downurl) else Result.failure(Exception(body.msg))
    } catch (e: Exception) { Result.failure(e) }

    private suspend fun uploadToWanyueyun(file: File): Result<String> = try {
        val response = KtorClient.wanyueyunUploadHttpClient.submitFormWithBinaryData(url = "upload", formData = formData {
            append("Api", "小趣API")
            append("file", InputProvider { file.inputStream().asInput() }, Headers.build {
                append(HttpHeaders.ContentType, "application/vnd.android.package-archive")
                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
            })
        })
        val body: KtorClient.WanyueyunUploadResponse = response.body()
        if (body.code == 200 && !body.data.isNullOrBlank()) Result.success(body.data) else Result.failure(Exception(body.msg))
    } catch (e: Exception) { Result.failure(e) }

    // toggleFavorite, deleteReview, getMyReviews, getMyComments, deleteComment(appId, id)
    // 均已移除，使用基类默认实现。
}