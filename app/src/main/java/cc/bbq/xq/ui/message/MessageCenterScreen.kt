//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.message

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.bbq.xq.KtorClient
import cc.bbq.xq.ui.compose.MessageItem
import cc.bbq.xq.ui.compose.PageJumpDialog
import cc.bbq.xq.ui.compose.PaginationControls
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.foundation.layout.Box

// 在 MessageCenterScreen.kt 中修复 UI 显示问题

@Composable
fun MessageCenterScreen(
    viewModel: MessageViewModel,
    onMessageClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var showPageDialog by remember { mutableStateOf(false) }
    val dialogShape = remember { RoundedCornerShape(4.dp) }

    // 判断是否正在刷新（第一页加载时）
    val isRefreshing = state.isLoading && state.currentPage == 1

    // 修复：只在真正需要时初始化，不强制重置
    LaunchedEffect(Unit) {
        viewModel.initializeIfNeeded()
    }

    // 创建状态 - 必须只创建一次！
    val pullToRefreshState = rememberPullToRefreshState()
    
    // 监听状态变化，手动刷新
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing && !isRefreshing) {
            // 开始刷新
            viewModel.reset()
        }
    }
    
    // 当刷新完成时，结束刷新状态
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && pullToRefreshState.isRefreshing) {
            // 结束刷新动画
            pullToRefreshState.endRefresh()
        }
    }

    // 方法1：使用 PullToRefreshBox（推荐）
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = {
            // 下拉刷新时重置到第一页
            viewModel.reset()
        }
    ) {
        // 内容区域
        ContentArea(
            state = state,
            onMessageClick = onMessageClick,
            viewModel = viewModel,
            showPageDialog = showPageDialog,
            dialogShape = dialogShape,
            onShowPageDialogChange = { showPageDialog = it }
        )
    }

    // 分页跳转对话框
    if (showPageDialog) {
        PageJumpDialog(
            currentPage = state.currentPage,
            totalPages = state.totalPages,
            onDismiss = { showPageDialog = false },
            onConfirm = { page ->
                viewModel.goToPage(page)
                showPageDialog = false
            },
            shape = dialogShape
        )
    }
}

@Composable
private fun ContentArea(
    state: MessageState,
    onMessageClick: (Long) -> Unit,
    viewModel: MessageViewModel,
    showPageDialog: Boolean,
    dialogShape: RoundedCornerShape,
    onShowPageDialogChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 消息列表
        Box(modifier = Modifier.weight(1f)) {
            when {
                // 修复：显示加载指示条
                state.isLoading && state.messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("加载中...")
                    }
                }
                state.messages.isEmpty() && state.isInitialized -> {
                    Text(
                        text = "暂无消息",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.messages.isEmpty() -> {
                    Text(
                        text = "加载中...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.messages) { message ->
                            MessageItem(
                                message = message,
                                onClick = {
                                    if (message.postid != null) {
                                        onMessageClick(message.postid)
                                    }
                                }
                            )
                        }

                        // 修复：分页加载时显示底部加载指示
                        if (state.isLoading && state.messages.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }

            // 显示错误信息
            state.error?.let { error ->
                if (state.messages.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.reset() }) {
                            Text("重试")
                        }
                    }
                }
            }
        }

        // 分页控制栏 - 只在有数据且不是加载中时显示
        if (state.messages.isNotEmpty() && !state.isLoading) {
            PaginationControls(
                currentPage = state.currentPage,
                totalPages = state.totalPages,
                onPrevClick = { viewModel.prevPage() },
                onNextClick = { viewModel.nextPage() },
                onPageClick = { onShowPageDialogChange(true) },
                isPrevEnabled = state.currentPage > 1,
                isNextEnabled = state.currentPage < state.totalPages
            )
        }
    }
}

// 方法2：如果需要自定义指示器位置或样式，可以使用这个替代方案
/*
@Composable
fun MessageCenterScreenAlternative(
    viewModel: MessageViewModel,
    onMessageClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var showPageDialog by remember { mutableStateOf(false) }
    val dialogShape = remember { RoundedCornerShape(4.dp) }
    val isRefreshing = state.isLoading && state.currentPage == 1
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        viewModel.initializeIfNeeded()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.reset() }
            )
    ) {
        // 内容区域
        ContentArea(
            state = state,
            onMessageClick = onMessageClick,
            viewModel = viewModel,
            showPageDialog = showPageDialog,
            dialogShape = dialogShape,
            onShowPageDialogChange = { showPageDialog = it }
        )

        // 手动显示指示器
        PullToRefreshDefaults.Indicator(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showPageDialog) {
        PageJumpDialog(
            currentPage = state.currentPage,
            totalPages = state.totalPages,
            onDismiss = { showPageDialog = false },
            onConfirm = { page ->
                viewModel.goToPage(page)
                showPageDialog = false
            },
            shape = dialogShape
        )
    }
}
*/

// 扩展函数：安全结束刷新
private suspend fun PullToRefreshState.endRefresh() {
    // 检查是否有刷新动画在进行
    if (isAnimating) {
        animateToHidden()
    }
}