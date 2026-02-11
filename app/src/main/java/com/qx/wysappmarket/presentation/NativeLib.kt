package com.qx.wysappmarket.presentation

/**
 * 对应 libwysappmarket.so 的 Native 接口
 * 严格对标 Smali 定义
 */
object NativeLib {

    init {
        // Smali 中是在 static constructor <clinit> 里加载的
        System.loadLibrary("wysappmarket")
    }

    // --- 核心 URL 构造方法 ---

    external fun getStartupUrlNative(
        p1: String?, 
        p2: String?, 
        p3: String?, 
        p4: String?
    ): String?

    external fun getLoginUrlNative(p1: String?, p2: String?): String?

    external fun getLogoutUrlNative(
        p1: String?, 
        p2: String?, 
        p3: String?, 
        p4: String?
    ): String?

    external fun getAppDownloadUrlNative(
        p1: String?, 
        p2: Int,       // 注意这个是 Int (I)
        p3: String?, 
        p4: String?, 
        p5: String?
    ): String?

    // --- 加解密与安全相关 ---

    external fun encryptTokenNative(token: String?): String?

    external fun decryptTokenNative(token: String?): String?

    external fun decodeSecureNative(data: String?): String?

    external fun hashsha256Native(data: String?): String?

    /**
     * 注意：这个方法在 Smali 中存在，但你漏掉了。
     * 它的返回类型是 Void (V)，参数是 String。
     * 这可能是校验配置的关键点。
     */
    external fun validateAndFixConfigNative(config: String?)

    /**
     * 同样是漏掉的方法：获取签名哈希。
     * so 内部可能用这个来比对当前运行环境的签名。
     */
    external fun getSignatureHashNative(): String?
}