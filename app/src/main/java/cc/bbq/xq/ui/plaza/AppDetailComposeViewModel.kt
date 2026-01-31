//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.plaza

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.bbq.xq.AppStore
import cc.bbq.xq.data.repository.IAppStoreRepository
import cc.bbq.xq.data.unified.*
import kotlinx.coroutines.flow.MutableSharedFlow
import cc.bbq.xq.KtorClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import cc.bbq.xq.LingMarketClient
//import cc.bbq.xq.service.download.DownloadService

@KoinViewModel
class AppDetailComposeViewModel(
    application: Application,
    private val repositories: Map<AppStore, IAppStoreRepository>
) : AndroidViewModel(application) {

    private val _appDetail = MutableStateFlow<UnifiedAppDetail?>(null)
    val appDetail: StateFlow<UnifiedAppDetail?> = _appDetail.asStateFlow()

    private val _comments = MutableStateFlow<List<UnifiedComment>>(emptyList())
    val comments: StateFlow<List<UnifiedComment>> = _comments.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _showCommentDialog = MutableStateFlow(false)
    val showCommentDialog: StateFlow<Boolean> = _showCommentDialog.asStateFlow()

    private val _showReplyDialog = MutableStateFlow(false)
    val showReplyDialog: StateFlow<Boolean> = _showReplyDialog.asStateFlow()

    private val _currentReplyComment = MutableStateFlow<UnifiedComment?>(null)
    val currentReplyComment: StateFlow<UnifiedComment?> = _currentReplyComment.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentStore: AppStore = AppStore.XIAOQU_SPACE
    private var currentAppId: String = ""
    private var currentVersionId: Long = 0L

    private val _downloadSources = MutableStateFlow<List<UnifiedDownloadSource>>(emptyList())
    val downloadSources: StateFlow<List<UnifiedDownloadSource>> = _downloadSources.asStateFlow()

    private val _showDownloadDrawer = MutableStateFlow(false)
    val showDownloadDrawer: StateFlow<Boolean> = _showDownloadDrawer.asStateFlow()

    // 新增：Snackbar 事件
    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()
    
    // 新增：导航事件
    private val _navigateToDownloadEvent = MutableSharedFlow<Boolean>()
    val navigateToDownloadEvent: SharedFlow<Boolean> = _navigateToDownloadEvent.asSharedFlow()

    // 新增：用于发送一次性事件（如打开浏览器）
    private val _openUrlEvent = MutableSharedFlow<String>()
    val openUrlEvent: SharedFlow<String> = _openUrlEvent.asSharedFlow()

    private val _currentTab = MutableStateFlow(0) // 0: 详情, 1: 版本列表
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()
    
    // 分享链接
    private val _shareLink = MutableStateFlow<String?>(null)
    val shareLink: StateFlow<String?> = _shareLink.asStateFlow()

    private val repository: IAppStoreRepository
        get() = repositories[currentStore] ?: throw IllegalStateException("Repository not found")
        
    // 退币事件
    private val _refundEvent = MutableSharedFlow<RefundInfo>()
    val refundEvent: SharedFlow<RefundInfo> = _refundEvent.asSharedFlow()

    // 更新事件
    private val _updateEvent = MutableSharedFlow<String>()
    val updateEvent: SharedFlow<String> = _updateEvent.asSharedFlow()
    
    // 支付相关状态和方法
    private val _navigateToPaymentEvent = MutableSharedFlow<PaymentInfo>()
    val navigateToPaymentEvent: SharedFlow<PaymentInfo> = _navigateToPaymentEvent.asSharedFlow()

    // 支付信息数据类
    data class PaymentInfo(
        val appId: Long,
        val appName: String,
        val versionId: Long,
        val price: Int,
        val iconUrl: String,
        val previewContent: String
    )

    // 退款信息数据类
    data class RefundInfo(
        val appId: String,
        val versionId: Long,
        val appName: String,
        val payMoney: Int
    )    

    // 新增：获取设备SDK版本
    val deviceSdkVersion: Int
        get() = Build.VERSION.SDK_INT

    fun initializeData(appId: String, versionId: Long, storeName: String) {
        val store = try {
            AppStore.valueOf(storeName)
        } catch (e: Exception) {
            AppStore.XIAOQU_SPACE
        }

        if (currentAppId == appId && currentVersionId == versionId && currentStore == store && _appDetail.value != null) {
            return
        }

        currentAppId = appId
        currentVersionId = versionId
        currentStore = store

        loadData()
    }
    
    // 请求退款功能（仅小趣空间）
    fun requestRefund() {
        viewModelScope.launch {
            val detail = _appDetail.value ?: return@launch
            
            if (detail.store == AppStore.XIAOQU_SPACE) {
                val raw = detail.raw as? cc.bbq.xq.KtorClient.AppDetail
                if (raw != null) {
                    _refundEvent.emit(
                        RefundInfo(
                            appId = currentAppId,
                            versionId = currentVersionId,
                            appName = detail.name,
                            payMoney = raw.pay_money
                        )
                    )
                }
            } else {
                _snackbarEvent.tryEmit("该商店不支持退币功能")
            }
        }
    }

    // 请求更新功能（仅小趣空间）
    fun requestUpdate() {
        viewModelScope.launch {
            val detail = _appDetail.value ?: return@launch
            
            if (detail.store == AppStore.XIAOQU_SPACE) {
                val raw = detail.raw as? cc.bbq.xq.KtorClient.AppDetail
                if (raw != null) {
                    // 使用 KtorClient 的 JsonConverter 将 AppDetail 转换为 JSON 字符串
                    val appDetailJson = KtorClient.JsonConverter.toJson(raw)
                    _updateEvent.emit(appDetailJson)
                }
            } else {
                _snackbarEvent.tryEmit("该商店不支持更新功能")
            }
        }
    }        

    fun refresh() {
        loadData()
    }

    // 检查是否需要购买
    fun checkPurchaseNeeded(): PaymentInfo? {
        val detail = _appDetail.value ?: return null
        
        // 仅小趣空间应用需要检查购买状态
        if (detail.store == AppStore.XIAOQU_SPACE) {
            val raw = detail.raw as? cc.bbq.xq.KtorClient.AppDetail
            if (raw != null && raw.is_pay == 1 && raw.pay_money > 0 && raw.is_user_pay != true) {
                return PaymentInfo(
                    appId = raw.id,
                    appName = raw.appname,
                    versionId = raw.apps_version_id,
                    price = raw.pay_money,
                    iconUrl = raw.app_icon,
                    previewContent = raw.app_introduce?.take(30) ?: ""
                )
            }
        }
        return null
    }

    fun handleDownloadClick() {
    viewModelScope.launch {
        val paymentInfo = checkPurchaseNeeded()
        if (paymentInfo != null) {
            // 需要购买，触发支付事件
            _navigateToPaymentEvent.emit(paymentInfo)
        } else {
            // 不需要购买，继续原有下载逻辑
            _isLoading.value = true
            
            // 获取当前应用详情
            val detail = _appDetail.value
            
            // 检查是否为灵应用商店且是否有直接下载URL
            if (detail != null && detail.store == AppStore.LING_MARKET) {
                // 对于灵应用商店，我们已经在 getAppDetail 中尝试获取了直接URL
                if (detail.downloadUrl != null) {
                    // 有直接下载URL，直接使用
                    startDownload(detail.downloadUrl!!)
                } else {
                    // 如果没有直接下载URL，尝试从原始数据获取apkKey并请求下载URL
                    handleLingMarketDownload(detail)
                }
            } else {
                // 其他商店，使用原有逻辑
                val result = repository.getAppDownloadSources(currentAppId, currentVersionId)
                _isLoading.value = false

                if (result.isSuccess) {
                    val sources = result.getOrThrow()
                    if (sources.isEmpty()) {
                        _errorMessage.value = "未找到下载源"
                    } else if (sources.size == 1) {
                        // 只有一个源，直接触发下载
                        startDownload(sources.first().url)
                    } else {
                        // 多个源，显示抽屉
                        _downloadSources.value = sources
                        _showDownloadDrawer.value = true
                    }
                } else {
                    _errorMessage.value = "获取下载链接失败: ${result.exceptionOrNull()?.message}"
                }
            }
        }
    }
}

/**
 * 处理灵应用商店的下载逻辑
 */
private suspend fun handleLingMarketDownload(detail: UnifiedAppDetail) {
    try {
        // 从原始数据获取apkKey
        val raw = detail.raw as? LingMarketClient.LingMarketApp
        if (raw != null && raw.apkKey.isNotEmpty()) {
            // 获取直接下载URL
            val result = LingMarketClient.getFileDownloadUrl(raw.apkKey)
            _isLoading.value = false
            
            if (result.isSuccess) {
                val downloadUrl = result.getOrThrow().url
                startDownload(downloadUrl)
            } else {
                _errorMessage.value = "获取下载链接失败: ${result.exceptionOrNull()?.message}"
            }
        } else {
            _isLoading.value = false
            _errorMessage.value = "未找到应用文件信息"
        }
    } catch (e: Exception) {
        _isLoading.value = false
        _errorMessage.value = "处理下载时出错: ${e.message}"
    }
}

    fun closeDownloadDrawer() {
        _showDownloadDrawer.value = false
    }

    private fun loadData() {
        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                val detailResult = repository.getAppDetail(currentAppId, currentVersionId)

                if (detailResult.isSuccess) {
                    var detail = detailResult.getOrThrow()
                    
                    // 如果是弦应用商店，需要在 raw 数据中添加设备SDK信息
                    if (currentStore == AppStore.SIENE_SHOP && detail.raw is cc.bbq.xq.SineShopClient.SineShopAppDetail) {
                        val raw = detail.raw as cc.bbq.xq.SineShopClient.SineShopAppDetail
                        // 这里我们不需要修改 raw 对象，因为在 Composable 中会动态计算
                    }
                    
                    _appDetail.value = detail
                    loadComments()
                    
                    // 移除：不再在此处加载版本列表
                    //if (currentStore == AppStore.SIENE_SHOP) {
                    //    loadVersionList()
                    //}
                } else {
                    _errorMessage.value = "加载详情失败: ${detailResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "网络错误: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadComments() {
        viewModelScope.launch {
            // 传入 currentVersionId
            val result = repository.getAppComments(currentAppId, currentVersionId, 1)
            if (result.isSuccess) {
                _comments.value = result.getOrThrow().first
            }
        }
    }

    fun openCommentDialog() {
        _showCommentDialog.value = true
        _currentReplyComment.value = null
    }

    fun closeCommentDialog() {
        _showCommentDialog.value = false
    }

    fun openReplyDialog(comment: UnifiedComment) {
        _currentReplyComment.value = comment
        _showReplyDialog.value = true
    }

    fun closeReplyDialog() {
        _showReplyDialog.value = false
        _currentReplyComment.value = null
    }

    fun submitComment(content: String) {
    viewModelScope.launch {
        val parentId = _currentReplyComment.value?.id
        val result = repository.postComment(currentAppId, currentVersionId, content, parentId, null)

        if (result.isSuccess) {
            loadComments()
            if (parentId == null) closeCommentDialog() else closeReplyDialog()
        } else {
            _errorMessage.value = "提交失败: ${result.exceptionOrNull()?.message}"
        }
    }
}

    fun deleteComment(commentId: String) {
    viewModelScope.launch {
        // 判断是否为灵应用商店，如果是则使用新方法
        val result = if (currentStore == AppStore.LING_MARKET) {
            repository.deleteComment(currentAppId, commentId)
        } else {
            repository.deleteComment(commentId)
        }
        
        if (result.isSuccess) {
            loadComments()
        } else {
            _errorMessage.value = "删除失败: ${result.exceptionOrNull()?.message}"
        }
    }
}

    // 删除应用（无需鉴权，服务器会处理）
    fun deleteApp(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // 直接调用仓库的删除方法，服务器会自行鉴权
            val result = repository.deleteApp(currentAppId, currentVersionId)
            if (result.isSuccess) {
                onSuccess()
            } else {
                val error = result.exceptionOrNull()?.message ?: "删除失败"
                _errorMessage.value = error
            }
        }
    }
        
    // 启动下载
    private fun startDownload(downloadUrl: String) {
        viewModelScope.launch {
            try {
                // 触发 Service 开始下载
                val appName = "${_appDetail.value?.name ?: "未命名应用"}.apk"
                getApplication<Application>().startDownload(downloadUrl, appName)
                                
                // 发送导航到下载管理界面的事件
                _navigateToDownloadEvent.emit(true)
                
            } catch (e: Exception) {
                _errorMessage.value = "启动下载失败: ${e.message}"
            }
        }
    }

    // 扩展函数：启动下载服务
private fun Application.startDownload(downloadUrl: String, fileName: String) {
/*    val intent = Intent(this, cc.bbq.xq.service.download.DownloadService::class.java)
    intent.action = DownloadService.ACTION_START_DOWNLOAD  // 明确指定 action
    intent.putExtra(DownloadService.EXTRA_URL, downloadUrl)  // 使用正确的 key
    intent.putExtra(DownloadService.EXTRA_FILE_NAME, fileName)
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}*/}