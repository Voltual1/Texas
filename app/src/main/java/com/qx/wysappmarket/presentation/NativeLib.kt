package com.qx.wysappmarket.presentation

/**
 * 对应 libwysappmarket.so 的 Native 接口
 */
object NativeLib {

    init {
        // 加载 libwysappmarket.so
        System.loadLibrary("wysappmarket")
    }

    external fun getStartupUrlNative(
        baseUrl: String?,
        channelId: String?,
        model: String?,
        timestamp: String?
    ): String?

    external fun decodeSecureNative(data: String?): String?

    external fun decryptTokenNative(token: String?): String?

    external fun encryptTokenNative(token: String?): String?

    external fun getAppDownloadUrlNative(
        p1: String?, p2: Int, p3: String?, p4: String?, p5: String?
    ): String?

    external fun getLoginUrlNative(p1: String?, p2: String?): String?

    external fun getLogoutUrlNative(p1: String?, p2: String?, p3: String?, p4: String?): String?

    external fun hashsha256Native(data: String?): String?
}