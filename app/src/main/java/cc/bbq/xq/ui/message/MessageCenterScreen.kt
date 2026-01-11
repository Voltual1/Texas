//Copyright (C) 2025 Voltual
// ... (版权信息保持不变) ...

package cc.bbq.xq.ui.message

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.bbq.xq.ui.compose.MessageItem
import cc.bbq.xq.ui.compose.PageJumpDialog
import cc.bbq.xq.ui.compose.PaginationControls

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

    // 下拉刷新状态
    var isRefreshing by remember { mutableStateOf(false) }

    // 修复：只在真正需要时初始化，不强制重置
    LaunchedEffect(Unit) {
        viewModel.initializeIfNeeded()
    }

    val pullRefreshState = rememberPullToRefreshState()

    // 使用 MD3 的 PullToRefreshBox
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            // 触发 ViewModel 重置和加载第一页
            viewModel.reset()
            // 结束刷新状态的逻辑由 LaunchedEffect 处理
        },
        state = pullRefreshState, // 显式传递 state
        // 自定义指示器颜色
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                // 使用语义颜色
                color = MaterialTheme.colorScheme.primary, // 指示器颜色
                backgroundColor = MaterialTheme.colorScheme.surface, // 背景颜色
            )
        },
        modifier = modifier.fillMaxSize()
    ) {
        // 内容区域
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 消息列表
            Box(modifier = Modifier.weight(1f)) {
                when {
                    // 显示加载指示条 - 仅在非刷新状态下且没有消息时显示
                    // 修改条件：不在刷新时显示这个加载指示器
                    state.isLoading && state.messages.isEmpty() && !isRefreshing -> {
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
                    // 这个条件可能与上面的冲突或冗余，根据实际初始化逻辑调整
                    // 如果初始化但尚未加载，且不在刷新状态，可以显示加载提示
                    state.messages.isEmpty() && !state.isInitialized && !isRefreshing -> {
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

                            // 分页加载时显示底部加载指示 - 仅在非刷新状态下显示
                            // 修改条件：不在刷新时显示这个加载指示器
                            if (state.isLoading && state.messages.isNotEmpty() && !isRefreshing) {
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

                // 显示错误信息 - 通常不在刷新时显示，除非刷新本身失败
                // 这里简化处理：只要有错误且列表为空就显示（忽略 isRefreshing）
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
                            Button(onClick = {
                                viewModel.reset()
                            }) {
                                Text("重试")
                            }
                        }
                    }
                    // 如果有消息且加载出错，可以考虑用 Snackbar 显示错误
                }
            }

            // 分页控制栏 - 只在有数据且不是加载中时显示
            // 修改条件：不在刷新时显示分页控件（可选，取决于设计）
            if (state.messages.isNotEmpty() && !state.isLoading /* && !isRefreshing */) {
                PaginationControls(
                    currentPage = state.currentPage,
                    totalPages = state.totalPages,
                    onPrevClick = { viewModel.prevPage() },
                    onNextClick = { viewModel.nextPage() },
                    onPageClick = { showPageDialog = true },
                    isPrevEnabled = state.currentPage > 1,
                    isNextEnabled = state.currentPage < state.totalPages
                )
            }
        }
    } // End of PullToRefreshBox content

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

    // 监听 ViewModel 状态变化以结束刷新状态
    LaunchedEffect(state.isLoading, state.isInitialized, state.error) {
        // 当加载完成（isLoading 变为 false）且初始化完成（isInitialized 为 true）时，结束刷新
        if (!state.isLoading && state.isInitialized && isRefreshing) {
            isRefreshing = false
        }
        // 如果加载失败并且正在刷新，也应该结束刷新状态
        if (state.error != null && isRefreshing) {
             isRefreshing = false
        }
        // 额外检查：如果 ViewModel 的状态已经是初始化完成且非加载状态，但 UI 仍认为在刷新，也应结束
        // 这可以处理一些边界情况
        if (state.isInitialized && !state.isLoading && isRefreshing) {
             isRefreshing = false
        }
    }
}