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
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException

object WysAppMarketClient {
    private const val BASE_URL = "https://api.wysteam.cn"
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY = 1000L
    private const val REQUEST_TIMEOUT = 30000L
    private const val CONNECT_TIMEOUT = 30000L
    private const val SOCKET_TIMEOUT = 30000L
    internal const val WYSAPPMARKET_ICON_BASE_URL = "https://image.apk.wysteam.cn/"   

    // Ktor HttpClient 实例
    val httpClient = HttpClient(OkHttp) {
        initConfig(this)
    }

    private fun initConfig(client: HttpClientConfig<OkHttpConfig>) {
        // 默认请求配置
        client.defaultRequest {
            url(BASE_URL)
            // 注意：只需指定必要的业务 Header。
        // 避免手动设置 Content-Type 或 Accept 字符串，建议使用 ContentType 对象                
        /* * ⚠️ 警示：不要在这里手动添加 "Accept-Encoding: gzip"。
         * Ktor 的 HttpClient 会根据底层的引擎（如 OkHttp）自动处理压缩。
         * 手动强制指定会导致 Ktor 无法正确拦截响应流进行自动解压，从而拿到乱码。
         */
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }

        // JSON 序列化配置
        client.install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            })
            /*
         * ContentNegotiation 插件会自动处理 "Accept" 和 "Content-Type" Header。
         * 除非有特殊的非标 API 要求，否则不要在其他地方手动覆盖这些值。
         */
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

    // ===== 数据模型定义 =====
    
    // 基础响应模型
    @Serializable
    data class WysApiResponse<T>(
        val code: Int,
        val data: T
    ) {
        val isSuccess: Boolean get() = code == ApiResponseCode.SUCCESS.code
    }
    
    // 应用列表项（用于搜索和列表接口）
    @Serializable
    data class WysAppListItem(
        val id: Int,
        val name: String,
        val pack: String,
        val size: Long,
        val watch: Int,
        val version: String,
        val logo: String,
        val type: Int,
        val verid: Int,
        val info: String
    ) {
        // 辅助属性：获取版本类型显示名称
        val versionTypeDisplay: String
            get() = AppVersionType.fromValue(type).displayName
    }
    
    // 应用详情模型
    @Serializable
    data class WysAppDetail(
        val id: Int,
        val name: String,
        val pack: String,
        val size: Long,
        val watch: Int,
        @SerialName("user") val userId: Int,
        val developer: String,
        @SerialName("devid") val developerId: Int,
        val version: String,
        val logo: String,
        val image: List<String>? = null,
        @SerialName("sys") val osCompatibility: Int,
        @SerialName("display") val displayCompatibility: Int,
        @SerialName("minsdk") val minSdk: Int,
        @SerialName("targetsdk") val targetSdk: Int,
        @SerialName("cpu") val cpuArch: Int,
        val type: Int,
        val keywords: String,
        val access: List<Int>? = null,
        val content: String,
        val verid: Int,
        @SerialName("down") val downloadCount: Int,
        val uptime: String,
        val edittime: String,
        val family: String,
        val uplog: String,
        val upnote: String,
        val link: String?,
        val auditor: Int,
        val username: String,
        val collect: Int,
        val code: Int
    ) {
        // 辅助属性：获取版本类型
        val appVersionType: AppVersionType
            get() = AppVersionType.fromValue(type)
            
        // 辅助属性：获取CPU架构显示名称
        val cpuArchDisplay: String
            get() = CpuArch.fromValue(cpuArch).displayName
            
        // 辅助属性：获取操作系统兼容性显示名称
        val osCompatibilityDisplay: String
            get() = OsCompatibility.fromValue(osCompatibility).displayName
            
        // 辅助属性：获取屏幕兼容性显示名称
        val displayCompatibilityDisplay: String
            get() = DisplayCompatibility.fromValue(displayCompatibility).displayName
            
        // 辅助属性：获取应用分类
        val appFamily: AppFamily
            get() = AppFamily.fromDisplayName(family)
            
        // 辅助属性：获取最低Android版本显示名称
        val minSdkDisplay: String
            get() = AndroidSdkVersion.fromApiLevel(minSdk).displayName
            
        // 辅助属性：获取目标Android版本显示名称
        val targetSdkDisplay: String
            get() = AndroidSdkVersion.fromApiLevel(targetSdk).displayName
    }

    /**
     * 安全地执行 Ktor 请求，并处理异常和重试
     */
    @Suppress("RedundantSuspendModifier")
    internal suspend inline fun <reified T> safeApiCall(block: suspend () -> HttpResponse): Result<T> {
        var attempts = 0
        while (attempts < MAX_RETRIES) {
            try {
                val response = block()
                if (!response.status.isSuccess()) {
                    println("WysAppMarket Request failed with status: ${response.status}")
                    throw IOException("Request failed with status: ${response.status}")
                }
                val responseBody: T = try {
                    response.body()
                } catch (e: Exception) {
                    println("WysAppMarket Failed to deserialize response body: ${e.message}")
                    throw e
                }
                return Result.success(responseBody)
            } catch (e: IOException) {
                attempts++
                println("WysAppMarket Request failed, retrying in $RETRY_DELAY ms... (Attempt $attempts/$MAX_RETRIES)")
                if (attempts < MAX_RETRIES) {
                    delay(RETRY_DELAY)
                }
            } catch (e: Exception) {
                println("WysAppMarket Request failed: ${e.message}")
                return Result.failure(e)
            }
        }
        println("WysAppMarket Request failed after $MAX_RETRIES attempts.")
        return Result.failure(IOException("Request failed after $MAX_RETRIES attempts."))
    }

    /**
     * 发起 GET 请求
     */
    internal suspend inline fun <reified T> get(
        url: String,
        parameters: Parameters = Parameters.Empty
    ): Result<T> {
        return safeApiCall {
            httpClient.get(url) {
                parameters.entries().forEach { (key, values) ->
                    values.forEach { value ->
                        parameter(key, value)
                    }
                }
            }
        }
    }

    /**
     * 关闭 HttpClient（在应用退出时调用）
     */
    fun close() {
        httpClient.close()
    }

    // ===== API 方法 =====
    
    /**
     * 搜索应用
     * @param query 搜索关键词
     * @param searchType 搜索类型，默认为关键词搜索
     */
    suspend fun searchApps(
        query: String,
        searchType: SearchType = SearchType.KEYWORD
    ): Result<List<WysAppListItem>> {
        val url = ApiEndpoint.SEARCH.path
        val parameters = Parameters.build {
            append("type", searchType.value.toString())
            append("key", query)
        }
        
        return get<WysApiResponse<List<WysAppListItem>>>(url, parameters).map { response ->
            if (response.isSuccess) {
                response.data
            } else {
                throw IOException("Search failed with code: ${response.code}")
            }
        }
    }
    
    /**
     * 获取应用列表
     * @param listType 列表类型，默认为最新上架
     */
    suspend fun getAppsList(
        listType: AppListType = AppListType.LATEST
    ): Result<List<WysAppListItem>> {
        val url = ApiEndpoint.APP_LIST.path
        val parameters = Parameters.build {
            append("type", listType.value.toString())
        }
        
        return get<WysApiResponse<List<WysAppListItem>>>(url, parameters).map { response ->
            if (response.isSuccess) {
                response.data
            } else {
                throw IOException("Get app list failed with code: ${response.code}")
            }
        }
    }
    
    /**
     * 获取应用详情
     * @param appId 应用ID
     */
    suspend fun getAppInfo(
        appId: Int
    ): Result<WysAppDetail> {
        val url = ApiEndpoint.APP_INFO.path
        val parameters = Parameters.build {
            append("id", appId.toString())
        }
        
        return get<WysAppDetail>(url, parameters).map { detail ->
            if (detail.code == ApiResponseCode.SUCCESS.code) {
                detail
            } else {
                throw IOException("Get app info failed with code: ${detail.code}")
            }
        }
    }
    
    /**
     * 获取最新上架的应用列表
     */
    suspend fun getLatestApps(): Result<List<WysAppListItem>> {
        return getAppsList(AppListType.LATEST)
    }
    
    /**
     * 获取最多点击的应用列表
     */
    suspend fun getMostViewedApps(): Result<List<WysAppListItem>> {
        return getAppsList(AppListType.MOST_VIEWED)
    }
    
    /**
     * 通过分类搜索应用
     * @param category 分类名称（如"游戏娱乐"）
     */
    suspend fun searchAppsByCategory(category: String): Result<List<WysAppListItem>> {
        return searchApps(category, SearchType.CATEGORY)
    }
    
    /**
     * 扩展函数，便于参数构建
     */
    internal fun wysParameters(block: ParametersBuilder.() -> Unit): Parameters {
        return Parameters.build(block)
    }
}