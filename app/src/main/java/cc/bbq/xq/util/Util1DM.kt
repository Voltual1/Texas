package cc.bbq.xq.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import androidx.core.content.FileProvider
import java.io.File
import java.util.*

/**
 * 1DM 下载工具类（纯逻辑版）
 * 职责：负责构建 Intent 并启动 1DM。
 * 注意：调用前请确保已处理 [android.permission.QUERY_ALL_PACKAGES] 或在 manifest 中添加 <queries> 标签。
 */
object Util1DM {
    private const val PACKAGE_NAME_1DM_PLUS = "idm.internet.download.manager.plus"
    private const val PACKAGE_NAME_1DM_NORMAL = "idm.internet.download.manager"
    private const val PACKAGE_NAME_1DM_LITE = "idm.internet.download.manager.adm.lite"
    private const val DOWNLOADER_ACTIVITY_NAME_1DM = "idm.internet.download.manager.Downloader"
    
    private const val SECURE_URI_1DM_SUPPORT_MIN_VERSION_CODE = 169
    private const val HEADERS_AND_MULTIPLE_LINKS_1DM_SUPPORT_MIN_VERSION_CODE = 157

    // Intent Extras
    private const val EXTRA_SECURE_URI = "secure_uri"
    private const val EXTRA_COOKIES = "extra_cookies"
    private const val EXTRA_USERAGENT = "extra_useragent"
    private const val EXTRA_REFERER = "extra_referer"
    private const val EXTRA_HEADERS = "extra_headers"
    private const val EXTRA_FILENAME = "extra_filename"
    private const val EXTRA_URL_LIST = "url_list"
    private const val EXTRA_URL_FILENAME_LIST = "url_list.filename"

    /**
     * 获取当前系统安装的 1DM 包名及其状态
     * @return Pair<String?, AppState> 包名（如果可用）和 状态
     */
    fun check1DMState(context: Context, secureUri: Boolean = false, hasHeadersOrMultiple: Boolean = false): Pair<String?, AppState> {
        val requiredVersion = when {
            secureUri -> SECURE_URI_1DM_SUPPORT_MIN_VERSION_CODE
            hasHeadersOrMultiple -> HEADERS_AND_MULTIPLE_LINKS_1DM_SUPPORT_MIN_VERSION_CODE
            else -> 0
        }

        val packages = listOf(PACKAGE_NAME_1DM_PLUS, PACKAGE_NAME_1DM_NORMAL, PACKAGE_NAME_1DM_LITE)
        var lastState = AppState.NOT_INSTALLED

        for (pkg in packages) {
            val state = get1DMAppState(context.packageManager, pkg, requiredVersion)
            if (state == AppState.OK) return pkg to AppState.OK
            if (state == AppState.UPDATE_REQUIRED) lastState = AppState.UPDATE_REQUIRED
        }
        return null to lastState
    }

    /**
     * 发起下载任务的主入口
     * @return true 如果成功启动了 1DM, false 如果由于未安装或版本低启动失败
     */
    fun download(
        context: Context,
        url: String? = null,
        urlAndFileNames: Map<String, String>? = null,
        referer: String? = null,
        fileName: String? = null,
        userAgent: String? = null,
        cookies: String? = null,
        headers: Map<String, String>? = null,
        secureUri: Boolean = false
    ): Boolean {
        val (packageName, state) = check1DMState(context, secureUri, !urlAndFileNames.isNullOrEmpty() || !headers.isNullOrEmpty())
        
        if (packageName == null || state != AppState.OK) return false

        val intent = Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(packageName, DOWNLOADER_ACTIVITY_NAME_1DM)
            putExtra(EXTRA_SECURE_URI, secureUri)
            
            if (urlAndFileNames.isNullOrEmpty()) {
                data = Uri.parse(url)
                if (!referer.isNullOrEmpty()) putExtra(EXTRA_REFERER, referer)
                if (!userAgent.isNullOrEmpty()) putExtra(EXTRA_USERAGENT, userAgent)
                if (!cookies.isNullOrEmpty()) putExtra(EXTRA_COOKIES, cookies)
                if (!fileName.isNullOrEmpty()) putExtra(EXTRA_FILENAME, fileName)
            } else {
                val urls = ArrayList<String>(urlAndFileNames.keys)
                val names = ArrayList<String>(urlAndFileNames.values)
                putExtra(EXTRA_URL_LIST, urls)
                putExtra(EXTRA_URL_FILENAME_LIST, names)
                data = Uri.parse(urls[0])
            }

            if (!headers.isNullOrEmpty()) {
                val extraBundle = Bundle()
                headers.forEach { (k, v) -> extraBundle.putString(k, v) }
                putExtra(EXTRA_HEADERS, extraBundle)
            }
            
            // 确保在 Activity 之外调用时也能运行
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 发送本地种子文件
     */
    fun downloadTorrentFile(context: Context, torrentFile: File, authority: String): Boolean {
        val (packageName, _) = check1DMState(context)
        if (packageName == null) return false

        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, authority, torrentFile)
        } else {
            Uri.fromFile(torrentFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            component = ComponentName(packageName, DOWNLOADER_ACTIVITY_NAME_1DM)
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun get1DMAppState(pm: PackageManager, pkg: String, requiredVersion: Int): AppState {
        return try {
            val info: PackageInfo = pm.getPackageInfo(pkg, 0)
            if (requiredVersion <= 0 || info.versionCode >= requiredVersion) AppState.OK 
            else AppState.UPDATE_REQUIRED
        } catch (e: PackageManager.NameNotFoundException) {
            AppState.NOT_INSTALLED
        }
    }

    enum class AppState { OK, UPDATE_REQUIRED, NOT_INSTALLED }
}