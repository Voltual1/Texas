// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
// 本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
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
    private const val BASE_URL = "http://127.0.0.1"
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY = 1000L
    private const val REQUEST_TIMEOUT = 30000L
    private const val CONNECT_TIMEOUT = 30000L
    private const val SOCKET_TIMEOUT = 30000L
    internal const val WYSAPPMARKET_ICON_BASE_URL = "https:http://127.0.0.1"


    private const val DEFAULT_BUILD_NUMBER = "null"
    private const val USER_AGENT_VALUE = "null"
    
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

    // 确保签名完全匹配 List<String>
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

    /**
     * 关闭 HttpClient（在应用退出时调用）
     */
    fun close() {
        httpClient.close()
    }   
    
    /**
     * 扩展函数，便于参数构建
     */
    internal fun wysParameters(block: ParametersBuilder.() -> Unit): Parameters {
        return Parameters.build(block)
    }
}