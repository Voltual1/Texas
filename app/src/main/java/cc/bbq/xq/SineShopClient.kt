//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package cc.bbq.xq

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.io.IOException

object SineShopClient {
    private const val BASE_URL = "https://api.sineshop.xin"
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY = 1000L
    private const val REQUEST_TIMEOUT = 30000L
    private const val CONNECT_TIMEOUT = 30000L
    private const val SOCKET_TIMEOUT = 30000L

    // 基础 User-Agent 前缀
    private const val UA_PREFIX = "Token:"

    val httpClient = HttpClient(OkHttp) {
        initConfig(this)
        defaultRequest {
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
    }

    private fun initConfig(client: HttpClientConfig<OkHttpConfig>) {
        client.defaultRequest {
            url(BASE_URL)
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }

        client.install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            })
        }

        client.install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }

        client.install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT
            connectTimeoutMillis = CONNECT_TIMEOUT
            socketTimeoutMillis = SOCKET_TIMEOUT
        }
    }
    
    // 基础响应模型 - 弦应用商店的统一响应格式
    @kotlinx.serialization.Serializable
    data class BaseResponse<T>(
        val code: Int,
        val msg: String,
        val data: T? = null
    ) {
        val isSuccess: Boolean get() = code == 0
    }
    
    // 为弦应用商店头像上传定义专门的响应模型
    @kotlinx.serialization.Serializable
    data class AvatarUploadResponse(
        val code: Int,
        val msg: String,
        val data: Int? = null // 上传成功时为 1
    ) {
        val isSuccess: Boolean get() = code == 0
    }

    // 新增：用户信息模型
    @Serializable
    data class SineShopUserInfo(
        val id: Int,
        val username: String,
        @SerialName("display_name") val displayName: String,
        @SerialName("user_describe") val userDescribe: String?,
        @SerialName("user_official") val userOfficial: String?,
        @SerialName("user_avatar") val userAvatar: String?,
        @SerialName("user_badge") val userBadge: String?,
        @SerialName("user_status") val userStatus: Int,
        @SerialName("user_status_reason") val userStatusReason: String?,
        @SerialName("ban_time") val banTime: Long,
        @SerialName("join_time") val joinTime: Long,
        @SerialName("user_permission") val userPermission: Int,
        @SerialName("bind_qq") val bindQq: Long?,
        @SerialName("bind_email") val bindEmail: String?,
        @SerialName("bind_bilibili") val bindBilibili: Int?,
        @SerialName("verify_email") val verifyEmail: Int,
        @SerialName("last_login_device") val lastLoginDevice: String?,
        @SerialName("last_online_time") val lastOnlineTime: Long,
        @SerialName("pub_favourite") val pubFavourite: JsonObjectWrapper?,
        @SerialName("upload_count") val uploadCount: Int,
        @SerialName("reply_count") val replyCount: Int
    )

    @Serializable
    data class JsonObjectWrapper(
        @SerialName("Int64") val int64: Long?,
        @SerialName("Valid") val valid: Boolean?
    )

    // 新增：应用分类标签模型
    @Serializable
    data class AppTag(
        val id: Int,
        val name: String,
        val icon: String?
    )
    
    // 新增：评价数据模型
@Serializable
data class SineShopReview(
    val id: Int,
    @SerialName("app_id") val appId: Int,          // 新增: 应用 ID (Int)
    @SerialName("package_name") val packageName: String,
    @SerialName("app_version") val appVersion: String, // 注意：Kotlin 字段名是 appVersion，JSON key 是 app_version
    val rating: Int,
    val content: String,
    @SerialName("upvote_count") val upvoteCount: Int,
    @SerialName("downvote_count") val downvoteCount: Int,
    @SerialName("create_time") val createTime: Long,
    val user: SineShopUserInfoLite,
    @SerialName("user_vote_type") val userVoteType: Int,
    val priority: Int,
    @SerialName("is_counted_in_rating") val isCountedInRating: Boolean
)

    // 为评价列表定义单独的数据模型
    @Serializable
    data class SineShopReviewListData(
        val total: Int,
        val list: List<SineShopReview>
    )

    // 新增：应用数据模型
    @Serializable
    data class SineShopApp(
        val id: Int,
        @SerialName("package_name") val package_name: String,
        @SerialName("app_icon") val app_icon: String,
        @SerialName("app_name") val app_name: String,
        @SerialName("version_code") val version_code: Int,
        @SerialName("version_name") val version_name: String,
        @SerialName("app_type") val app_type: String,
        @SerialName("app_version_type") val app_version_type: String,
        @SerialName("app_abi") val app_abi: Int,
        @SerialName("app_sdk_min") val app_sdk_min: Int,
        @SerialName("version_count") val version_count: Int?  // 改为可空类型
    )

    // 为标签列表定义单独的数据模型，保持与 AppTag 一致
    @Serializable
    data class AppTagListData(
        val total: Int,
        val list: List<AppTag>
    )
    
    // 为应用列表定义单独的数据模型
    @Serializable
    data class AppListData(
        val total: Int,
        val list: List<SineShopApp>
    )

    // 新增：弦应用商店专用用户信息模型
    @Serializable
    data class SineShopUserInfoLite(
        val id: Int,
        val username: String,
        @SerialName("display_name") val displayName: String,
        @SerialName("user_avatar") val userAvatar: String?
    )

    @Serializable
    data class SineShopAppDetail(
        val id: Int,
        @SerialName("package_name") val package_name: String,
        @SerialName("app_name") val app_name: String,
        @SerialName("version_code") val version_code: Int,
        @SerialName("version_name") val version_name: String,
        @SerialName("app_icon") val app_icon: String,
        @SerialName("app_type") val app_type: String,
        @SerialName("app_version_type") val app_version_type: String,
        @SerialName("app_abi") val app_abi: Int,
        @SerialName("app_sdk_min") val app_sdk_min: Int,
        @SerialName("app_sdk_target") val app_sdk_target: Int?, // 新增：目标SDK版本（可为空）
        @SerialName("app_previews") val app_previews: List<String>?,
        @SerialName("app_describe") val app_describe: String?,
        @SerialName("app_update_log") val app_update_log: String?,
        @SerialName("app_developer") val app_developer: String?,
        @SerialName("app_source") val app_source: String?,
        @SerialName("upload_message") val upload_message: String?,
        @SerialName("download_size") val download_size: String?,
        @SerialName("upload_time") val upload_time: Long,
        @SerialName("update_time") val update_time: Long,
        @SerialName("user") val user: SineShopUserInfoLite,
        @SerialName("tags") val tags: List<AppTag>?,
        @SerialName("download_count") val download_count: Int,
        @SerialName("is_favourite") val is_favourite: Int,
        @SerialName("favourite_count") val favourite_count: Int,
        @SerialName("review_count") val review_count: Int,
        // 新增：审核状态和审核原因字段
        @SerialName("audit_status") val audit_status: Int?,
        @SerialName("audit_reason") val audit_reason: String?,
        // 新增：审核员信息
        @SerialName("audit_user") val audit_user: SineShopUserInfoLite?
    )

    @Serializable
    data class SineShopComment(
        val id: Int,
        val content: String,
        @SerialName("send_time") val send_time: Long,
        @SerialName("father_reply_id") val father_reply_id: Int,
        @SerialName("sender") val sender: SineShopUserInfoLite,
        @SerialName("child_count") val child_count: Int,
        @SerialName("father_reply") val father_reply: SineShopComment?,
        // 添加 app_id 字段
        @SerialName("app_id") val app_id: Int
    )

    // 为评论列表定义单独的数据模型
    @Serializable
    data class SineShopCommentListData(
        val total: Int,
        val list: List<SineShopComment>
    )
    
    // 新增：下载源模型
    @Serializable
    data class SineShopDownloadSource(
        val id: Int,
        @SerialName("app_id") val appId: Int,
        val name: String,
        val url: String,
        @SerialName("is_extra") val isExtra: Int
    )

    @Serializable
    data class DownloadSourceResponse(
        val actions: List<String>, // 通常为空
        val code: Int,
        val data: List<SineShopDownloadSource>?,
        val msg: String
    )    

    /**
     * 安全执行请求并处理重试逻辑
     */
    internal suspend inline fun <reified T> safeApiCall(block: suspend () -> HttpResponse): Result<T> {
        var attempts = 0
        while (attempts < MAX_RETRIES) {
            try {
                val response = block()
                if (!response.status.isSuccess()) {
                    throw IOException("Request failed with status: ${response.status}")
                }
                return Result.success(response.body())
            } catch (e: IOException) {
                attempts++
                if (attempts < MAX_RETRIES) delay(RETRY_DELAY)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.failure(IOException("Request failed after $MAX_RETRIES attempts."))
    }

    // --- 基础请求封装（支持传入可选 Token） ---

    internal suspend inline fun <reified T> get(
        url: String,
        parameters: Parameters = Parameters.Empty,
        token: String? = null
    ): Result<T> {
        return safeApiCall {
            httpClient.get(url) {
                parameters.entries().forEach { (key, values) ->
                    values.forEach { value -> parameter(key, value) }
                }
                header(HttpHeaders.UserAgent, if (token != null) "$UA_PREFIX$token" else UA_PREFIX)
            }
        }
    }

    internal suspend inline fun <reified T> postForm(
        url: String,
        parameters: Parameters = Parameters.Empty,
        token: String? = null
    ): Result<T> {
        return safeApiCall {
            httpClient.post(url) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(parameters))
                header(HttpHeaders.UserAgent, if (token != null) "$UA_PREFIX$token" else UA_PREFIX)
            }
        }
    }

    // --- 业务接口方法 ---

    /** 登录接口：不需要 Token */
    suspend fun login(username: String, password: String): Result<String> {
        val parameters = Parameters.build {
            append("username", username)
            append("password", password)
        }
        return postForm<BaseResponse<String>>("/user/login", parameters).map { 
            if (it.code == 0) it.data ?: "" else throw IOException(it.msg)
        }
    }

    /** 获取应用版本：通常不需要 Token */
    suspend fun getAppVersionsByAppId(appid: Int, page: Int = 1): Result<AppListData> {
        val parameters = Parameters.build {
            append("appid", appid.toString())
            append("page", page.toString())
        }
        return get<BaseResponse<AppListData>>("/app/list", parameters).map {
            if (it.code == 0) it.data ?: AppListData(0, emptyList()) else throw IOException(it.msg)
        }
    }

    /** 获取个人信息：该请求通常需要携带 Token */
    suspend fun getUserInfo(token: String? = null): Result<SineShopUserInfo> {
        return get<BaseResponse<SineShopUserInfo>>("/user/info", token = token).map {
            if (it.code == 0) it.data ?: throw IOException("Data is null") else throw IOException(it.msg)
        }
    }

    /** 修改个人信息：该请求通常需要携带 Token */
    suspend fun editUserInfo(displayName: String, describe: String, token: String? = null): Result<Boolean> {
        val parameters = Parameters.build {
            append("displayname", displayName)
            append("describe", describe)
        }
        return postForm<BaseResponse<Boolean>>("/user/edit", parameters, token = token).map {
            if (it.code == 0) it.data ?: false else throw IOException(it.msg)
        }
    }

    /** 获取我的评论：该请求通常需要携带 Token */
    suspend fun getMyComments(page: Int = 1, token: String? = null): Result<SineShopCommentListData> {
        val parameters = Parameters.build { append("page", page.toString()) }
        return get<BaseResponse<SineShopCommentListData>>("/reply/mine", parameters, token = token).map {
            if (it.code == 0) it.data ?: SineShopCommentListData(0, emptyList()) else throw IOException(it.msg)
        }
    }

    /** 上传头像：该请求通常需要携带 Token */
    suspend fun uploadAvatar(imageData: ByteArray, filename: String, token: String? = null): Result<Boolean> {
        return try {
            val response: HttpResponse = httpClient.post("/user/avatar") {
                setBody(MultiPartFormDataContent(formData {
                    append("image", imageData, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                    })
                }))
                header(HttpHeaders.UserAgent, if (token != null) "$UA_PREFIX$token" else UA_PREFIX)
            }
            val uploadResponse: AvatarUploadResponse = response.body()
            Result.success(uploadResponse.code == 0 && uploadResponse.data == 1)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 发送根评论：该请求通常需要携带 Token */
    suspend fun postSineShopAppRootComment(appId: Int, content: String, token: String? = null): Result<Int> {
        val parameters = Parameters.build {
            append("appid", appId.toString())
            append("content", content)
            append("father", "-1")
        }
        return postForm<BaseResponse<Int>>("/reply/send", parameters, token = token).map {
            if (it.code == 0) it.data ?: throw IOException("Data null") else throw IOException(it.msg)
        }
    }

    /** 删除评论：该请求通常需要携带 Token */
    suspend fun deleteSineShopComment(commentId: Int, token: String? = null): Result<Unit> {
        val parameters = Parameters.build { append("id", commentId.toString()) }
        return get<BaseResponse<Unit?>>("/reply/delete", parameters, token = token).map {
            if (it.code != 0) throw IOException(it.msg)
        }
    }

    /** 获取收藏列表：该请求通常需要携带 Token */
    suspend fun getMyFavouriteAppsList(page: Int = 1, token: String? = null): Result<AppListData> {
        val parameters = Parameters.build { append("page", page.toString()) }
        return get<BaseResponse<AppListData>>("/user/favourite", parameters, token = token).map {
            if (it.code == 0) it.data ?: AppListData(0, emptyList()) else throw IOException(it.msg)
        }
    }

    /** 获取上传列表：该请求通常需要携带 Token */
    suspend fun getMyUploadAppsList(page: Int = 1, token: String? = null): Result<AppListData> {
        val parameters = Parameters.build { append("page", page.toString()) }
        return get<BaseResponse<AppListData>>("/user/upload", parameters, token = token).map {
            if (it.code == 0) it.data ?: AppListData(0, emptyList()) else throw IOException(it.msg)
        }
    }

    /** 获取历史记录：该请求通常需要携带 Token */
    suspend fun getMyHistoryAppsList(page: Int = 1, token: String? = null): Result<AppListData> {
        val parameters = Parameters.build { append("page", page.toString()) }
        return get<BaseResponse<AppListData>>("/user/history", parameters, token = token).map {
            if (it.code == 0) it.data ?: AppListData(0, emptyList()) else throw IOException(it.msg)
        }
    }

    /** 获取应用列表：支持可选 token 以获取个性化推荐或状态 */
    suspend fun getAppsList(
        tag: Int? = null, 
        page: Int = 1, 
        keyword: String? = null, 
        userId: Int? = null
    ): Result<AppListData> {
        val parameters = Parameters.build {
            tag?.let { append("tag", it.toString()) }
            keyword?.let { append("keyword", it) }
            userId?.let { append("userid", it.toString()) }
            append("page", page.toString())
        }
        return get<BaseResponse<AppListData>>("/app/list", parameters).map {
            if (it.code == 0) it.data ?: AppListData(0, emptyList()) else throw IOException(it.msg)
        }
    }

    fun close() {
        httpClient.close()
    }
}