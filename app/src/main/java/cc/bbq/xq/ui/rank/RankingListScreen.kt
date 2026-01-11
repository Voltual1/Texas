//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package cc.bbq.xq.ui.rank

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
// 移除 MD2 的 ExperimentalMaterialApi 和 pullrefresh 导入
// import androidx.compose.material.ExperimentalMaterialApi
// import androidx.compose.material.pullrefresh.PullRefreshIndicator
// import androidx.compose.material.pullrefresh.pullRefresh
// import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import cc.bbq.xq.ui.theme.BBQExposedDropdownMenuBox
import cc.bbq.xq.ui.theme.BBQExposedDropdownMenu
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cc.bbq.xq.ui.UserDetail
// 添加 MD3 pullrefresh 导入
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
// 导入我们自定义的指示器
import cc.bbq.xq.ui.theme.BBQPullRefreshIndicator
import androidx.compose.material3.ExperimentalMaterial3Api

// 移除 @ExperimentalMaterialApi 注解
// @OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingListScreen(
    navController: NavController,
    viewModel: RankingListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    // val refreshing = state.isRefreshing // 不需要直接使用 state.isRefreshing
    // val pullRefreshState = rememberPullRefreshState(refreshing, { viewModel.refreshRankingList() }) // 移除

    // 下拉菜单状态
    var expanded by remember { mutableStateOf(false) }

    // 下拉刷新状态
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    // 监听 ViewModel 状态变化以结束刷新状态
    LaunchedEffect(state.isRefreshing, state.isLoading, state.error, state.rankingList) {
        // 当加载完成（isLoading 变为 false）且有数据或出错时，结束刷新
        if (!state.isLoading && (state.rankingList.isNotEmpty() || state.error != null) && isRefreshing) {
            isRefreshing = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 排序选择器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp) // 增加垂直间距让视觉更平衡
        ) {
            BBQExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = state.sortType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // 展开时的颜色
                        unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer, // 平时的颜色
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent, // 移除底部横线
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    shape = MaterialTheme.shapes.medium, // 让边角圆润一点，避免直角的生硬感
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        )
                )

                BBQExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    SortType.values().forEach { sortType ->
                        DropdownMenuItem(
                            text = { Text(sortType.displayName) },
                            onClick = {
                                viewModel.changeSortType(sortType)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // 使用 MD3 的 PullToRefreshBox
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refreshRankingList()
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
            modifier = Modifier.fillMaxSize()
        ) {
            // Box( // 移除旧的 Box
            //     modifier = Modifier
            //         .fillMaxSize()
            //         .pullRefresh(pullRefreshState) // 移除旧的 pullRefresh
            // ) { // 移除旧的 Box
            if (state.isLoading && state.rankingList.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.error != null) {
                val errorMessage = state.error // 使用非空断言，因为我们已经检查过 state.error != null
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "加载失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
    text = errorMessage ?: "未知错误",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refreshRankingList() }) {
                        Text("重试")
                    }
                }
            } else if (state.rankingList.isEmpty()) {
                Text(
                    text = "暂无排名数据",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(state.rankingList) { index, user ->
                        RankingListItem(
                            ranking = index + 1,
                            user = user,
                            sortType = state.sortType, // 传递排序类型用于显示正确的数值
                            onClick = {
                                // 使用 NavController 导航
                                navController.navigate(UserDetail(user.id).createRoute())
                            }
                        )

                        if (index == state.rankingList.size - 1 && state.hasMore && !state.isLoading) {
                            SideEffect {
                                viewModel.loadNextRankingList()
                            }
                        }
                    }

                    if (state.isLoading && state.rankingList.isNotEmpty() && !isRefreshing) {
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
            // 移除旧的 PullRefreshIndicator
            // PullRefreshIndicator(
            //     refreshing,
            //     pullRefreshState,
            //     Modifier.align(Alignment.TopCenter),
            //     contentColor = MaterialTheme.colorScheme.primary, // 使用主题主色
            //     backgroundColor = MaterialTheme.colorScheme.surface // 使用主题背景色
            // )
            // } // 移除旧的 Box
        } // End PullToRefreshBox
    }
}