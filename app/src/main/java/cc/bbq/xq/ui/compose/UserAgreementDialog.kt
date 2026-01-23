package cc.bbq.xq.ui.compose

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.bbq.xq.R
import cc.bbq.xq.data.UserAgreementDataStore
import cc.bbq.xq.ui.animation.materialSharedAxisX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UserAgreementDialog(
    onDismissRequest: () -> Unit,
    onAgreed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val agreementDataStore = remember { UserAgreementDataStore(context) }

    // 状态管理
    var currentAgreementIndex by remember { mutableStateOf(0) }
    val agreementContents = remember { mutableStateMapOf<Int, String>() }
    var animationForward by remember { mutableStateOf(true) }

    val agreements = listOf(
        AgreementItem("《OpenQu 用户协议》", R.raw.useragreement),
        AgreementItem("《小趣空间用户协议》", R.raw.xiaoquuseragreement),
        AgreementItem("《弦-应用商店用户协议》", R.raw.sineuseragreement),
        AgreementItem("《弦-应用商店隐私政策》", R.raw.sineprivacypolicy)
    )

    // 异步加载
    LaunchedEffect(Unit) {
        agreements.forEachIndexed { index, item ->
            val content = withContext(Dispatchers.IO) {
                loadRawResourceText(context, item.resId)
            }
            agreementContents[index] = content
        }
    }

    Dialog(
        onDismissRequest = { /* 强制流程，不可外部点击取消 */ },
        properties = DialogProperties(usePlatformDefaultWidth = false) // 允许自定义宽度适配
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f) // 占据屏幕宽度的 90%
                .fillMaxHeight(0.85f) // 关键优化：限制最大高度，防止按钮溢出屏幕
                .padding(vertical = 24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // 1. 头部：总标题
                Text(
                    text = "服务协议与隐私政策",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "请阅读并同意以下条款以继续使用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. 中间：内容区域（使用 weight 确保其可弹性缩放）
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = currentAgreementIndex,
                        transitionSpec = {
                            materialSharedAxisX(forward = animationForward, slideDistance = 30)
                        },
                        label = "AgreementTransition"
                    ) { targetIndex ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = agreements[targetIndex].title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val scrollState = rememberScrollState()
                            // 切换协议时自动回顶
                            LaunchedEffect(targetIndex) { scrollState.scrollTo(0) }

                            MarkDownText(
                                content = agreementContents[targetIndex] ?: "正在加载...",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            )
                        }
                    }
                }

                // 3. 底部：按钮区域
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 上一个按钮：使用 Tonal 样式
                        if (currentAgreementIndex > 0) {
                            FilledTonalButton(
                                onClick = {
                                    animationForward = false
                                    currentAgreementIndex--
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("上一个")
                            }
                        } else {
                            // 第一页时占位，保持右侧按钮比例一致
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        // 同意按钮：使用 Filled 样式
                        Button(
                            onClick = {
                                scope.launch {
                                    saveAgreement(agreementDataStore, currentAgreementIndex)
                                    if (currentAgreementIndex < agreements.size - 1) {
                                        animationForward = true
                                        currentAgreementIndex++
                                    } else {
                                        onAgreed()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (currentAgreementIndex < agreements.size - 1) "同意并继续" else "同意并进入",
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// 辅助数据类
private data class AgreementItem(val title: String, val resId: Int)

// 资源加载函数
private fun loadRawResourceText(context: android.content.Context, resId: Int): String {
    return try {
        context.resources.openRawResource(resId).use { it.bufferedReader().readText() }
    } catch (e: Exception) {
        "加载失败"
    }
}

// 存储逻辑封装
private suspend fun saveAgreement(ds: UserAgreementDataStore, index: Int) {
    when (index) {
        0 -> ds.setUserAgreementAccepted(true)
        1 -> ds.setXiaoquUserAgreementAccepted(true)
        2 -> ds.setSineUserAgreementAccepted(true)
        3 -> ds.setSinePrivacyPolicyAccepted(true)
    }
}