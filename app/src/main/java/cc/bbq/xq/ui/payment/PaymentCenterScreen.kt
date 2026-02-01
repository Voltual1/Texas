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

// ... PaymentContent 代码保持不变 ...

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