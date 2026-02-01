//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package cc.bbq.xq.ui.payment

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import cc.bbq.xq.ui.Download
import cc.bbq.xq.ui.theme.*
import cc.bbq.xq.util.DownloadManager
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest // 必须导入这个才能使用 collectLatest
import kotlinx.coroutines.launch

@Composable
fun PaymentCenterScreen(
    viewModel: PaymentViewModel,
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    val isLoadingBalance by viewModel.isLoadingBalance.collectAsState()
    val paymentInfo by viewModel.paymentInfo.collectAsState()
    val coinsBalance by viewModel.coinsBalance.collectAsState()
    val paymentStatus by viewModel.paymentStatus.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val downloadUrl by remember(paymentStatus) {
        derivedStateOf {
            if (paymentStatus == PaymentStatus.SUCCESS) viewModel.getDownloadUrl() else null
        }
    }
    
    val downloadFileName by remember(paymentStatus) {
        derivedStateOf {
            if (paymentStatus == PaymentStatus.SUCCESS) viewModel.getDownloadFileName() else null
        }
    }
    
    fun startDownload(url: String, fileName: String) {
        val activity = context as? Activity
        activity?.let {
            DownloadManager.download(it, url, fileName, null)
        }
    }
    
    LaunchedEffect(Unit) {
        // 修正 138 & 139: 确保导入了 collectLatest，event 类型会被自动推断
        viewModel.downloadEvent.collectLatest { event ->
            val activity = context as? Activity
            activity?.let {
                // 修正 140 & 141: 使用 event.url 和 event.fileName
                DownloadManager.download(
                    activity = it,
                    url = event.url,
                    fileName = event.fileName
                )

                // 修正 142 & 143: 使用 coroutineScope 而不是 scope
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "任务已发送至 1DM: ${event.fileName}",
                        actionLabel = "管理下载",
                        withDismissAction = true,
                        duration = SnackbarDuration.Indefinite
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // 修正 144: 使用安全调用 ?.
                        navController?.navigate(Download.route)
                    }
                }
            }
        }
    }

    when (paymentStatus) {
        PaymentStatus.SUCCESS -> {
            PaymentResultDialog(
                success = true,
                onDismiss = { 
                    viewModel.resetPaymentStatus()
                    navController?.popBackStack()
                },
                onDownload = {
                    downloadUrl?.let { url ->
                        downloadFileName?.let { fileName -> startDownload(url, fileName) }
                    }
                },
                showDownloadButton = paymentInfo?.type == PaymentType.APP_PURCHASE && !downloadUrl.isNullOrEmpty(),
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                navController = navController,
                fileName = downloadFileName,
                viewModel = viewModel // 修正：传入 viewModel
            )
        }
        PaymentStatus.FAILED -> {
            PaymentResultDialog(
                success = false,
                error = errorMessage,
                onDismiss = { viewModel.resetPaymentStatus() },
                showDownloadButton = false,
                viewModel = viewModel // 修正：传入 viewModel
            )
        }
        else -> {
            PaymentContent(
                paymentInfo = paymentInfo,
                coinsBalance = coinsBalance,
                isLoadingBalance = isLoadingBalance,
                errorMessage = errorMessage,
                onFetchBalance = { viewModel.fetchCoinsBalance() },
                onPay = { amount -> viewModel.executePayment(amount) },
                viewModel = viewModel,
                isPaymentProcessing = paymentStatus == PaymentStatus.PROCESSING,
                modifier = modifier,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
fun PaymentContent(
    paymentInfo: PaymentInfo?,
    coinsBalance: Int?,
    isLoadingBalance: Boolean,
    errorMessage: String?,
    onFetchBalance: () -> Unit,
    onPay: (Int) -> Unit,
    viewModel: PaymentViewModel,
    isPaymentProcessing: Boolean,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    var amount by remember { mutableStateOf(paymentInfo?.price?.toString() ?: "") }
    var postIdInput by remember { mutableStateOf("") }
    val isAdvancedMode = paymentInfo?.type == PaymentType.POST_REWARD && paymentInfo.postId == 0L

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isAdvancedMode) {
                BBQCard(modifier = Modifier.fillMaxWidth()) {
                     Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("高级支付模式", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = postIdInput,
                            onValueChange = { postIdInput = it },
                            label = { Text("输入帖子ID") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                        )
                        Button(
                            onClick = {
                                postIdInput.toLongOrNull()?.let { viewModel.loadPostInfo(it) } ?: coroutineScope.launch {
                                    snackbarHostState.showSnackbar("请输入有效的帖子ID")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("加载帖子") }
                    }
                }
            } else {
                // 信息展示卡片...
                BBQCard(modifier = Modifier.fillMaxWidth()) {
                    paymentInfo?.let { info ->
                        when (info.type) {
                            PaymentType.APP_PURCHASE -> {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = info.iconUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(16.dp))
                                        Column {
                                            Text(info.appName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                            Text("版本: ${info.versionId}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    Text(info.previewContent, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("价格")
                                        Text("${info.price} 硬币", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            PaymentType.POST_REWARD -> { /* 帖子打赏布局保持原样 */ }
                            else -> { Text("支付信息", modifier = Modifier.padding(16.dp)) }
                        }
                    }
                }
            }

            // 余额区域
            BBQCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("我的硬币", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        BBQOutlinedButton(
                            onClick = onFetchBalance,
                            enabled = !isLoadingBalance,
                            text = { Text(if (isLoadingBalance) "查询中..." else "刷新余额") }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("当前余额:", modifier = Modifier.weight(1f))
                        if (isLoadingBalance) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("${coinsBalance ?: "--"} 硬币", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 支付按钮
            val payAmount = amount.toIntOrNull() ?: 0
            BBQButton(
                onClick = {
                    if (payAmount > 0) {
                        if (coinsBalance != null && payAmount > coinsBalance) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("硬币余额不足") }
                        } else onPay(payAmount)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = payAmount > 0 && !isPaymentProcessing,
                text = {
                    if (isPaymentProcessing) CircularProgressIndicator(Modifier.size(24.dp))
                    else Text("确认支付")
                }
            )
        }

        // SnackBar 宿主，修正对齐问题
        BBQSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    errorMessage?.let { msg ->
        LaunchedEffect(msg) { snackbarHostState.showSnackbar(msg) }
    }
}

@Composable
fun PaymentResultDialog(
    success: Boolean,
    error: String? = null,
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)? = null,
    showDownloadButton: Boolean = false,
    snackbarHostState: SnackbarHostState? = null,
    coroutineScope: CoroutineScope? = null,
    navController: NavController? = null,
    fileName: String? = null,
    viewModel: PaymentViewModel // 修正 145, 146, 147: 添加 viewModel 参数
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        BBQCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                    contentDescription = null,
                    tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(24.dp))
                Text(if (success) "支付成功！" else "支付失败", style = MaterialTheme.typography.headlineMedium)
                
                if (!success && !error.isNullOrEmpty()) {
                    Text(error, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
                }

                Spacer(Modifier.height(32.dp))

                if (success && showDownloadButton) {
                    BBQButton(
                        onClick = {
                            // 现在 viewModel 已经可以访问了
                            val url = viewModel.getDownloadUrl()
                            val name = viewModel.getDownloadFileName()
                            if (url != null) {
                                viewModel.startDownload(url, name)
                            }
                            onDismiss() 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        text = { Text("下载应用") }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                BBQOutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    text = { Text(if (success) "完成" else "重试") }
                )
            }
        }
    }
}