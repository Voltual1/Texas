//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package cc.bbq.xq.ui.plaza

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

// Compose Foundation
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

// Compose Material 3
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

// Compose Runtime & UI
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// Image Loading (Coil 3)
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest

// Dependency Injection (Koin)
import org.koin.androidx.compose.koinViewModel

// Coroutines
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Project Specific (App Logic & UI)
import cc.bbq.xq.AppStore
import cc.bbq.xq.LingMarketClient
import cc.bbq.xq.data.unified.*
import cc.bbq.xq.ui.*
import cc.bbq.xq.ui.community.compose.CommentDialog
import cc.bbq.xq.ui.community.compose.CommentItem
import cc.bbq.xq.ui.compose.LinkifyText
import cc.bbq.xq.ui.theme.*
import cc.bbq.xq.util.DownloadManager
import cc.bbq.xq.util.formatTimestamp

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

LaunchedEffect(Unit) {
    viewModel.downloadEvent.collectLatest { downloadEvent ->
        val activity = context as? Activity
        if (activity != null) {
            // 1. 启动 1DM 下载 (它是 Activity 形式，会覆盖当前 UI)
            DownloadManager.download(
                activity = activity,
                url = downloadEvent.url,
                fileName = downloadEvent.fileName,
                headers = downloadEvent.headers
            )

            // 2. 启动 1DM 后，立即在底层界面弹出“持久化” SnackBar
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "任务已发送至 1DM: ${downloadEvent.fileName}",
                    actionLabel = "管理下载", // 按钮文案
                    withDismissAction = true, // MD3 支持显示右侧的 X 按钮
                    duration = SnackbarDuration.Indefinite // 关键：除非点击否则不消失
                )

                // 3. 处理 SnackBar 的点击事件
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        // 用户点击了“管理下载”，执行静默导航
                        navController.navigate(Download.route)
                    }
                    SnackbarResult.Dismissed -> {
                        // 用户点击了 X 或者手动关闭
                    }
                }
            }
        }
    }
}

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
                AppStore.WYSAPPMARKET -> {
val shareUrl = "https://apk.wysteam.cn/app/?id=${detail.id}"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("应用链接", shareUrl)
                    clipboard.setPrimaryClip(clip)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("已复制分享链接: $shareUrl")
                    }                                    }
                                    
                else -> {
                    // 暂不支持分享
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("暂不支持该商店的分享功能")
                    }
                }
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
                // 颜色和形状将使用在 Components.kt 中定义的默认值（语义颜色）
            )
        },
        modifier = modifier.fillMaxSize()
    ) {
    if (isLoading && appDetail == null) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    } else if (appDetail != null) {
        val detail = appDetail!! // 建议在这里先解包，避免后面到处用 !!
        val pageCount = when (detail.store) {
            AppStore.SIENE_SHOP, AppStore.WYSAPPMARKET -> 2
            else -> 1
        }
        val pagerState = rememberPagerState(pageCount = { pageCount })

        // 使用 Column 或 Box 包裹 Pager，确保布局正确
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f) 
            ) { page ->
                when (page) {
                    0 -> {
                        AppDetailContent(
                            navController = navController,
                            appDetail = detail,
                            comments = comments,
                            onCommentReply = { comment -> viewModel.openReplyDialog(comment) },
                            onDownloadClick = { viewModel.handleDownloadClick() },
                            onCommentLongClick = { id: String -> // 明确指定类型
                                commentToDeleteId = id
                                showDeleteCommentDialog = true
                            },
                            onDeleteAppClick = { showDeleteAppDialog = true },
                            onShareClick = { handleShare() },
                            onMoreMenuClick = { },
                            onImagePreview = { url: String -> // 明确指定类型
                                navController.navigate(ImagePreview(url).createRoute())
                            },
                            onRefundClick = { viewModel.requestRefund() },
                            onUpdateClick = { viewModel.requestUpdate() },
                            onFavoriteToggle = { viewModel.toggleFavorite() }
                        )
                    }
                    1 -> {
                        val packageName = detail.packageName
                        if (packageName.isNotEmpty()) {
                            VersionListScreen(
                                packageName = packageName,
                                storeName = detail.store.name,
                                navController = navController,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("该应用无包名，无法获取版本列表")
                            }
                        }
                    }
                }
            }
        }
        
        // FAB 需要放在这里，因为它相对于 Box 布局对齐
        FloatingActionButton(
            onClick = { viewModel.openCommentDialog() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.AutoMirrored.Filled.Comment, "评论")
        }
        
        BBQSnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

    // 删除应用确认对话框
    if (showDeleteAppDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAppDialog = false },
            title = { Text("确认删除应用") },
            shape = AppShapes.medium,
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
            shape = AppShapes.medium,
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
            // --- 关键修改处 ---
            // 不再直接通过 Intent 打开，而是调用 ViewModel 的逻辑
            // 这样它就会走 startDownload -> emit(DownloadEvent) -> 1DM 启动
            viewModel.startDownload(source.url) 
            viewModel.closeDownloadDrawer() // 选择后关闭抽屉
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
    navController: NavController, // 确保传入 navController
    appDetail: UnifiedAppDetail,
    comments: List<UnifiedComment>,
    onCommentReply: (UnifiedComment) -> Unit,
    onDownloadClick: () -> Unit,
    onCommentLongClick: (String) -> Unit,
    onDeleteAppClick: () -> Unit,
    onShareClick: () -> Unit,
     onMoreMenuClick: () -> Unit, // 这个可能不再需要，因为逻辑移到了 Header 组件里
    onImagePreview: (String) -> Unit,
    onRefundClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onFavoriteToggle: () -> Unit // 新增参数
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 应用头部信息 ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // 根据商店类型调用不同的头部组件
                when (appDetail.store) {
                    AppStore.XIAOQU_SPACE -> {
                        XiaoquSpaceAppHeader(
                            appDetail = appDetail,
                            onImagePreview = onImagePreview,
                            onDownloadClick = onDownloadClick,
                            onMoreMenuClick = { }, // 如果组件内部处理了，可以传空 lambda
                            onShareClick = onShareClick,
                            onUpdateClick = onUpdateClick,
                            onRefundClick = onRefundClick,
                            onDeleteAppClick = onDeleteAppClick
                        )
                    }
                    else -> {
                        DefaultAppHeader(
                            appDetail = appDetail,
                            onImagePreview = onImagePreview,
                            onDownloadClick = onDownloadClick,
                            onMoreMenuClick = { },
                            onShareClick = onShareClick
                        )
                    }
                }
            }
        }
        
        if (appDetail.store == AppStore.SIENE_SHOP || appDetail.store == AppStore.LING_MARKET) {
    item {
        val favoriteState = UnifiedFavoriteState(
            isFavorite = appDetail.isFavorite,
            favoriteCount = appDetail.favoriteCount
        )
        
        AppFavoriteCard(
            state = favoriteState,
            onToggle = { onFavoriteToggle() }
        )
    }
}

        // --- 更新日志 ---
// 判断是否显示
val updateLog = when (appDetail.store) {
    AppStore.SIENE_SHOP -> appDetail.updateLog
    AppStore.LING_MARKET -> {
        // 直接从 UnifiedAppDetail 的 updateLog 字段获取
        appDetail.updateLog
    }
    // Xiaoqu Space 不显示更新日志，所以返回 null
    AppStore.XIAOQU_SPACE -> null
    // 其他商店默认使用 UnifiedAppDetail.updateLog (如果需要)
    else -> null
    
}

if (!updateLog.isNullOrEmpty()) { // 这里检查的是上面 when 表达式的结果
    item {
        UpdateLogSection(appDetail = appDetail, navController = navController)
    }
}

        // --- 适配说明（小趣空间） ---
        item {
            XiaoquSpaceExplainSection(appDetail = appDetail, navController = navController)
        }

        // --- 应用信息卡片 ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "应用信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    // 根据不同商店显示不同的信息字段
                    when (appDetail.store) {
                        AppStore.XIAOQU_SPACE -> {
                            XiaoquSpaceAppInfo(appDetail = appDetail)
                        }

                        AppStore.SIENE_SHOP -> {
                            SineShopAppInfo(appDetail = appDetail)
                        }

                        AppStore.LING_MARKET -> {
                            LingMarketAppInfo(appDetail = appDetail)
                        }

                        AppStore.WYSAPPMARKET -> {
                            WysAppMarketInfo(appDetail = appDetail)
                        }

                        else -> {
                            Text(
                                text = "⚠️什么都没有(||๐_๐)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // --- 应用介绍 ---
        item {
            AppDescriptionSection(appDetail = appDetail, navController = navController)
        }

        // --- 应用截图 ---
        item {
            AppPreviewsSection(appDetail = appDetail, onImagePreview = onImagePreview)
        }

        // --- 作者信息 ---
        item {
            AppAuthorSection(appDetail = appDetail, navController = navController)
        }

        // --- 评论列表 ---
        item {
            CommentsHeader(appDetail = appDetail)
        }

        if (comments.isEmpty()) {
            item {
                NoCommentsMessage()
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