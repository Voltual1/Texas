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
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

object LingMarketClient {
    private const val BASE_URL = "https://market.ziling.xin/api/api/v1/" 
    /**
     * 【重要警告】BASE_URL 必须以 '/' 结尾。
     * * 根据 Ktor 的路径解析规则：
     * 1. 这里的 BASE_URL 必须保留末尾斜杠。
     * 2. 下方 API 方法中的 url 路径【绝对不能】以 '/' 开头（必须是 "auth/login" 而不能是 "/auth/login"）。
     * * 逻辑说明：
     * - 如果请求路径以 '/' 开头（如 "/path"），Ktor 会将其视为绝对路径，
     * 导致最终请求变为 "https://domain.com/path"，从而丢失 "/api/api/v1" 部分。
     * - 如果请求路径不以 '/' 开头（如 "path"），Ktor 会将其拼接到 BASE_URL 之后。
     */
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY = 1000L
    private const val REQUEST_TIMEOUT = 30000L
    private const val CONNECT_TIMEOUT = 30000L
    private const val SOCKET_TIMEOUT = 30000L

    // 用户代理信息
    private const val USER_AGENT = "Ktor client"

    // Ktor HttpClient 实例
    val httpClient = HttpClient(OkHttp) {
        initConfig(this)
    }

    private fun initConfig(client: HttpClientConfig<OkHttpConfig>) {
        // 默认请求配置
        client.defaultRequest {
            url(BASE_URL)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.AcceptCharset, "UTF-8")
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Host, "market.ziling.xin")
            header(HttpHeaders.Connection, "Keep-Alive")
            header(HttpHeaders.AcceptEncoding, "gzip")
        }

        // JSON 序列化配置
        client.install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            })
        }

        // 日志配置
        client.install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }

        // 超时配置
        client.install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT
            connectTimeoutMillis = CONNECT_TIMEOUT
            socketTimeoutMillis = SOCKET_TIMEOUT
        }
    }

    // ===== 数据模型 =====

    // 基础响应模型（灵应用商店响应格式）
    @Serializable
    data class LingMarketBaseResponse<T>(
        val code: Int? = null, // 成功时为null，错误时可能有错误码
        @SerialName("message") val msg: String? = null,
        val data: T? = null,
        val token: String? = null,
        val user: LingMarketUser? = null,
        val comment: LingMarketComment? = null,
        val reply: LingMarketReply? = null
    ) {
        val isSuccess: Boolean get() = code == null || code == 200 || code == 201
    }

    // 登录请求
    @Serializable
    data class LoginRequest(
        val username: String,
        val password: String
    )

    // 登录响应
    @Serializable
    data class LoginResponseData(
        val token: String,
        val user: LingMarketUser
    )

    // 用户信息
    @Serializable
    data class LingMarketUser(
        @SerialName("_id") val id: String,
        val username: String,
        val password: String? = null, // 登录响应中可能有加密密码
        val nickname: String,
        val email: String,
        val role: String,
        val status: String,
        @SerialName("createdAt") val createdAt: String,
        @SerialName("__v") val version: Int = 0,
        @SerialName("avatarUrl") val avatarUrl: String? = null
    )

    // 评论数据
    @Serializable
    data class LingMarketComment(
        @SerialName("_id") val id: String,
        val content: String,
        @SerialName("createdAt") val createdAt: String,
        val user: LingMarketUserLite,
        @SerialName("replyCount") val replyCount: Int = 0
    )

    // 回复数据
    @Serializable
    data class LingMarketReply(
        @SerialName("_id") val id: String,
        val content: String,
        @SerialName("createdAt") val createdAt: String,
        val user: LingMarketUserLite,
        @SerialName("replyToUser") val replyToUser: LingMarketUserLite
    )

    // 精简用户信息（用于评论/回复）
    @Serializable
    data class LingMarketUserLite(
        @SerialName("_id") val id: String,
        val username: String,
        val nickname: String,
        @SerialName("avatarUrl") val avatarUrl: String? = null
    )

    // 应用分类
    @Serializable
    data class LingMarketCategory(
        @SerialName("_id") val id: String,
        val name: String,
        @SerialName("displayName") val displayName: String,
        val description: String? = null,
        val icon: String? = null,
        val order: Int,
        @SerialName("isActive") val isActive: Boolean,
        @SerialName("createdAt") val createdAt: String,
        @SerialName("__v") val version: Int = 0
    )

    // 应用信息
    @Serializable
    data class LingMarketApp(
        @SerialName("_id") val id: String,
        val name: String,
        @SerialName("packageName") val packageName: String,
        @SerialName("variantKey") val variantKey: String,
        @SerialName("versionCode") val versionCode: Int,
        @SerialName("versionName") val versionName: String,
        @SerialName("minSdk") val minSdk: Int,
        @SerialName("targetSdk") val targetSdk: Int,
        val architectures: List<String> = emptyList(),
        val category: String,
        val tags: List<String> = emptyList(),
        val description: String,
        val developer: String? = null,
        val source: String? = null,
        @SerialName("supportedDevices") val supportedDevices: List<String> = emptyList(),
        @SerialName("isWearOS") val isWearOS: Boolean = false,
        @SerialName("isApks") val isApks: Boolean = false,
        @SerialName("supportedLanguages") val supportedLanguages: List<String> = emptyList(),
        @SerialName("supportedDensities") val supportedDensities: List<String> = emptyList(),
        @SerialName("iconKey") val iconKey: String,
        @SerialName("apkKey") val apkKey: String,
        @SerialName("screenshotKeys") val screenshotKeys: List<String> = emptyList(),
        @SerialName("downloadLines") val downloadLines: List<String> = emptyList(),
        val size: Long,
        val uploader: LingMarketUploader,
        val status: String,
        @SerialName("auditLog") val auditLog: List<LingMarketAuditLog> = emptyList(),
        val downloads: Int,
        @SerialName("viewCount") val viewCount: Int,
        @SerialName("ratingAvg") val ratingAvg: Float = 0f,
        @SerialName("ratingCount") val ratingCount: Int = 0,
        @SerialName("lastVersionUpdateAt") val lastVersionUpdateAt: String,
        @SerialName("createdAt") val createdAt: String,
        @SerialName("updatedAt") val updatedAt: String,
        @SerialName("__v") val version: Int = 0,
        @SerialName("versions") val versions: List<LingMarketAppVersion>? = null,
        val variants: List<LingMarketVariant>? = null
    )

    // 应用版本
    @Serializable
    data class LingMarketAppVersion(
        @SerialName("_id") val id: String,
        val app: String,
        @SerialName("versionCode") val versionCode: Int,
        @SerialName("versionName") val versionName: String,
        @SerialName("apkKey") val apkKey: String,
        @SerialName("iconKey") val iconKey: String,
        @SerialName("metaId") val metaId: String,
        @SerialName("isApks") val isApks: Boolean,
        @SerialName("supportedLanguages") val supportedLanguages: List<String>,
        @SerialName("supportedDensities") val supportedDensities: List<String>,
        val architectures: List<String>,
        @SerialName("downloadLines") val downloadLines: List<String>,
        @SerialName("splitContributions") val splitContributions: List<String>,
        val size: Long,
        val changelog: String,
        @SerialName("isActive") val isActive: Boolean,
        val downloads: Int,
        @SerialName("createdAt") val createdAt: String,
        @SerialName("updatedAt") val updatedAt: String,
        @SerialName("__v") val version: Int = 0,
        val uploader: String
    )

    // 变体信息
    @Serializable
    data class LingMarketVariant(
        @SerialName("_id") val id: String,
        val name: String,
        @SerialName("packageName") val packageName: String,
        @SerialName("variantKey") val variantKey: String,
        @SerialName("versionCode") val versionCode: Int,
        @SerialName("versionName") val versionName: String,
        @SerialName("iconKey") val iconKey: String,
        val status: String,
        val downloads: Int,
        @SerialName("createdAt") val createdAt: String
    )

    // 上传者信息
    @Serializable
    data class LingMarketUploader(
        @SerialName("_id") val id: String,
        val username: String,
        val nickname: String
    )

    // 审核日志
    @Serializable
    data class LingMarketAuditLog(
        val action: String,
        val reason: String,
        val reviewer: LingMarketUploader,
        val timestamp: String,
        @SerialName("_id") val id: String
    )

    // 分页信息
    @Serializable
    data class LingMarketPagination(
        val page: Int,
        val limit: Int,
        val total: Int,
        val pages: Int
    )

    // 应用列表响应
    @Serializable
    data class LingMarketAppListResponse(
        val apps: List<LingMarketApp>,
        val pagination: LingMarketPagination
    )

    // 评论请求
    @Serializable
    data class CommentRequest(
        val content: String
    )

    // 回复请求
    @Serializable
    data class ReplyRequest(
        val content: String
    )

    // 删除响应
    @Serializable
    data class DeleteResponse(
        val message: String,
        @SerialName("commentId") val commentId: String? = null,
        @SerialName("replyId") val replyId: String? = null
    )

    // ===== API 方法 =====

    /**
     * 安全地执行 Ktor 请求
     */
    @Suppress("RedundantSuspendModifier")
    private suspend inline fun <reified T> safeApiCall(block: suspend () -> HttpResponse): Result<T> {
        var attempts = 0
        while (attempts < MAX_RETRIES) {
            try {
                val response = block()
                if (!response.status.isSuccess()) {
                    throw IOException("Request failed with status: ${response.status}")
                }
                val responseBody: T = try {
                    response.body()
                } catch (e: Exception) {
                    println("LingMarket Failed to deserialize response body: ${e.message}")
                    throw e
                }
                return Result.success(responseBody)
            } catch (e: IOException) {
                attempts++
                if (attempts < MAX_RETRIES) {
                    delay(RETRY_DELAY)
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.failure(IOException("Request failed after $MAX_RETRIES attempts."))
    }

    /**
     * 发起 GET 请求
     */
    private suspend inline fun <reified T> get(
        url: String,
        token: String? = null
    ): Result<T> {
        return safeApiCall {
            httpClient.get(url) {
                token?.let { bearerAuth(it) }
            }
        }
    }

    /**
     * 发起 POST 请求（JSON 格式）
     */
    private suspend inline fun <reified T> postJson(
        url: String,
        body: Any? = null,
        token: String? = null
    ): Result<T> {
        return safeApiCall {
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                body?.let { setBody(it) }
                token?.let { bearerAuth(it) }
            }
        }
    }

    /**
     * 发起 DELETE 请求
     */
    private suspend inline fun <reified T> delete(
        url: String,
        token: String? = null
    ): Result<T> {
        return safeApiCall {
            httpClient.delete(url) {
                token?.let { bearerAuth(it) }
            }
        }
    }

    /**
     * 登录灵应用商店
     */
    suspend fun login(username: String, password: String): Result<LingMarketBaseResponse<LoginResponseData>> {
        val url = "auth/login"
        val requestBody = LoginRequest(username, password)
        
        return postJson(url, requestBody)
    }

    /**
     * 获取应用分类列表
     */
    suspend fun getCategories(includeInactive: Boolean = false): Result<List<LingMarketCategory>> {
        val token = getToken()
        val url = "categories?includeInactive=$includeInactive"
        
        return get(url, token)
    }

    /**
     * 按分类获取应用列表
     */
    suspend fun getAppsByCategory(
        category: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<LingMarketAppListResponse> {
        val token = getToken()
        val url = "apps/category/$category?page=$page&limit=$limit"
        
        return get(url, token)
    }

    /**
     * 获取应用详情
     */
    suspend fun getAppDetail(appId: String): Result<LingMarketApp> {
        val token = getToken()
        val url = "apps/$appId"
        
        return get(url, token)
    }

    /**
     * 搜索应用
     */
    suspend fun searchApps(
        query: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<LingMarketAppListResponse> {
        val token = getToken()
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "apps?page=$page&limit=$limit&search=$encodedQuery"
        
        return get(url, token)
    }

    /**
     * 获取最近更新的应用
     */
    suspend fun getRecentlyUpdatedApps(
        page: Int = 1,
        limit: Int = 10
    ): Result<LingMarketAppListResponse> {
        val token = getToken()
        val url = "apps/recently-updated?page=$page&limit=$limit"
        
        return get(url, token)
    }

    /**
     * 发送应用评论
     */
    suspend fun postAppComment(
        appId: String,
        content: String
    ): Result<LingMarketBaseResponse<LingMarketComment>> {
        val token = getToken()
        val url = "apps/$appId/comments"
        val requestBody = CommentRequest(content)
        
        return postJson(url, requestBody, token)
    }

    /**
     * 发送评论回复
     */
    suspend fun postCommentReply(
        appId: String,
        commentId: String,
        content: String
    ): Result<LingMarketBaseResponse<LingMarketReply>> {
        val token = getToken()
        val url = "apps/$appId/comments/$commentId/replies"
        val requestBody = ReplyRequest(content)
        
        return postJson(url, requestBody, token)
    }

    /**
     * 删除评论（或回复）
     */
    suspend fun deleteComment(
        appId: String,
        commentId: String
    ): Result<DeleteResponse> {
        val token = getToken()
        val url = "apps/$appId/comments/$commentId"
        
        return delete(url, token)
    }

    /**
     * 获取应用评论列表（根据示例，可能需要单独的端点）
     * 注意：从示例中未看到专门的评论列表API，评论可能包含在应用详情中
     */
    suspend fun getAppComments(
        appId: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<LingMarketBaseResponse<List<LingMarketComment>>> {
        val token = getToken()
        // 根据示例，可能需要猜测端点格式
        val url = "apps/$appId/comments?page=$page&limit=$limit"
        
        return get(url, token)
    }

    /**
     * 获取评论回复列表
     */
    suspend fun getCommentReplies(
        appId: String,
        commentId: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<LingMarketBaseResponse<List<LingMarketReply>>> {
        val token = getToken()
        val url = "apps/$appId/comments/$commentId/replies?page=$page&limit=$limit"
        
        return get(url, token)
    }

    // 辅助方法：为请求添加Bearer认证
    private fun HttpRequestBuilder.bearerAuth(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    /**
     * 获取灵应用商店 Token
     */
    private suspend fun getToken(): String? {
        return try {
            // 使用 BBQApplication.instance 获取 Context，然后从 AuthManager 获取 token
            AuthManager.getLingMarketToken(BBQApplication.instance).first()
        } catch (e: Exception) {
            println("Failed to get LingMarket token: ${e.message}")
            null
        }
    }

    /**
     * 关闭 HttpClient
     */
    fun close() {
        httpClient.close()
    }

    // ===== API Service 接口（兼容现有模式） =====
    
    interface LingMarketApiService {
        suspend fun login(username: String, password: String): Result<LingMarketBaseResponse<LoginResponseData>>
        suspend fun getCategories(includeInactive: Boolean): Result<List<LingMarketCategory>>
        suspend fun getAppsByCategory(category: String, page: Int, limit: Int): Result<LingMarketAppListResponse>
        suspend fun getAppDetail(appId: String): Result<LingMarketApp>
        suspend fun searchApps(query: String, page: Int, limit: Int): Result<LingMarketAppListResponse>
        suspend fun getRecentlyUpdatedApps(page: Int, limit: Int): Result<LingMarketAppListResponse>
        suspend fun postAppComment(appId: String, content: String): Result<LingMarketBaseResponse<LingMarketComment>>
        suspend fun postCommentReply(appId: String, commentId: String, content: String): Result<LingMarketBaseResponse<LingMarketReply>>
        suspend fun deleteComment(appId: String, commentId: String): Result<DeleteResponse>
    }

    object LingMarketApiServiceImpl : LingMarketApiService {
        override suspend fun login(username: String, password: String): Result<LingMarketBaseResponse<LoginResponseData>> {
            return this@LingMarketClient.login(username, password)
        }

        override suspend fun getCategories(includeInactive: Boolean): Result<List<LingMarketCategory>> {
            return this@LingMarketClient.getCategories(includeInactive)
        }

        override suspend fun getAppsByCategory(category: String, page: Int, limit: Int): Result<LingMarketAppListResponse> {
            return this@LingMarketClient.getAppsByCategory(category, page, limit)
        }

        override suspend fun getAppDetail(appId: String): Result<LingMarketApp> {
            return this@LingMarketClient.getAppDetail(appId)
        }

        override suspend fun searchApps(query: String, page: Int, limit: Int): Result<LingMarketAppListResponse> {
            return this@LingMarketClient.searchApps(query, page, limit)
        }

        override suspend fun getRecentlyUpdatedApps(page: Int, limit: Int): Result<LingMarketAppListResponse> {
            return this@LingMarketClient.getRecentlyUpdatedApps(page, limit)
        }

        override suspend fun postAppComment(appId: String, content: String): Result<LingMarketBaseResponse<LingMarketComment>> {
            return this@LingMarketClient.postAppComment(appId, content)
        }

        override suspend fun postCommentReply(appId: String, commentId: String, content: String): Result<LingMarketBaseResponse<LingMarketReply>> {
            return this@LingMarketClient.postCommentReply(appId, commentId, content)
        }

        override suspend fun deleteComment(appId: String, commentId: String): Result<DeleteResponse> {
            return this@LingMarketClient.deleteComment(appId, commentId)
        }
    }
}