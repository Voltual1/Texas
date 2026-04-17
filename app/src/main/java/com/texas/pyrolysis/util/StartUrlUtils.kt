package com.texas.pyrolysis.util

import android.os.Build
import com.qx.wysappmarket.presentation.NativeLib
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DeviceInfo(
    val Model: String,
    val Board: String,
    val Android: String
)

object StartupTool {

    private const val BASE_URL = "https://api.wysteam.cn/market/start/"
    private const val BUILD_ID = "3400" // 对应 Smali 中的 "3400"

    /**
     * 完全复刻 Smali 逻辑
     */
    fun generateUrl(customModel: String? = null): String {
        // 1. 时间戳转换 (秒)
        val timestampSec = (System.currentTimeMillis() / 1000).toString()
        
        // 模拟 sa2.liIi(10, timestamp) - 确保长度
        val finalTimestamp = timestampSec.take(10).padStart(10, '0')

        // 2. 构造设备 JSON (严格对应 Smali 中的字段名)
        val deviceInfo = DeviceInfo(
            Model = customModel ?: Build.MODEL,
            Board = Build.BOARD,
            Android = Build.VERSION.SDK_INT.toString()
        )
        
        val deviceJson = Json.encodeToString(deviceInfo)

        // 3. 调用 Native 库
        // 记得确保 NativeLib 已经加载了 libwysappmarket.so
        return try {
            NativeLib.getStartupUrlNative(
                BASE_URL,
                BUILD_ID,
                deviceJson,
                finalTimestamp
            ) ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}