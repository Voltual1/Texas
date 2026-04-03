package com.texas.pyrolysis.util

import android.app.Activity

/**
 * 统一下载管理器
 * 封装第三方下载器的调用细节，解耦业务逻辑
 */
object DownloadManager {

    /**
     * 调起 1DM 下载文件
     * @param activity 上下文
     * @param url 下载地址
     * @param fileName 建议文件名
     * @param headers 自定义请求头（如：User-Agent, Cookie, Referer）
     */
    fun download(
        activity: Activity,
        url: String,
        fileName: String? = null,
        headers: Map<String, String>? = null
    ) {
        try {
            // 提取常用的 Header 字段，1DM 示例支持这些特定字段的简化传递
            val referer = headers?.get("Referer")
            val userAgent = headers?.get("User-Agent")
            val cookies = headers?.get("Cookie")

            // 调用 1DM 示例工具类
            Util1DM.downloadFile(
                activity = activity,
                url = url,
                referer = referer,
                fileName = fileName,
                userAgent = userAgent,
                cookies = cookies,
                headers = headers, // 也可以传递完整的 Map
                secureUri = false,
                askUserToInstall1DMIfNotInstalled = true // 如果没装，引导用户安装
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // 这里可以做全局错误提示，比如弹出 Toast 告知调用失败
        }
    }
}