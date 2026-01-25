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
    onUpdateClick: () -> Unit
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