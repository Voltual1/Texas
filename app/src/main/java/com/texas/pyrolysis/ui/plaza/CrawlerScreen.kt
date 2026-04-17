package com.texas.pyrolysis.ui.plaza

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrawlerScreen(
    onBack: () -> Unit,
    viewModel: CrawlerViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // 本地输入状态，用于 TextField
    var startIdText by remember { mutableStateOf(uiState.startId.toString()) }
    var endIdText by remember { mutableStateOf(uiState.endId.toString()) }

    // 当 ViewModel 中的 endId 改变时（如探测完成），同步更新本地文本
    LaunchedEffect(uiState.endId) {
        endIdText = uiState.endId.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("微思商店爬虫控制台") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.isCrawling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 配置区域 ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("抓取配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startIdText,
                            onValueChange = { 
                                startIdText = it
                                it.toIntOrNull()?.let { id -> viewModel.updateStartId(id) }
                            },
                            label = { Text("起始 ID") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !uiState.isCrawling
                        )
                        OutlinedTextField(
                            value = endIdText,
                            onValueChange = { 
                                endIdText = it
                                it.toIntOrNull()?.let { id -> viewModel.updateEndId(id) }
                            },
                            label = { Text("结束 ID") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !uiState.isCrawling
                        )
                    }

                    Button(
                        onClick = { viewModel.detectMaxId() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isCrawling && !uiState.isDetectingMaxId,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        if (uiState.isDetectingMaxId) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("探测中...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("自动探测最大有效 ID")
                        }
                    }
                }
            }

            // --- 进度显示区域 ---
            if (uiState.isCrawling || uiState.currentProgress > 0) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("当前进度", style = MaterialTheme.typography.titleMedium)
                        
                        val progress = if (uiState.totalToCrawl > 0) {
                            uiState.currentProgress.toFloat() / uiState.totalToCrawl.toFloat()
                        } else 0f
                        
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("已完成: ${uiState.currentProgress} / ${uiState.totalToCrawl}")
                            Text("${(progress * 100).toInt()}%")
                        }
                    }
                }
            }

            // --- 控制按钮区域 ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!uiState.isCrawling) {
                    Button(
                        onClick = { viewModel.startCrawling() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("开始抓取")
                    }
                } else {
                    Button(
                        onClick = { viewModel.stopCrawling() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("停止抓取")
                    }
                }

                Button(
                    onClick = { viewModel.exportData() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isCrawling && !uiState.isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("导出 JSON")
                    }
                }
            }

            // --- 日志区域 ---
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("运行日志:", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.lastLog,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // --- 错误提示 ---
            uiState.errorMessage?.let { error ->
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("确认", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Text(error)
                }
            }

            // --- 导出成功提示 ---
            uiState.exportedFile?.let { file ->
                AlertDialog(
                    onDismissRequest = { /* 不允许点击外部消失 */ },
                    title = { Text("导出成功") },
                    text = { Text("数据已保存至：\n${file.absolutePath}") },
                    confirmButton = {
                        TextButton(onClick = { /* 这里可以添加分享逻辑 */ }) {
                            Text("好的")
                        }
                    }
                )
            }
        }
    }
}