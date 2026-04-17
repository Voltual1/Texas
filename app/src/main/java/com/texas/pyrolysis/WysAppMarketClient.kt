// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>. 

@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.texas.pyrolysis

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import com.texas.pyrolysis.util.StartupTool
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.net.URLEncoder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.security.MessageDigest
import java.util.*

object WysAppMarketClient {
    private const val BASE_URL = "https://api.wysteam.cn"
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY = 1000L
    private const val REQUEST_TIMEOUT = 30000L
    private const val CONNECT_TIMEOUT = 30000L
    private const val SOCKET_TIMEOUT = 30000L
    internal const val WYSAPPMARKET_ICON_BASE_URL = "https://image.apk.wysteam.cn/"

    private const val DEFAULT_BUILD_NUMBER = "3400"
    private const val USER_AGENT_VALUE = "WysAppMarket/3.0 (Android; WearOS)"
    
    // Ktor HttpClient 实例
    val httpClient = HttpClient(OkHttp) {
        initConfig(this)
    }

    private fun initConfig(client: HttpClientConfig<OkHttpConfig>) {
        // 默认请求配置
        client.defaultRequest {
            url(BASE_URL)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            header(HttpHeaders.UserAgent, USER_AGENT_VALUE)
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

    // ===== 数据模型定义 =====
    
    // 基础响应模型
    @Serializable
    data class WysApiResponse<T>(
        val code: Int,
        val data: T
    ) {
        val isSuccess: Boolean get() = code == ApiResponseCode.SUCCESS.code
    }
    
    // 登录响应模型
@Serializable
data class LoginResponse(
    val code: Int,
    val url: String? = null,
    val token: String? = null
) {
    val isSuccess: Boolean get() = code == 200 && token != null
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
        val info: String? = null
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
        @Serializable(with = SmartListSerializer::class)
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

object SmartListSerializer : KSerializer<List<String>> {
    // 使用内置的 ListSerializer 描述符，这样最标准
    override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        val input = decoder as? JsonDecoder ?: throw Exception("只能在 JSON 格式下使用此解析器")
        val element = input.decodeJsonElement()

        return when (element) {
            // 兼容数组: ["url1", "url2"]
            is JsonArray -> {
                element.map { it.jsonPrimitive.content }
            }
            // 兼容对象: {"1": "url1", "2": "url2"}
            is JsonObject -> {
                element.values.map { it.jsonPrimitive.content }
            }
            // 兜底：如果是 null 或其他类型
            else -> emptyList()
        }
    }

    // 关键修正：确保签名完全匹配 List<String>
    override fun serialize(encoder: Encoder, value: List<String>) {
        // 直接序列化为数组格式，保持数据规范化
        val jsonEncoder = encoder as? JsonEncoder ?: throw Exception("只能在 JSON 格式下序列化")
        val array = JsonArray(value.map { JsonPrimitive(it) })
        jsonEncoder.encodeJsonElement(array)
    }
}
    
    // 下载源响应模型
    @Serializable
    data class DownloadSourceResponse(
        val code: Int,
        val id: String,
        val sha256: String? = null,
        val data: List<DownloadSource>
    ) {
        val isSuccess: Boolean get() = code == ApiResponseCode.SUCCESS.code
    }
    
    // 下载源项目
    @Serializable
    data class DownloadSource(
        @SerialName("t") val type: Int,
        @SerialName("u") val url: String,
        @SerialName("n") val name: String
    )
    
    // StartKey 响应模型
    @Serializable
    data class StartKeyResponse(
        val code: Int,
        @SerialName("startkey") val startKey: String?
    ) {
        val isSuccess: Boolean get() = code == ApiResponseCode.SUCCESS.code && startKey != null
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

    // ===== 工具函数 =====
    
    /**
     * 计算 SHA-256 哈希值
     */
    private fun sha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 获取当前时间戳（秒）
     */
    private fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis() / 1000
    }
    
    /**
     * URL 编码参数（参考Python原型）
     */
    private fun urlEncode(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
    }
    
    /**
     * 构建签名（参考Python原型）
     */
    private fun buildSignature(timestamp: Long, vararg parts: String): String {
        val raw = parts.joinToString("")
        return sha256(raw)
    }

    // ===== 下载相关 API 方法 =====
    
    /**
     * 获取 StartKey
     * @param deviceModel 设备型号
     * @param buildNumber 构建号
     * 核心逻辑已切换至 NativeLib 实现，确保签名 100% 兼容
     */     
    suspend fun getStartKey(
        deviceModel: String,
        buildNumber: String = DEFAULT_BUILD_NUMBER
    ): Result<String> {
        // 直接调用我们之前写的 StartupTool
        // 它内部封装了时间戳获取、补零、以及 NativeLib.getStartupUrlNative 的调用
        val url = com.qx.wysappmarket.presentation.NativeLib.getStartupUrlNative(
            "$BASE_URL/market/start/",
            buildNumber,
            deviceModel,
            (System.currentTimeMillis() / 1000).toString().padStart(10, '0')
        )

        if (url.isNullOrBlank()) {
            return Result.failure(IOException("Native 层生成 URL 失败"))
        }

        println("Native 生成的完整启动 URL: $url")

        return safeApiCall<StartKeyResponse> {
            httpClient.get(url) // 直接请求 Native 返回的完整 URL
        }.map { response ->
            if (response.isSuccess) {
                response.startKey ?: throw IOException("StartKey 为空")
            } else {
                throw IOException("获取 StartKey 失败，code: ${response.code}")
            }
        }
    }
    
    /**
     * 获取应用下载链接（参考Python原型的 get_download_links 方法）
     * @param appId 应用ID
     * @param startKey StartKey，如果为空则自动获取
     * @param deviceModel 设备型号，用于自动获取StartKey时使用
     */
    suspend fun getDownloadSources(
        appId: Int,
        startKey: String? = null,
        deviceModel: String // 改为强制要求，由调用方保证传入
    ): Result<DownloadSourceResponse> {
        // 获取或使用提供的StartKey
        val finalStartKey = if (startKey != null) {
            startKey
        } else {
            // 自动获取StartKey
            val startKeyResult = getStartKey(deviceModel)
            if (startKeyResult.isSuccess) {
                startKeyResult.getOrNull() ?: return Result.failure(
                    IOException("无法获取StartKey")
                )
            } else {
                return Result.failure(
                    startKeyResult.exceptionOrNull() ?: IOException("获取StartKey失败")
                )
            }
        }
        
        val timestamp = getCurrentTimestamp()
        
        // 构建签名：WYS + timestamp + APP0STORE + appId + " KEY=" + startKey
        val signature = buildSignature(
            timestamp,
            "WYS",
            timestamp.toString(),
            "APP0STORE",
            appId.toString(),
            " KEY=",
            finalStartKey
        )
        
        // 构建URL
        val encodedStartKey = urlEncode(finalStartKey)
        val url = "$BASE_URL/market/app/down/" +
                  "?|=$signature" +
                  "&id=$appId" +
                  "&token=0" +
                  "&market=$encodedStartKey" +
                  "&_=$timestamp"
        
        println("获取下载源 URL: $url")
        
        return safeApiCall<DownloadSourceResponse> {
            httpClient.get(url)             
        }.map { response ->
            if (response.isSuccess) {
                response
            } else {
                throw IOException("获取下载源失败，code: ${response.code}")
            }
        }
    }
    
    /**
 * 发起登录请求
 * @param startKey 之前获取到的 startKey
 * @return 返回 LoginResponse，包含授权 URL 和 Token
 */
suspend fun login(startKey: String): Result<LoginResponse> {
    // 这里的 market 参数实际上就是 startkey
    // URL 编码是必要的，因为 startkey 中包含 Base64 的特殊字符
    val encodedMarket = urlEncode(startKey)
    
    val url = "$BASE_URL/market/user/login/?market=$encodedMarket"
    
    println("发起登录请求 URL: $url")
    
    return safeApiCall<LoginResponse> {
        httpClient.get(url)                  
    }.map { response ->
        if (response.isSuccess) {
            response
        } else {
            throw IOException("登录请求失败，code: ${response.code}")
        }
    }
}    
    
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
 * 根据包名获取应用的所有版本列表
 * @param packageName 应用包名 (如 com.deepseek.chat)
 */
suspend fun getAppVersionsByPackage(
    packageName: String
): Result<List<WysAppListItem>> {
    // 复用 searchApps 逻辑，但固定使用 PACKAGE_NAME 类型
    return searchApps(packageName, SearchType.PACKAGE_NAME)
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