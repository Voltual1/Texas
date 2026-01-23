//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.plaza

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
// 移除 MD2 的 ExperimentalMaterialApi 和 pullrefresh 导入
// import androidx.compose.material.ExperimentalMaterialApi
// import androidx.compose.material.pullrefresh.PullRefreshIndicator
// import androidx.compose.material.pullrefresh.pullRefresh
// import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// 添加 MD3 pullrefresh 导入
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import cc.bbq.xq.ui.theme.BBQDropdownMenu
import cc.bbq.xq.ui.compose.LinkifyText
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cc.bbq.xq.data.unified.UnifiedAppDetail
import cc.bbq.xq.data.unified.UnifiedComment
import cc.bbq.xq.ui.ImagePreview
import cc.bbq.xq.ui.UserDetail
import cc.bbq.xq.ui.PaymentForApp
import cc.bbq.xq.ui.community.compose.CommentDialog
import cc.bbq.xq.ui.CreateRefundPost
import cc.bbq.xq.ui.UpdateAppRelease
import cc.bbq.xq.ui.community.compose.CommentItem
import cc.bbq.xq.ui.theme.BBQSnackbarHost
import cc.bbq.xq.ui.theme.DownloadSourceDrawer
// 导入我们自定义的指示器
import cc.bbq.xq.ui.theme.BBQPullRefreshIndicator // <<<--- 新增导入
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import cc.bbq.xq.ui.Download
import androidx.compose.foundation.background
import cc.bbq.xq.AppStore
import cc.bbq.xq.util.formatTimestamp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import cc.bbq.xq.ui.theme.UnifiedCommentItem
import cc.bbq.xq.LingMarketClient

// 移除 @ExperimentalMaterialApi 注解
// @OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppDetailScreen(
    appId: String,
    versionId: Long,
    storeName: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: AppDetailComposeViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val appDetail by viewModel.appDetail.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val showCommentDialog by viewModel.showCommentDialog.collectAsState()
    val showReplyDialog by viewModel.showReplyDialog.collectAsState()
    val currentReplyComment by viewModel.currentReplyComment.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showDownloadDrawer by viewModel.showDownloadDrawer.collectAsState()
    val downloadSources by viewModel.downloadSources.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // 应用删除确认对话框
    var showDeleteAppDialog by remember { mutableStateOf(false) }
    // 评论删除确认对话框
    var showDeleteCommentDialog by remember { mutableStateOf(false) }
    var commentToDeleteId by remember { mutableStateOf<String?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }

    LaunchedEffect(appId, versionId, storeName) {
        viewModel.initializeData(appId, versionId, storeName)
    }

    LaunchedEffect(Unit) {
        viewModel.openUrlEvent.collectLatest { url ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("无法打开链接: $url")
            }
        }
    }

    // 监听 ViewModel 中的 snackbarEvent
    LaunchedEffect(viewModel.snackbarEvent) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // 监听更新事件
    LaunchedEffect(viewModel.snackbarEvent) {
        viewModel.updateEvent.collectLatest { jsonString ->
            // 直接导航到 UpdateAppRelease
            navController.navigate(UpdateAppRelease(jsonString).createRoute())
        }
    }

    // 监听退款和更新事件
    LaunchedEffect(Unit) {
        viewModel.refundEvent.collectLatest { refundInfo ->
            navController.navigate(
                CreateRefundPost(
                    appId = refundInfo.appId.toLongOrNull() ?: 0L,
                    versionId = refundInfo.versionId,
                    appName = refundInfo.appName,
                    payMoney = refundInfo.payMoney
                ).createRoute()
            )
        }
    }

    // 监听支付导航事件
    LaunchedEffect(Unit) {
        viewModel.navigateToPaymentEvent.collectLatest { paymentInfo ->
            navController.navigate(
                PaymentForApp(
                    appId = paymentInfo.appId,
                    appName = paymentInfo.appName,
                    versionId = paymentInfo.versionId,
                    price = paymentInfo.price,
                    iconUrl = paymentInfo.iconUrl,
                    previewContent = paymentInfo.previewContent
                ).createRoute()
            )
        }
    }

    // 监听 ViewModel 中的 navigateToDownloadEvent
    LaunchedEffect(viewModel.navigateToDownloadEvent) {
        viewModel.navigateToDownloadEvent.collectLatest { navigate ->
            if (navigate) {
                navController.navigate(Download.route)
            }
        }
    }

    // 下拉刷新状态
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(errorMessage)
            }
        }
    }

    // 监听 ViewModel 状态变化以结束刷新状态
    LaunchedEffect(isRefreshing, isLoading, appDetail, errorMessage) {
        // 当加载完成（isLoading 变为 false）且有数据或出错时，结束刷新
        if (!isLoading && (appDetail != null || errorMessage.isNotEmpty()) && isRefreshing) {
            isRefreshing = false
        }
    }

    // 处理分享功能
    fun handleShare() {
        appDetail?.let { detail ->
            when (detail.store) {
                AppStore.XIAOQU_SPACE -> {
                    // 小趣空间：使用 posturl
                    val raw = detail.raw as? cc.bbq.xq.KtorClient.AppDetail
                    val shareUrl = raw?.posturl
                    if (!shareUrl.isNullOrBlank()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("应用链接", shareUrl)
                        clipboard.setPrimaryClip(clip)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("已复制分享链接: $shareUrl")
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("分享链接无效")
                        }
                    }
                }
                AppStore.SIENE_SHOP -> {
                    // 弦应用商店：使用自定义格式
                    val shareUrl = "sinemarket://app/${detail.id}"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("应用链接", shareUrl)
                    clipboard.setPrimaryClip(clip)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("已复制分享链接: $shareUrl")
                    }
                }
                AppStore.SINE_OPEN_MARKET -> {
                    // 弦开放市场：暂不支持分享
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("暂不支持该商店的分享功能")
                    }
                }
                AppStore.LING_MARKET -> {
                                        coroutineScope.launch {
                        snackbarHostState.showSnackbar("暂不支持该商店的分享功能")
                                    }
                                    }
                else -> {
                    // 暂不支持分享
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("暂不支持该商店的分享功能")
                    }
                }
            }
        }
    }

    // 使用 MD3 的 PullToRefreshBox
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.refresh()
            // 结束刷新状态的逻辑由 LaunchedEffect 处理
        },
        state = pullRefreshState,
        // 使用我们自定义的指示器
        indicator = {
            BBQPullRefreshIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
                // 颜色和形状将使用我们在 Components.kt 中定义的默认值（语义颜色）
            )
        },
        modifier = modifier.fillMaxSize()
    ) {
        // Box(modifier = modifier.fillMaxSize().pullRefresh(pullRefreshState)) { // 移除旧的 Box 包裹
        if (isLoading && appDetail == null) { // 仅在初始加载且无数据时显示
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (appDetail != null) {
            val pageCount = if (appDetail!!.store == AppStore.SIENE_SHOP) 2 else 1
            val pagerState = rememberPagerState(pageCount = { pageCount })
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> {
                        AppDetailContent(
                            navController = navController,
                            appDetail = appDetail!!,
                            comments = comments,
                            onCommentReply = { viewModel.openReplyDialog(it) },
                            onDownloadClick = { viewModel.handleDownloadClick() },
                            onCommentLongClick = { commentId ->
                                commentToDeleteId = commentId
                                showDeleteCommentDialog = true
                            },
                            onDeleteAppClick = { showDeleteAppDialog = true },
                            onShareClick = { handleShare() },
                            onMoreMenuClick = { showMoreMenu = true },
                            onImagePreview = { url ->
                                navController.navigate(ImagePreview(url).createRoute())
                            },
                            onRefundClick = { viewModel.requestRefund() },
                            onUpdateClick = { viewModel.requestUpdate() }
                        )
                    }
                    1 -> {
                        if (appDetail!!.store == AppStore.SIENE_SHOP) {
                            // 版本列表页面
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("版本列表功能？还没做完呢")
                            }
                        } else {
                            Text("版本列表仅在弦应用商店提供")
                        }
                    }
                }
            }
        }
        // 浮动评论按钮
        FloatingActionButton(
            onClick = { viewModel.openCommentDialog() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.AutoMirrored.Filled.Comment, "评论")
        }
        // 移除旧的 PullRefreshIndicator
        // PullRefreshIndicator(
        //     refreshing,
        //     pullRefreshState,
        //     Modifier.align(Alignment.TopCenter),
        //     backgroundColor = MaterialTheme.colorScheme.surface,
        //     contentColor = MaterialTheme.colorScheme.primary
        // )
        BBQSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        // } // End of old Box
    } // End of PullToRefreshBox

    // 删除应用确认对话框
    if (showDeleteAppDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAppDialog = false },
            title = { Text("确认删除应用") },
            text = { Text("确定要删除此应用吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAppDialog = false
                    viewModel.deleteApp {
                        navController.popBackStack()
                    }
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAppDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 删除评论确认对话框
    if (showDeleteCommentDialog && commentToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteCommentDialog = false },
            title = { Text("确认删除评论") },
            text = { Text("确定要删除这条评论吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteCommentDialog = false
                    commentToDeleteId?.let { viewModel.deleteComment(it) }
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCommentDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    DownloadSourceDrawer(
        show = showDownloadDrawer,
        onDismissRequest = { viewModel.closeDownloadDrawer() },
        sources = downloadSources,
        onSourceSelected = { source ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                context.startActivity(intent)
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("无法打开链接")
                }
            }
        }
    )

    if (showCommentDialog) {
        CommentDialog(
            hint = "输入评论...",
            onDismiss = { viewModel.closeCommentDialog() },
            context = context,
            onSubmit = { content, _ ->
                viewModel.submitComment(content)
            }
        )
    }

    if (showReplyDialog && currentReplyComment != null) {
        CommentDialog(
            hint = "回复 @${currentReplyComment!!.sender.displayName}",
            onDismiss = { viewModel.closeReplyDialog() },
            context = context,
            onSubmit = { content, _ ->
                viewModel.submitComment(content)
            }
        )
    }
}

@Composable
fun AppDetailContent(
    navController: NavController,
    appDetail: UnifiedAppDetail,
    comments: List<UnifiedComment>,
    onCommentReply: (UnifiedComment) -> Unit,
    onDownloadClick: () -> Unit,
    onCommentLongClick: (String) -> Unit,
    onDeleteAppClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreMenuClick: () -> Unit,
    onImagePreview: (String) -> Unit,
        onRefundClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
var showMoreMenu by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 应用头部信息 ---
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = appDetail.iconUrl,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                                .clickable { onImagePreview(appDetail.iconUrl) },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                appDetail.name, 
                                style = MaterialTheme.typography.titleLarge, 
                                fontWeight = FontWeight.Bold
                            )
                            Text("版本: ${appDetail.versionName}", style = MaterialTheme.typography.bodyMedium)
                            Text("大小: ${appDetail.size ?: "未知"}", style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true }
                            ) {
                                Icon(Icons.Default.MoreVert, "更多")
                            }
                            
                            // 下拉菜单直接与按钮关联
                            BBQDropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                modifier = Modifier.width(180.dp)
                            ) {
                                // 分享选项
                                DropdownMenuItem(
                                    text = { Text("分享应用") },
                                    onClick = {
                                        showMoreMenu = false
                                        onShareClick()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                    }
                                )
                                
                                // 根据商店类型显示不同选项
                                when (appDetail.store) {
                                    AppStore.XIAOQU_SPACE -> {
                                        val raw = appDetail.raw as? cc.bbq.xq.KtorClient.AppDetail
                                        
                                        // 更新选项（总是显示）
                                        DropdownMenuItem(
                                            text = { Text("更新应用") },
                                            onClick = {
                                                showMoreMenu = false
                                                onUpdateClick()
                                            }
                                        )
                                        
                                        // 退币选项（仅当应用是付费应用且用户已购买后时显示）
                                        if (raw?.is_pay == 1 && raw.is_user_pay == true) {
                                            DropdownMenuItem(
                                                text = { Text("申请退币") },
                                                onClick = {
                                                    showMoreMenu = false
                                                    onRefundClick()
                                                }
                                            )
                                        }
                                        
                                        // 删除选项
                                        DropdownMenuItem(
                                            text = { Text("删除应用") },
                                            onClick = {
                                                showMoreMenu = false
                                                onDeleteAppClick()
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Delete, contentDescription = null)
                                            }
                                        )
                                    }
                                    else -> {
                                        // 本地商店：无特殊选项
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // 显示付费信息（如果是付费应用）
                    val raw = appDetail.raw as? cc.bbq.xq.KtorClient.AppDetail
                    
                    Button(
                    onClick = onDownloadClick,  // 这里会触发购买检查
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true  // 总是启用，ViewModel会处理购买逻辑
                ) {
                    Icon(Icons.Filled.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            raw?.is_pay == 1 && raw.pay_money > 0 && raw.is_user_pay != true -> 
                                "购买并下载 (${raw.pay_money}硬币)"
                            raw?.is_pay == 1 && raw.is_user_pay == true -> 
                                "下载应用 (已购买)"
                            else -> "下载应用"
                        }
                    )
                }
            }
        }
    }
        
// 更新日志（弦应用商店和灵应用商店）---
val updateLog = when (appDetail.store) {
    AppStore.SIENE_SHOP -> appDetail.updateLog
    AppStore.LING_MARKET -> {
        // 直接从 UnifiedAppDetail 的 updateLog 字段获取
        appDetail.updateLog
    }
    else -> null
}

if (!updateLog.isNullOrEmpty()) {
    item {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("更新日志", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LinkifyText(
                    text = updateLog,
                    navController = navController,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }
        }
    }
}

        // --- 适配说明（小趣空间） ---
if (appDetail.store == AppStore.XIAOQU_SPACE) {
    val appExplain = when (val raw = appDetail.raw) {
        is cc.bbq.xq.KtorClient.AppDetail -> raw.app_explain
        else -> null
    }
    
    if (!appExplain.isNullOrEmpty()) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("适配说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LinkifyText(
                        text = appExplain,
                        navController = navController,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        }
    }
}

        // --- 应用信息卡片 ---
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("应用信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    // 根据不同商店显示不同的信息字段
                    when (appDetail.store) {
                        AppStore.XIAOQU_SPACE -> {
                            // 小趣空间信息
                            val raw = appDetail.raw as? cc.bbq.xq.KtorClient.AppDetail
                            
                            InfoRow(
                                label = "应用类型",
                                value = appDetail.type
                            )
                            InfoRow(
                                label = "下载次数",
                                value = "${appDetail.downloadCount} 次"
                            )
                            if (appDetail.size != null) {
                                InfoRow(
                                    label = "安装包大小",
                                    value = appDetail.size
                                )
                            }
                            InfoRow(
                                label = "上传时间",
                                value = raw?.create_time ?: "未知"
                            )
                            InfoRow(
                                label = "更新时间",
                                value = raw?.update_time ?: "未知"
                            )
                        }
                        AppStore.SIENE_SHOP -> {
                            // 弦应用商店信息
                            val raw = appDetail.raw as? cc.bbq.xq.SineShopClient.SineShopAppDetail
                            val deviceInfo = getDeviceInfo(raw?.app_sdk_min ?: 0)
                            
                            InfoRow(
                                label = "应用类型",
                                value = appDetail.type
                            )
                            InfoRow(
                                label = "版本类型",
                                value = raw?.app_version_type ?: "未知"
                            )
                            
                            // 支持系统信息（包含最低SDK、目标SDK和设备兼容性）
                            val supportSystem = buildString {
                                append("最低SDK: ${raw?.app_sdk_min ?: "未知"}")
                                if (raw?.app_sdk_target != null && raw.app_sdk_target != raw.app_sdk_min) {
                                    append(" (目标SDK: ${raw.app_sdk_target})")
                                }
                                append(" • ")
                                append(deviceInfo)
                            }
                            InfoRow(
                                label = "支持系统",
                                value = supportSystem
                            )
                            
                            if (appDetail.size != null) {
                                InfoRow(
                                    label = "安装包大小",
                                    value = appDetail.size
                                )
                            }
                            InfoRow(
                                label = "下载次数",
                                value = "${appDetail.downloadCount} 次"
                            )
                            InfoRow(
                                label = "应用开发者",
                                value = raw?.app_developer ?: "未知"
                            )
                            InfoRow(
                                label = "应用来源",
                                value = raw?.app_source ?: "未知"
                            )
                            InfoRow(
                                label = "上传时间",
                                value = if (raw?.upload_time != null) formatTimestamp(raw.upload_time) else "未知"
                            )
                            InfoRow(
                                label = "资料时间",
                                value = if (raw?.update_time != null) formatTimestamp(raw.update_time) else "未知"
                            )
                            
                            // 显示应用标签
                            if (!raw?.tags.isNullOrEmpty()) {
                                InfoRow(
                                    label = "应用标签",
                                    value = raw?.tags?.joinToString(", ") { it.name } ?: ""
                                )
                            }
                            
                            // 显示审核状态（如果有审核失败的情况）
                            if (raw?.audit_status == 0 && !raw.audit_reason.isNullOrEmpty()) {
                                InfoRow(
                                    label = "审核状态",
                                    value = raw.audit_reason
                                )
                            }
                        }
                        AppStore.SINE_OPEN_MARKET -> {
                            // 弦开放市场信息
                            InfoRow(
                                label = "应用类型",
                                value = appDetail.type
                            )
                            if (appDetail.size != null) {
                                InfoRow(
                                    label = "安装包大小",
                                    value = appDetail.size
                                )
                            }
                            InfoRow(
                                label = "下载次数",
                                value = "${appDetail.downloadCount} 次"
                            )
                        }                        
// 修改 AppDetailContent 函数中的灵应用商店信息显示部分
AppStore.LING_MARKET -> {
    val raw = appDetail.raw as? LingMarketClient.LingMarketApp
    
    // SDK 信息
    val minSdk = raw?.minSdk
    val targetSdk = raw?.targetSdk
    if (minSdk != null && targetSdk != null) {
        InfoRow(
            label = "SDK",
            value = "Min $minSdk / Target $targetSdk"
        )
    } else if (minSdk != null) {
        InfoRow(
            label = "SDK",
            value = "Min $minSdk"
        )
    }
    
    // 架构信息 - 检查字段名是否正确
    val architectures = raw?.architectures
    val archText = architectures?.joinToString(", ") ?: "未知"
    InfoRow(
        label = "Arch",
        value = archText
    )
    
    // 下载量 - 检查字段名
    InfoRow(
        label = "下载量",
        value = "${appDetail.downloadCount ?: 0}"
    )
    
    // 创建时间 - 检查字段名
    val createdAt = raw?.createdAt
    createdAt?.let {
        // 尝试格式化日期 (2026-01-14T12:54:00.499Z -> 2026-01-14)
        val formattedDate = try {
            it.substring(0, 10)
        } catch (e: Exception) {
            it
        }
        InfoRow(
            label = "创建于",
            value = formattedDate
        )
    }
    
    // 包名 - 检查字段名
    val packageName = raw?.packageName
    InfoRow(
        label = "包名",
        value = packageName ?: "未知"
    )
    
    // 应用类型
    InfoRow(
        label = "应用类型",
        value = appDetail.type
    )
    
    // 安装包大小
    if (appDetail.size != null) {
        InfoRow(
            label = "安装包大小",
            value = appDetail.size
        )
    }
    
    // 支持设备类型 - 检查字段名
    val supportedDevices = raw?.supportedDevices
    val devicesText = supportedDevices?.joinToString(", ") ?: "未知"
    if (devicesText.isNotEmpty() && devicesText != "未知") {
        InfoRow(
            label = "支持设备",
            value = devicesText
        )
    }
    
    // 支持的屏幕密度 - 检查字段名
    val supportedDensities = raw?.supportedDensities
    supportedDensities?.takeIf { it.isNotEmpty() }?.let { densities ->
        InfoRow(
            label = "屏幕密度",
            value = densities.joinToString(", ")
        )
    }
    
    // 最后更新时间 - 检查字段名
    val updatedAt = raw?.updatedAt
    updatedAt?.let {
        val formattedDate = try {
            it.substring(0, 10)
        } catch (e: Exception) {
            it
        }
        InfoRow(
            label = "最后更新",
            value = formattedDate
        )
    }
}
else -> {
Text(
                    text = "⚠️什么都没有(||๐_๐)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error//,
//                    fontSize = 12.sp,
//                    lineHeight = 14.sp
                )                        }
}      }
                }
            }
        

        // --- 应用介绍 ---
item {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("应用介绍", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (!appDetail.description.isNullOrEmpty()) {
                LinkifyText(
                    text = appDetail.description,
                    navController = navController,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            } else {
                Text("暂无介绍", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

        // --- 应用截图 ---
        if (!appDetail.previews.isNullOrEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("应用截图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(appDetail.previews) { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onImagePreview(url) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 作者信息 ---
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("作者信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    when (appDetail.store) {
                        AppStore.SIENE_SHOP -> {
                            // 弦应用商店：同时显示上传者和审核员
                            val raw = appDetail.raw as? cc.bbq.xq.SineShopClient.SineShopAppDetail
                            
                            // 上传者信息
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val userId = raw?.user?.id
                                        if (userId != null) {
                                            navController.navigate(UserDetail(userId.toLong(), appDetail.store).createRoute())
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(raw?.user?.userAvatar ?: "https://static.sineshop.xin/images/user_avatar/default_avatar.png")
                                        .diskCachePolicy(CachePolicy.DISABLED) // 禁用磁盘缓存
                                        .build(),
                                    contentDescription = "上传者头像",
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(raw?.user?.displayName ?: "未知上传者", style = MaterialTheme.typography.titleMedium)
                                    Text("上传者", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            
                            // 审核员信息
                            if (raw?.audit_user != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            val userId = raw.audit_user?.id
                                            if (userId != null) {
                                                navController.navigate(UserDetail(userId.toLong(), appDetail.store).createRoute())
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(raw.audit_user?.userAvatar ?: "https://static.sineshop.xin/images/user_avatar/default_avatar.png")
                                            .diskCachePolicy(CachePolicy.DISABLED) // 禁用磁盘缓存
                                            .build(),
                                        contentDescription = "审核员头像",
                                        modifier = Modifier.size(40.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(raw.audit_user?.displayName ?: "未知审核员", style = MaterialTheme.typography.titleMedium)
                                        Text("审核员", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        AppStore.XIAOQU_SPACE -> {
                            // 小趣空间只显示上传者
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        val userId = appDetail.user.id.toLongOrNull()
                                        if (userId != null) {
                                            navController.navigate(UserDetail(userId, appDetail.store).createRoute())
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = appDetail.user.avatarUrl,
                                    contentDescription = "上传者头像",
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(appDetail.user.displayName, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        AppStore.SINE_OPEN_MARKET -> {
                            // 弦开放市场只显示上传者
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        val userId = appDetail.user.id.toLongOrNull()
                                        if (userId != null) {
                                            navController.navigate(UserDetail(userId, appDetail.store).createRoute())
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = appDetail.user.avatarUrl,
                                    contentDescription = "上传者头像",
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(appDetail.user.displayName, style = MaterialTheme.typography.titleMedium)
                            }
                        }                        
                        AppStore.LING_MARKET -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        val userId = appDetail.user.id.toLongOrNull()
                                        if (userId != null) {
                                            navController.navigate(UserDetail(userId, appDetail.store).createRoute())
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = appDetail.user.avatarUrl,
                                    contentDescription = "上传者头像",
                                    modifier = Modifier.size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(appDetail.user.displayName, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        else -> {
                            Text(
                    text = "⚠什么都没有!﹁_﹂",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error//,
//                    fontSize = 12.sp,
//                    lineHeight = 14.sp
                )
                        }
                    }
                }
            }
        }

        // --- 评论列表 ---
        item {
            Text("评论 (${appDetail.reviewCount})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (comments.isEmpty()) {
            item {
                Text("暂无评论", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp))
            }
        } else {
items(comments) { comment ->
    UnifiedCommentItem(
        comment = comment,
        onReply = { onCommentReply(comment) },
        onLongClick = { onCommentLongClick(comment.id) },
        onUserClick = {
            val userId = comment.sender.id.toLongOrNull()
            if (userId != null) {
                navController.navigate(UserDetail(userId, appDetail.store).createRoute())
            }
        },
        navController = navController // 新增参数
    )
}
        }
    }
}

// 新增：信息行组件
@Composable
fun InfoRow(label: String, value: String?) {
    if (!value.isNullOrEmpty() && value != "未知" && value != "") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
    }
}

// 新增：获取设备兼容性信息
@Composable
fun getDeviceInfo(minSdk: Int?): String {
    val context = LocalContext.current
    val deviceSdk = android.os.Build.VERSION.SDK_INT
    
    return buildString {
        append("当前设备SDK:  $deviceSdk")
        if (minSdk != null && deviceSdk >= minSdk) {
            append(" • 兼容")
        } else {
            append(" • 不兼容")
        }
    }
}