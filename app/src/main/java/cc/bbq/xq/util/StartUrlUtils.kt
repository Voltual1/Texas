package cc.bbq.xq.util // 你自己的包名

import android.os.Build
import com.qx.wysappmarket.presentation.NativeLib

object StartupTool {

    private const val BASE_URL = "https://api.wysteam.cn/market/start/"
    private const val CHANNEL_ID = "3301"

    /**
     * 复刻 Smali 逻辑生成启动 URL
     */
    fun generateUrl(): String {
        // 1. 获取时间戳并除以 1000 得到秒
        val timestampLong = System.currentTimeMillis() / 1000
        
        // 2. 对应 wa2.lill(10, timestamp)
        // 逻辑通常是强制补足10位，如果时间戳已经是10位则不变
        val timestampStr = timestampLong.toString().padStart(10, '0')

        // 3. 获取设备型号 (Smali 里的 Build.MODEL)
        val deviceModel = Build.MODEL 

        // 4. 调用搬运过来的 Native 方法
        // 注意：原 Smali 中 v2 是从 SecureConfig 拿的 BaseURL
        val result = NativeLib.getStartupUrlNative(
            BASE_URL,
            CHANNEL_ID,
            deviceModel,
            timestampStr
        )

        return result ?: ""
    }
}