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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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

    // 使用 MD3 的 PullToRefreshBox
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            // 触发 ViewModel 重置和加载第一页
            viewModel.reset()
            // 注意：viewModel.reset() 会将 _isInitialized 设为 false，
            // 然后 initializeIfNeeded() 又会调用 loadPage(1)。
            // 我们需要在 loadPage 完成后，将 isRefreshing 设为 false。
            // 一种方式是在 ViewModel 状态中添加一个字段来指示刷新完成，
            // 或者使用 LaunchedEffect 监听 state.isLoading 和 state.isInitialized 的变化。
            // 这里我们使用后者，因为它更符合 MVVM 的思想，
            // 让 UI 根据 ViewModel 的状态变化做出反应。
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
                        // 这种情况可能发生在初始化但尚未开始加载时，
                        // 或者加载完成但列表为空时（但 isInitialized 为 false）。
                        // 通常在 isLoading 为 true 时会走上面的分支。
                        // 如果走到这里，可能需要更细致的状态区分。
                        Text(
                            text = "加载中...", // 或者 "暂无消息" 取决于具体逻辑
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
                    // 只有在没有消息且有错误时才显示错误信息（避免覆盖列表）
                    // 如果有消息但加载下一页出错，错误信息可能需要在别的地方显示，比如 Snackbar
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
                                // 重试逻辑：重置并重新加载
                                viewModel.reset()
                            }) {
                                Text("重试")
                            }
                        }
                    }
                    // 如果有消息且加载出错，可以考虑用 Snackbar 显示错误
                    // 或者在列表底部显示错误信息
                }
            }

            // 分页控制栏 - 只在有数据且不是加载中时显示
            if (state.messages.isNotEmpty() && !state.isLoading) {
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
    // 当加载完成（isLoading 变为 false）且初始化完成（isInitialized 为 true）时，结束刷新
    LaunchedEffect(state.isLoading, state.isInitialized) {
        if (!state.isLoading && state.isInitialized && isRefreshing) {
            isRefreshing = false
        }
        // 如果加载失败并且正在刷新，也应该结束刷新状态
        if (state.error != null && isRefreshing) {
             isRefreshing = false
        }
    }
}