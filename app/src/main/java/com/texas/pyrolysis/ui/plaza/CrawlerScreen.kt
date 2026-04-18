package com.texas.pyrolysis.ui.plaza

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.texas.pyrolysis.ui.theme.BBQSnackbarHost
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrawlerScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState, // 外部传入
    viewModel: CrawlerViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var startIdText by remember { mutableStateOf(uiState.startId.toString()) }
    var endIdText by remember { mutableStateOf(uiState.endId.toString()) }

    // 监听 ViewModel 发出的 Snackbar 事件
    LaunchedEffect(viewModel.snackbarEvents) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(uiState.endId) { endIdText = uiState.endId.toString() }

    Scaffold(
        // 这里依然需要 Scaffold 来承载 TopAppBar，
        // 但 snackbarHost 使用传入的 snackbarHostState
        snackbarHost = { BBQSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("爬虫控制台") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 配置卡片
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("抓取范围", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startIdText,
                            onValueChange = { startIdText = it; it.toIntOrNull()?.let { v -> viewModel.updateStartId(v) } },
                            label = { Text("Start ID") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !uiState.isCrawling
                        )
                        OutlinedTextField(
                            value = endIdText,
                            onValueChange = { endIdText = it; it.toIntOrNull()?.let { v -> viewModel.updateEndId(v) } },
                            label = { Text("End ID") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            enabled = !uiState.isCrawling
                        )
                    }
                    Button(
                        onClick = { viewModel.detectMaxId() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isCrawling && !uiState.isDetectingMaxId
                    ) {
                        if (uiState.isDetectingMaxId) CircularProgressIndicator(Modifier.size(18.dp))
                        else Text("自动探测 ID 边界")
                    }
                }
            }

            // 进度条卡片
            if (uiState.isCrawling || uiState.currentProgress > 0) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val progress = if (uiState.totalToCrawl > 0) uiState.currentProgress.toFloat() / uiState.totalToCrawl else 0f
                        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("总进度: ${uiState.currentProgress}/${uiState.totalToCrawl} (成功入库: ${uiState.successCount})")
                    }
                }
            }

            // 控制按钮
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (uiState.isCrawling) viewModel.stopCrawling() else viewModel.startCrawling() },
                    modifier = Modifier.weight(1f),
                    colors = if (uiState.isCrawling) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) {
                    Icon(if (uiState.isCrawling) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isCrawling) "停止" else "开始抓取")
                }
                
                Button(
                    onClick = { viewModel.exportData() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isCrawling && !uiState.isExporting
                ) {
                    if (uiState.isExporting) CircularProgressIndicator(Modifier.size(18.dp))
                    else {
                        Icon(Icons.Default.FileDownload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("导出 JSON")
                    }
                }
            }

            // 黑色终端日志
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Text(
                    text = uiState.lastLog,
                    color = Color.Green,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }

    // 导出文件后的弹窗
    if (uiState.exportedFile != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportedFile() },
            title = { Text("数据导出成功") },
            text = { Text("文件已保存至：\n${uiState.exportedFile?.absolutePath}") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearExportedFile() }) { Text("好的") }
            }
        )
    }
}