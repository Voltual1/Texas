package cc.bbq.xq.ui.compose

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // 必须
import androidx.compose.foundation.verticalScroll     // 必须
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration // 建议增加，用于处理屏幕适配
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cc.bbq.xq.ui.theme.AppShapes
import cc.bbq.xq.R
import cc.bbq.xq.data.UserAgreementDataStore
import org.koin.compose.koinInject
import cc.bbq.xq.ui.animation.materialSharedAxisX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch // 必须：用于 onClick 中的滚动动画
import kotlinx.coroutines.withContext

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UserAgreementDialog(
    onDismissRequest: () -> Unit,
    shape: Shape = AppShapes.medium,
    onAgreed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    //val agreementDataStore = remember { UserAgreementDataStore(context) }
val agreementDataStore: UserAgreementDataStore = koinInject()
    var currentAgreementIndex by remember { mutableStateOf(0) }
    val agreementContents = remember { mutableStateMapOf<Int, String>() }
    var animationForward by remember { mutableStateOf(true) }

    val agreements = listOf(
        AgreementItem("《浊燃 用户协议》", R.raw.useragreement),
        AgreementItem("《小趣空间用户协议》", R.raw.xiaoquuseragreement),
        AgreementItem("《弦-应用商店用户协议》", R.raw.sineuseragreement),
        AgreementItem("《弦-应用商店隐私政策》", R.raw.sineprivacypolicy),
        AgreementItem("《微思应用商店用户协议》", R.raw.wysappmarketuseragreement),
        AgreementItem("《微思应用商店隐私协议》", R.raw.wysappmarketprivacypolicy)
    )

    LaunchedEffect(Unit) {
        agreements.forEachIndexed { index, item ->
            val content = withContext(Dispatchers.IO) {
                loadRawResourceText(context, item.resId)
            }
            agreementContents[index] = content
        }
    }

    Dialog(
        onDismissRequest = { /* 强制流程 */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp), // 删除了 fillMaxHeight(0.85f)，让卡片随内容自适应
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            // 关键修改：使整个 Column 可滚动，就像 UpdateDialog 一样
            val mainScrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(mainScrollState) // 整体滚动，确保按钮不被挤出
                    .padding(24.dp)
            ) {
                // 1. 头部
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

                // 2. 中间：内容区域 (动画切换)
                AnimatedContent(
                    targetState = currentAgreementIndex,
                    transitionSpec = {
                        materialSharedAxisX(forward = animationForward, slideDistance = 30)
                    },
                    label = "AgreementTransition"
                ) { targetIndex ->
                    Column(modifier = Modifier.fillMaxWidth()) {
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

                        // 注意：这里不再使用独立的 verticalScroll，避免嵌套滚动冲突
                        MarkDownText(
                            content = agreementContents[targetIndex] ?: "正在加载...",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 3. 底部：按钮区域
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentAgreementIndex > 0) {
                        FilledTonalButton(
                            onClick = {
                                animationForward = false
                                currentAgreementIndex--
                                // 切换协议后自动滚回卡片顶部
                                scope.launch { mainScrollState.animateScrollTo(0) }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("上一个")
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                saveAgreement(agreementDataStore, currentAgreementIndex)
                                if (currentAgreementIndex < agreements.size - 1) {
                                    animationForward = true
                                    currentAgreementIndex++
                                    // 切换协议后自动滚回卡片顶部
                                    mainScrollState.animateScrollTo(0)
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

// 存储逻辑封装：适配新版 DataStore 的版本号机制
private suspend fun saveAgreement(ds: UserAgreementDataStore, index: Int) {
    when (index) {
        0 -> ds.acceptUserAgreement()       // 对应 AgreementVersions.USER_AGREEMENT
        1 -> ds.acceptXiaoquAgreement()     // 对应 AgreementVersions.XIAOQU_AGREEMENT
        2 -> ds.acceptSineAgreement()       
        3 -> ds.acceptSinePrivacy()         // 对应 AgreementVersions.SINE_PRIVACY
        4 -> ds.acceptWysMarketAgreement()  
        5 -> ds.acceptWysMarketPrivacy()    
    }
}